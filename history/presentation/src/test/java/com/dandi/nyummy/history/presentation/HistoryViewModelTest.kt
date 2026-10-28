package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.GetMonthWithDayMealsUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.HistoryRepository
import com.dandi.nyummy.history.domain.MealDetailPage
import com.dandi.nyummy.history.domain.ReanalyzeMealUseCase
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import com.dandi.nyummy.history.presentation.util.todayDate
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeHistoryRepository()
    private val navigationHelper = FakeNavigationHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `초기 상태는 로드 완료 전에도 오늘 연월로 설정된다`() = runTest(testDispatcher) {
        val today = todayDate()

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(today.year, state.displayedYear)
        assertEquals(today.month, state.displayedMonth)
        assertEquals(today, state.selectedDate)
        assertTrue(state.isLoading)
    }

    @Test
    fun `초기 로드 완료 전 월 이동도 유효한 연월로 요청한다`() = runTest(testDispatcher) {
        val today = todayDate()
        val (prevYear, prevMonth) = previousMonthOf(today.year, today.month)
        val viewModel = createViewModel()

        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth(prevYear, prevMonth)))
        advanceUntilIdle()

        assertTrue(repository.monthlyRequests.all { (year, _) -> year > 0 })
        assertTrue((prevYear to prevMonth) in repository.monthlyRequests)
        assertEquals(prevMonth, viewModel.uiState.value.displayedMonth)
    }

    @Test
    fun `월 이동 중 새 이동이 오면 이전 요청은 취소되어 화면을 덮어쓰지 않는다`() = runTest(testDispatcher) {
        val today = todayDate()
        val (prevYear, prevMonth) = previousMonthOf(today.year, today.month)
        val gate = CompletableDeferred<HistoryCalendarVO>()
        repository.monthlyOverride = { year, month ->
            if (year == prevYear && month == prevMonth) gate.await() else HistoryCalendarVO(year, month)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth(prevYear, prevMonth)))
        advanceUntilIdle() // 이전 달 요청이 gate 에서 대기 중
        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth(today.year, today.month)))
        gate.complete(HistoryCalendarVO(prevYear, prevMonth)) // 취소된 요청의 늦은 응답
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(today.month, state.displayedMonth)
        assertTrue(state.displayedMonth != prevMonth)
    }

    @Test
    fun `인접 월 날짜를 선택하면 그 달로 이동하면서 해당 날짜가 선택된다`() = runTest(testDispatcher) {
        val today = todayDate()
        val (prevYear, prevMonth) = previousMonthOf(today.year, today.month)
        val target = HistoryDateVO(prevYear, prevMonth, 15)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(prevYear, state.displayedYear)
        assertEquals(prevMonth, state.displayedMonth)
        assertEquals(target, state.selectedDate)
    }

    @Test
    fun `표시 중인 달의 날짜를 선택하면 달은 그대로 유지된다`() = runTest(testDispatcher) {
        val today = todayDate()
        val target = HistoryDateVO(today.year, today.month, 1)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(today.year, state.displayedYear)
        assertEquals(today.month, state.displayedMonth)
        assertEquals(target, state.selectedDate)
    }

    @Test
    fun `삭제 확인을 연타해도 요청은 1회만 전송된다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val deleteGate = CompletableDeferred<Unit>()
        repository.deleteOverride = { deleteGate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("2"))
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        viewModel.onIntent(HistoryIntent.CancelDeleteMeal) // 요청 중 취소도 무시된다
        advanceUntilIdle()

        assertEquals(1, repository.deleteRequests.size)
        assertEquals("2", viewModel.uiState.value.deletingMeal?.id)
        deleteGate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.deletingMeal)
    }

    @Test
    fun `월 조회가 실패하면 로딩을 해제하고 일간 조회를 건너뛴다`() = runTest(testDispatcher) {
        repository.monthlyOverride = { _, _ -> throw httpException(500) }
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.isLoading)
        // 월간 조회가 실패하면 일간 조회를 하지 않고 바로 오류를 보여 준다.
        assertEquals(0, repository.dailyRequests.size)
        assertTrue(viewModel.uiState.value.isLoadFailed)
    }

    @Test
    fun `불러오지 못한 뒤 다시 불러오기를 누르면 오류가 풀리고 기록을 보여 준다`() = runTest(testDispatcher) {
        repository.monthlyOverride = { _, _ -> throw IOException("offline") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        repository.monthlyOverride = { year, month -> HistoryCalendarVO(year, month) }
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        viewModel.onIntent(HistoryIntent.RetryLoad)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadFailed)
        assertFalse(state.isLoading)
        assertEquals(2, state.selectedDayMeals.size)
    }

    @Test
    fun `달을 넘기면 이전 날짜의 영양을 비워 실패해도 남지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ ->
            DailyMealHistoryVO(nutrition = DailyNutritionVO(currentCalorieKcal = 1_200))
        }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1_200, viewModel.uiState.value.dailyNutrition.currentCalorieKcal)
        repository.monthlyOverride = { _, _ -> throw IOException("offline") }

        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth.of(todayDate()).plusMonths(-1)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoadFailed)
        assertEquals(DailyNutritionVO.empty, viewModel.uiState.value.dailyNutrition)
    }

    @Test
    fun `이번 달보다 뒤로는 넘어가지 않는다`() = runTest(testDispatcher) {
        val today = todayDate()
        val viewModel = createViewModel()
        advanceUntilIdle()
        val next = HistoryMonth.of(today).plusMonths(1)

        viewModel.onIntent(HistoryIntent.ChangeMonth(next))
        advanceUntilIdle()

        assertEquals(HistoryMonth.of(today), viewModel.uiState.value.displayedHistoryMonth)
        assertFalse((next.year to next.month) in repository.monthlyRequests)
    }

    @Test
    fun `달을 넘기면 데이터를 받기 전에 보고 있는 달부터 바뀐다`() = runTest(testDispatcher) {
        val previous = HistoryMonth.of(todayDate()).plusMonths(-1)
        val gate = CompletableDeferred<HistoryCalendarVO>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.monthlyOverride = { year, month ->
            if (year == previous.year && month == previous.month) gate.await() else HistoryCalendarVO(year, month)
        }

        viewModel.onIntent(HistoryIntent.ChangeMonth(previous))
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(previous, state.displayedHistoryMonth)
        assertTrue(state.isLoading)
        assertTrue(previous.contains(state.selectedDate))
        gate.complete(HistoryCalendarVO(previous.year, previous.month))
    }

    @Test
    fun `달을 넘기면 고른 날은 같은 날짜로 두되 그 달 말일을 넘지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.SelectDate(HistoryDateVO(2025, 1, 31)))
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth(2025, 2)))
        advanceUntilIdle()

        assertEquals(HistoryDateVO(2025, 2, 28), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `이번 달로 돌아오면 고른 날은 오늘을 넘지 않는다`() = runTest(testDispatcher) {
        val today = todayDate()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.SelectDate(HistoryDateVO(2025, 1, 31)))
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ChangeMonth(HistoryMonth.of(today)))
        advanceUntilIdle()

        assertEquals(HistoryDateVO(today.year, today.month, minOf(31, today.day)), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `받은 달의 캘린더는 남겨 두고 지난달은 미리 한 번만 받는다`() = runTest(testDispatcher) {
        val current = HistoryMonth.of(todayDate())
        val previous = current.plusMonths(-1)
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(current in state.calendarMonths)
        assertTrue(previous in state.calendarMonths)

        // 지난달로 넘겼다 돌아와도 이미 받은 지난달은 다시 미리 받지 않는다(보일 때 한 번 새로 받는 것만 센다).
        viewModel.onIntent(HistoryIntent.ChangeMonth(previous))
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ChangeMonth(current))
        advanceUntilIdle()

        assertEquals(2, repository.monthlyRequests.count { it == previous.year to previous.month })
    }

    @Test
    fun `같은 달 날짜를 고르면 칸 표시가 바로 옮겨 가고 기록 자리는 로딩이 된다`() = runTest(testDispatcher) {
        val previous = HistoryMonth.of(todayDate()).plusMonths(-1)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ChangeMonth(previous))
        advanceUntilIdle()
        val target = HistoryDateVO(previous.year, previous.month, if (viewModel.uiState.value.selectedDate.day == 1) 2 else 1)
        val gate = CompletableDeferred<DailyMealHistoryVO>()
        repository.dailyOverride = { _, _, _ -> gate.await() }

        viewModel.onIntent(HistoryIntent.SelectDate(target))
        runCurrent()

        assertEquals(target, viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isLoading)

        gate.complete(dailyWithTwoMeals())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.selectedDayMeals.size)
    }

    @Test
    fun `지금 기록하기를 누르면 식사 기록 화면으로 간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(HistoryIntent.ClickRecordMeal)

        assertEquals(listOf<Page>(MealRecordPage), navigationHelper.pages)
    }

    @Test
    fun `식사를 지우고 그날 기록이 남지 않으면 캘린더 칸의 기록 표시도 지운다`() = runTest(testDispatcher) {
        val today = todayDate()
        repository.monthlyOverride = { year, month ->
            HistoryCalendarVO(
                year = year,
                month = month,
                days = listOf(HistoryCalendarDayVO(date = today, foodIconIds = listOf("7"), mealCount = 1)),
            )
        }
        repository.dailyOverride = { _, _, _ ->
            DailyMealHistoryVO(meals = listOf(MealHistoryVO(id = "1", orderIndex = 1, status = MealAnalysisStatus.FAILED)))
        }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.calendarDays.single { it.inCurrentMonth && it.date == today }.hasRecord)

        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("1"))
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()

        val cell = viewModel.uiState.value.calendarDays.single { it.inCurrentMonth && it.date == today }
        assertFalse(cell.hasRecord)
        assertTrue(cell.foodIconIds.isEmpty())
    }

    @Test
    fun `식사를 지운 뒤 캘린더 칸 아이콘은 아이콘이 있는 식사에서만 고른다`() = runTest(testDispatcher) {
        val today = todayDate()
        repository.monthlyOverride = { year, month ->
            HistoryCalendarVO(
                year = year,
                month = month,
                days = listOf(HistoryCalendarDayVO(date = today, foodIconIds = listOf("9"), mealCount = 3)),
            )
        }
        repository.dailyOverride = { _, _, _ ->
            DailyMealHistoryVO(
                meals = listOf(
                    MealHistoryVO(id = "1", orderIndex = 1, status = MealAnalysisStatus.FAILED),
                    MealHistoryVO(id = "2", orderIndex = 2, status = MealAnalysisStatus.FAILED),
                    MealHistoryVO(id = "3", name = "라면", foodIconId = "9", orderIndex = 3),
                ),
            )
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("1"))
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()

        val cell = viewModel.uiState.value.calendarDays.single { it.inCurrentMonth && it.date == today }
        assertTrue(cell.hasRecord)
        assertEquals(listOf("9"), cell.foodIconIds) // 남은 분석 실패 식사(2)는 아이콘이 없다
    }

    @Test
    fun `재분석을 요청하면 그 식사만 분석 중으로 바뀐다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        val meals = viewModel.uiState.value.selectedDayMeals
        assertEquals(listOf(2L), repository.reanalyzeRequests)
        assertEquals(MealAnalysisStatus.ANALYZING, meals.single { it.id == "2" }.status)
        assertEquals(MealAnalysisStatus.COMPLETED, meals.single { it.id == "1" }.status)
        assertFalse(viewModel.uiState.value.isReanalyzing("2"))
    }

    @Test
    fun `재분석이 실패하면 진행 표시가 풀려 다시 시도할 수 있다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        repository.reanalyzeOverride = { throw httpException(400) }
        val viewModel = createViewModel()
        advanceUntilIdle()
        val dailyRequestsBefore = repository.dailyRequests.size

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReanalyzing("2"))
        assertEquals(MealAnalysisStatus.FAILED, state.selectedDayMeals.single { it.id == "2" }.status)
        // 목록이 낡아서 난 실패가 아니므로 다시 받지 않는다.
        assertEquals(dailyRequestsBefore, repository.dailyRequests.size)
    }

    @Test
    fun `이미 다시 분석한 식사라 재분석이 거절되면 그날 목록을 다시 받아 지금 상태를 보여 준다`() = runTest(testDispatcher) {
        var serverStatus = MealAnalysisStatus.FAILED
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal(status = serverStatus) }
        repository.reanalyzeOverride = { throw httpException(409, "api.meal.analysisNotRetryable") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        val dailyRequestsBefore = repository.dailyRequests.size
        // 다른 곳에서 이미 다시 분석해 서버에서는 분석 중이다. 화면은 아직 실패 카드다.
        serverStatus = MealAnalysisStatus.ANALYZING

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dailyRequestsBefore + 1, repository.dailyRequests.size)
        assertEquals(MealAnalysisStatus.ANALYZING, state.selectedDayMeals.single { it.id == "2" }.status)
        assertFalse(state.isReanalyzing("2"))
    }

    @Test
    fun `지운 식사라 재분석이 거절되면 그날 목록을 다시 받아 카드를 없앤다`() = runTest(testDispatcher) {
        var deleted = false
        repository.dailyOverride = { _, _, _ ->
            dailyWithFailedMeal().let { daily -> if (deleted) daily.copy(meals = daily.meals.filterNot { it.id == "2" }) else daily }
        }
        repository.reanalyzeOverride = { throw httpException(404, "api.meal.notFound") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        deleted = true

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        assertEquals(listOf("1"), viewModel.uiState.value.selectedDayMeals.map { it.id })
    }

    @Test
    fun `재분석 응답 전에는 같은 식사의 재요청을 무시한다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val gate = CompletableDeferred<MealAnalysisStatus>()
        repository.reanalyzeOverride = { gate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReanalyzing("2"))
        assertEquals(listOf(2L), repository.reanalyzeRequests)
        gate.complete(MealAnalysisStatus.ANALYZING)
        advanceUntilIdle()
    }

    @Test
    fun `조용한 새로고침이 끼어들어도 진행 중인 재분석 표시는 유지된다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val gate = CompletableDeferred<MealAnalysisStatus>()
        repository.reanalyzeOverride = { gate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()
        // 상세에서 돌아오는 등 화면이 다시 보이면 같은 날짜를 조용히 새로 받는다.
        viewModel.onIntent(HistoryIntent.ScreenResumed)
        advanceUntilIdle()

        // 서버 응답에는 "2"가 아직 FAILED 지만, 재분석 요청이 끝나기 전이므로 진행 표시를 유지한다.
        assertTrue(viewModel.uiState.value.isReanalyzing("2"))

        gate.complete(MealAnalysisStatus.ANALYZING)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReanalyzing("2"))
    }

    @Test
    fun `분석 실패 기록은 눌러도 상세로 가지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("2"))
        advanceUntilIdle()

        assertTrue(navigationHelper.pages.isEmpty())
    }

    @Test
    fun `분석이 끝난 식사를 누르면 그 식사의 상세 페이지로 간다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))

        assertEquals(listOf<Page>(MealDetailPage(1L)), navigationHelper.pages)
    }

    @Test
    fun `실패 카드의 삭제는 해당 식사의 삭제 확인 다이얼로그를 연다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("2"))
        advanceUntilIdle()

        assertEquals("2", viewModel.uiState.value.deletingMeal?.id)

        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(2L), repository.deleteRequests)
        assertNull(state.deletingMeal)
        assertEquals(listOf("1"), state.selectedDayMeals.map { it.id })
    }

    @Test
    fun `실패 카드에서 연 삭제 확인을 취소하면 다이얼로그를 닫는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("2"))
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.CancelDeleteMeal)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingMeal)
    }

    @Test
    fun `끼니 순번은 분석이 끝난 식사만 세어 매긴다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        val meals = viewModel.uiState.value.selectedDayMeals
        assertEquals(1, meals.single { it.id == "1" }.orderIndex)
        assertEquals(0, meals.single { it.id == "2" }.orderIndex)
        assertEquals(1, viewModel.uiState.value.completedMealCount)
    }

    @Test
    fun `만들기만 해서는 불러오지 않고 화면이 처음 보일 때 불러온다`() = runTest(testDispatcher) {
        val today = todayDate()
        val viewModel = createViewModel(resumed = false)
        advanceUntilIdle()

        assertTrue(repository.monthlyRequests.isEmpty())
        assertTrue(repository.dailyRequests.isEmpty())

        viewModel.onIntent(HistoryIntent.ScreenResumed)
        advanceUntilIdle()

        assertTrue((today.year to today.month) in repository.monthlyRequests)
        assertEquals(listOf(Triple(today.year, today.month, today.day)), repository.dailyRequests)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `다시 보이면 로딩 표시 없이 고른 날 기록을 새로 받는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val gate = CompletableDeferred<DailyMealHistoryVO>()
        repository.dailyOverride = { _, _, _ -> gate.await() }

        viewModel.onIntent(HistoryIntent.ScreenResumed)
        runCurrent()

        // 받는 동안에도 로딩 표시를 하지 않는다.
        assertFalse(viewModel.uiState.value.isLoading)

        gate.complete(dailyWithTwoMeals())
        advanceUntilIdle()

        assertEquals(setOf("1", "2"), viewModel.uiState.value.selectedDayMeals.map { it.id }.toSet())
    }

    @Test
    fun `다시 보일 때 받아오지 못하면 보던 기록을 그대로 둔다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.dailyOverride = { _, _, _ -> throw IOException("offline") }

        viewModel.onIntent(HistoryIntent.ScreenResumed)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf("1", "2"), state.selectedDayMeals.map { it.id }.toSet())
        assertFalse(state.isLoadFailed)
    }

    @Test
    fun `불러오지 못한 화면이면 다시 보일 때 다시 불러온다`() = runTest(testDispatcher) {
        repository.monthlyOverride = { _, _ -> throw IOException("offline") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        repository.monthlyOverride = { year, month -> HistoryCalendarVO(year, month) }
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        viewModel.onIntent(HistoryIntent.ScreenResumed)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadFailed)
        assertEquals(setOf("1", "2"), state.selectedDayMeals.map { it.id }.toSet())
    }

    @Test
    fun `다시 받는 사이 다른 날로 옮기면 늦게 온 그날 기록은 버린다`() = runTest(testDispatcher) {
        val today = todayDate()
        val (prevYear, prevMonth) = previousMonthOf(today.year, today.month)
        val target = HistoryDateVO(prevYear, prevMonth, 15)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val gate = CompletableDeferred<DailyMealHistoryVO>()
        repository.dailyOverride = { year, month, day ->
            if (HistoryDateVO(year, month, day) == today) {
                gate.await()
            } else {
                DailyMealHistoryVO(meals = listOf(MealHistoryVO(id = "15", name = "지난달 기록", orderIndex = 1)))
            }
        }

        viewModel.onIntent(HistoryIntent.ScreenResumed)
        runCurrent() // 오늘 기록을 다시 받는 중
        viewModel.onIntent(HistoryIntent.SelectDate(target))
        advanceUntilIdle()
        gate.complete(dailyWithTwoMeals()) // 옮기기 전 날짜의 늦은 응답
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(target, state.selectedDate)
        assertEquals(listOf("15"), state.selectedDayMeals.map { it.id })
    }

    @Test
    fun `조회가 진행 중일 때 다시 보이면 겹쳐 부르지 않는다`() = runTest(testDispatcher) {
        val today = todayDate()
        val gate = CompletableDeferred<HistoryCalendarVO>()
        repository.monthlyOverride = { year, month ->
            if (year == today.year && month == today.month) gate.await() else HistoryCalendarVO(year, month)
        }
        val viewModel = createViewModel()
        runCurrent() // 첫 조회가 응답을 기다리는 중

        viewModel.onIntent(HistoryIntent.ScreenResumed)
        gate.complete(HistoryCalendarVO(today.year, today.month))
        advanceUntilIdle()

        assertEquals(1, repository.monthlyRequests.count { it == today.year to today.month })
        assertEquals(1, repository.dailyRequests.size)
    }

    /** [resumed]가 true 면 화면이 처음 보일 때처럼 첫 조회를 시작한다. */
    private fun createViewModel(resumed: Boolean = true): HistoryViewModel {
        val resourceHelper = FakeResourceHelper()
        val messageHelper = FakeMessageHelper()
        val ttiHelper = FakeTTIHelper()
        return HistoryViewModel(
            getMonthlyMeals = GetMonthlyMealsUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            getDailyMeals = GetDailyMealsUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            getMonthWithDayMeals = GetMonthWithDayMealsUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            deleteMeal = DeleteMealUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            reanalyzeMeal = ReanalyzeMealUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            navigationHelper = navigationHelper,
        ).also {
            // ViewModel 생성만으로는 불러오지 않는다.
            if (resumed) it.onIntent(HistoryIntent.ScreenResumed)
        }
    }

    private fun dailyWithTwoMeals(): DailyMealHistoryVO = DailyMealHistoryVO(
        meals = listOf(
            MealHistoryVO(id = "1", name = "김치찌개 A", orderIndex = 1),
            MealHistoryVO(id = "2", name = "김치찌개 B", orderIndex = 2),
        ),
    )

    /** 완료 1건 + 분석 실패 1건이 섞인 하루. */
    private fun dailyWithFailedMeal(status: MealAnalysisStatus = MealAnalysisStatus.FAILED): DailyMealHistoryVO = DailyMealHistoryVO(
        meals = listOf(
            MealHistoryVO(id = "1", name = "김치찌개 A", orderIndex = 1),
            MealHistoryVO(
                id = "2",
                recordedAt = "12:30",
                orderIndex = 2,
                status = status,
            ),
        ),
    )

    /** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
    private fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/meals",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

}
