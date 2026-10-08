package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.domain.analysis.MealAnalysisEvent
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MealAnalysisEventHelper
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
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.HistoryRepository
import com.dandi.nyummy.history.domain.ReanalyzeMealUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.isoDateOf
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import com.dandi.nyummy.history.presentation.util.todayDate
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeHistoryRepository()
    private val analysisEvents = FakeMealAnalysisEventHelper()
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
    fun `먼저 연 상세의 늦은 사진 응답은 다른 상세에 적용되지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val photoGate = CompletableDeferred<MealHistoryVO>()
        repository.mealOverride = { mealId ->
            if (mealId == 1L) photoGate.await() else MealHistoryVO(id = mealId.toString())
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        advanceUntilIdle() // 상세 1 의 사진 요청이 대기 중
        viewModel.onIntent(HistoryIntent.DismissMealDetail)
        viewModel.onIntent(HistoryIntent.ClickMeal("2"))
        photoGate.complete(MealHistoryVO(id = "1", photoUrl = "https://old.photo"))
        advanceUntilIdle()

        val detail = viewModel.uiState.value.mealDetail
        assertEquals("2", detail?.meal?.id)
        assertEquals("", detail?.meal?.photoUrl)
    }

    @Test
    fun `같은 상세를 다시 열면 이전 요청의 응답은 반영되지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val firstGate = CompletableDeferred<MealHistoryVO>()
        val secondGate = CompletableDeferred<MealHistoryVO>()
        var requestCount = 0
        repository.mealOverride = {
            if (requestCount++ == 0) firstGate.await() else secondGate.await()
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.DismissMealDetail)
        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        advanceUntilIdle()

        secondGate.complete(MealHistoryVO(id = "1", photoUrl = "https://new.photo"))
        advanceUntilIdle()
        firstGate.complete(MealHistoryVO(id = "1", photoUrl = "https://old.photo"))
        advanceUntilIdle()

        assertEquals("https://new.photo", viewModel.uiState.value.mealDetail?.meal?.photoUrl)
    }

    @Test
    fun `상세 응답의 냐미 한마디는 목록 정보를 유지한 채 상세에 합쳐진다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        repository.mealOverride = { mealId ->
            MealHistoryVO(
                id = mealId.toString(),
                catComment = "채소가 듬뿍이라 냐미도 기분 좋아!",
                foodIconId = "12",
            )
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        advanceUntilIdle()

        val detail = viewModel.uiState.value.mealDetail
        assertEquals("1", detail?.meal?.id)
        assertEquals("채소가 듬뿍이라 냐미도 기분 좋아!", detail?.meal?.catComment)
        assertEquals("12", detail?.meal?.foodIconId)
        assertEquals("김치찌개 A", detail?.meal?.name) // 상세 응답이 비어 있어도 목록의 이름은 유지된다
    }

    @Test
    fun `이름 수정 응답이 늦게 와도 대상 식사만 갱신하고 열려 있는 다른 상세는 건드리지 않는다`() =
        runTest(testDispatcher) {
            repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
            val updateGate = CompletableDeferred<MealHistoryVO>()
            repository.updateOverride = { _, _ -> updateGate.await() }
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(HistoryIntent.ClickMeal("1"))
            viewModel.onIntent(HistoryIntent.ClickEditMealName)
            viewModel.onIntent(HistoryIntent.ChangeMealNameDraft("연어 포케"))
            viewModel.onIntent(HistoryIntent.ConfirmEditMealName)
            advanceUntilIdle() // 저장 요청이 대기 중
            viewModel.onIntent(HistoryIntent.DismissMealDetail)
            viewModel.onIntent(HistoryIntent.ClickMeal("2"))
            viewModel.onIntent(HistoryIntent.ClickEditMealName)
            viewModel.onIntent(HistoryIntent.ChangeMealNameDraft("다른 이름"))
            updateGate.complete(MealHistoryVO(id = "1", name = "연어 포케"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("연어 포케", state.selectedDayMeals.first { it.id == "1" }.name)
            assertEquals("2", state.mealDetail?.meal?.id)
            assertEquals("김치찌개 B", state.mealDetail?.meal?.name)
            assertEquals("다른 이름", state.mealDetail?.nameDraft)
            assertEquals(HistoryMealDetailMode.EditingName, state.mealDetail?.mode)
        }

    @Test
    fun `삭제 응답은 대상 식사만 제거하고 다른 상세를 닫지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val deleteGate = CompletableDeferred<Unit>()
        repository.deleteOverride = { deleteGate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        viewModel.onIntent(HistoryIntent.ClickDeleteMeal)
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle() // 삭제 요청이 대기 중
        viewModel.onIntent(HistoryIntent.DismissMealDetail)
        viewModel.onIntent(HistoryIntent.ClickMeal("2"))
        deleteGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.selectedDayMeals.none { it.id == "1" })
        assertEquals("2", state.mealDetail?.meal?.id)
    }

    @Test
    fun `삭제 확인을 연타해도 요청은 1회만 전송된다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val deleteGate = CompletableDeferred<Unit>()
        repository.deleteOverride = { deleteGate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        viewModel.onIntent(HistoryIntent.ClickDeleteMeal)
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        viewModel.onIntent(HistoryIntent.CancelDeleteMeal) // 요청 중 취소도 무시된다
        advanceUntilIdle()

        assertEquals(1, repository.deleteRequests.size)
        assertEquals(
            HistoryMealDetailMode.ConfirmingDelete,
            viewModel.uiState.value.mealDetail?.mode,
        )
        deleteGate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.mealDetail)
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
        repository.dailyOverride = { _, _, _ -> DailyMealHistoryVO(meals = listOf(MealHistoryVO(id = "1", name = "김밥", orderIndex = 1))) }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.calendarDays.single { it.inCurrentMonth && it.date == today }.hasRecord)

        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        viewModel.onIntent(HistoryIntent.ClickDeleteMeal)
        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()

        val cell = viewModel.uiState.value.calendarDays.single { it.inCurrentMonth && it.date == today }
        assertFalse(cell.hasRecord)
        assertTrue(cell.foodIconIds.isEmpty())
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

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReanalyzing("2"))
        assertEquals(MealAnalysisStatus.FAILED, state.selectedDayMeals.single { it.id == "2" }.status)
    }

    @Test
    fun `재분석 응답 전에는 같은 식사의 재요청을 무시한다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val gate = CompletableDeferred<MealHistoryVO>()
        repository.reanalyzeOverride = { gate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReanalyzing("2"))
        assertEquals(listOf(2L), repository.reanalyzeRequests)
        gate.complete(MealHistoryVO(id = "2", status = MealAnalysisStatus.ANALYZING))
        advanceUntilIdle()
    }

    @Test
    fun `분석 완료 알림을 받으면 로딩 표시 없이 선택 날짜를 다시 불러온다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()
        val requestsBefore = repository.dailyRequests.size
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }

        analysisEvents.publish(
            MealAnalysisEvent(mealId = "2", date = isoDateOf(viewModel.uiState.value.selectedDate)),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(requestsBefore + 1, repository.dailyRequests.size)
        assertFalse(state.isLoading)
        assertEquals(2, state.completedMealCount)
    }

    @Test
    fun `조용한 새로고침이 끼어들어도 진행 중인 재분석 표시는 유지된다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val gate = CompletableDeferred<MealHistoryVO>()
        repository.reanalyzeOverride = { gate.await() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickRetryAnalysis("2"))
        advanceUntilIdle()
        // 다른 식사의 분석 완료 푸시가 같은 날짜의 조용한 새로고침을 트리거한다.
        analysisEvents.publish(
            MealAnalysisEvent(mealId = "1", date = isoDateOf(viewModel.uiState.value.selectedDate)),
        )
        advanceUntilIdle()

        // 서버 응답에는 "2"가 아직 FAILED 지만, 재분석 요청이 끝나기 전이므로 진행 표시를 유지한다.
        assertTrue(viewModel.uiState.value.isReanalyzing("2"))

        gate.complete(MealHistoryVO(id = "2", status = MealAnalysisStatus.ANALYZING))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReanalyzing("2"))
    }

    @Test
    fun `다른 날짜의 분석 완료 알림은 화면을 다시 불러오지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val requestsBefore = repository.dailyRequests.size

        analysisEvents.publish(MealAnalysisEvent(mealId = "9", date = "1999-01-01"))
        advanceUntilIdle()

        assertEquals(requestsBefore, repository.dailyRequests.size)
    }

    @Test
    fun `분석 실패 기록은 눌러도 상세가 열리지 않는다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickMeal("2"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.mealDetail)
    }

    @Test
    fun `실패 카드의 삭제는 해당 식사의 삭제 확인 다이얼로그를 연다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("2"))
        advanceUntilIdle()

        val detail = viewModel.uiState.value.mealDetail
        assertEquals("2", detail?.meal?.id)
        assertEquals(HistoryMealDetailMode.ConfirmingDelete, detail?.mode)

        viewModel.onIntent(HistoryIntent.ConfirmDeleteMeal)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(2L), repository.deleteRequests)
        assertNull(state.mealDetail)
        assertEquals(listOf("1"), state.selectedDayMeals.map { it.id })
    }

    @Test
    fun `실패 카드에서 연 삭제 확인을 취소하면 상세가 아니라 목록으로 돌아간다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithFailedMeal() }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickDeleteFailedMeal("2"))
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.CancelDeleteMeal)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.mealDetail)
    }

    @Test
    fun `완료된 식사의 삭제 확인을 취소하면 상세 보기로 돌아간다`() = runTest(testDispatcher) {
        repository.dailyOverride = { _, _, _ -> dailyWithTwoMeals() }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickMeal("1"))
        advanceUntilIdle()
        viewModel.onIntent(HistoryIntent.ClickDeleteMeal)
        advanceUntilIdle()

        viewModel.onIntent(HistoryIntent.CancelDeleteMeal)
        advanceUntilIdle()

        assertEquals(HistoryMealDetailMode.Viewing, viewModel.uiState.value.mealDetail?.mode)
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

    private fun createViewModel(): HistoryViewModel {
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
            getMealDetail = GetMealDetailUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            updateMealName = UpdateMealNameUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            deleteMeal = DeleteMealUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            reanalyzeMeal = ReanalyzeMealUseCase(
                repository, resourceHelper, messageHelper, navigationHelper, ttiHelper,
            ),
            mealAnalysisEventHelper = analysisEvents,
            navigationHelper = navigationHelper,
        )
    }

    private fun dailyWithTwoMeals(): DailyMealHistoryVO = DailyMealHistoryVO(
        meals = listOf(
            MealHistoryVO(id = "1", name = "김치찌개 A", orderIndex = 1),
            MealHistoryVO(id = "2", name = "김치찌개 B", orderIndex = 2),
        ),
    )

    /** 완료 1건 + 분석 실패 1건이 섞인 하루. */
    private fun dailyWithFailedMeal(): DailyMealHistoryVO = DailyMealHistoryVO(
        meals = listOf(
            MealHistoryVO(id = "1", name = "김치찌개 A", orderIndex = 1),
            MealHistoryVO(
                id = "2",
                recordedAt = "12:30",
                orderIndex = 2,
                status = MealAnalysisStatus.FAILED,
            ),
        ),
    )

    private fun httpException(code: Int): HttpResponseException = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/meals",
        msg = "Http Request Failed ($code)",
    )

    private class FakeHistoryRepository : HistoryRepository {
        val monthlyRequests = mutableListOf<Pair<Int, Int>>()
        val dailyRequests = mutableListOf<Triple<Int, Int, Int>>()
        val deleteRequests = mutableListOf<Long>()

        var monthlyOverride: suspend (Int, Int) -> HistoryCalendarVO =
            { year, month -> HistoryCalendarVO(year, month) }
        var dailyOverride: suspend (Int, Int, Int) -> DailyMealHistoryVO =
            { _, _, _ -> DailyMealHistoryVO.empty }
        var mealOverride: suspend (Long) -> MealHistoryVO =
            { mealId -> MealHistoryVO(id = mealId.toString()) }
        var updateOverride: suspend (Long, String) -> MealHistoryVO =
            { mealId, name -> MealHistoryVO(id = mealId.toString(), name = name) }
        var deleteOverride: suspend (Long) -> Unit = { }
        val reanalyzeRequests = mutableListOf<Long>()
        var reanalyzeOverride: suspend (Long) -> MealHistoryVO = { mealId ->
            MealHistoryVO(id = mealId.toString(), status = MealAnalysisStatus.ANALYZING)
        }

        override suspend fun getMonthlyCalendar(year: Int, month: Int): HistoryCalendarVO {
            monthlyRequests += year to month
            return monthlyOverride(year, month)
        }

        override suspend fun getDailyMeals(year: Int, month: Int, day: Int): DailyMealHistoryVO {
            dailyRequests += Triple(year, month, day)
            return dailyOverride(year, month, day)
        }

        override suspend fun getMeal(mealId: Long): MealHistoryVO = mealOverride(mealId)

        override suspend fun updateMealName(mealId: Long, name: String): MealHistoryVO =
            updateOverride(mealId, name)

        override suspend fun reanalyzeMeal(mealId: Long): MealHistoryVO {
            reanalyzeRequests += mealId
            return reanalyzeOverride(mealId)
        }

        override suspend fun deleteMeal(mealId: Long) {
            deleteRequests += mealId
            deleteOverride(mealId)
        }
    }

    private class FakeMealAnalysisEventHelper : MealAnalysisEventHelper {
        private val _events = MutableSharedFlow<MealAnalysisEvent>(extraBufferCapacity = 8)
        override val events: Flow<MealAnalysisEvent> = _events

        override fun publish(event: MealAnalysisEvent) {
            _events.tryEmit(event)
        }
    }

    private class FakeNavigationHelper : NavigationHelper {
        val pages = mutableListOf<Page>()
        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) {
            pages += page
        }
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private class FakeMessageHelper : MessageHelper {
        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

        override fun showSnackBar(
            iconType: IconType,
            messageRes: Int,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

        override fun showOneButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            buttonText: String,
            onClickButton: (() -> Unit)?,
        ) = Unit

        override fun showTwoButtonDialog(
            titleText: String?,
            descText: String,
            cantIgnore: Boolean,
            leftButtonText: String,
            onClickLeftButton: (() -> Unit)?,
            rightButtonText: String,
            onClickRightButton: (() -> Unit)?,
        ) = Unit
    }

    private class FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
