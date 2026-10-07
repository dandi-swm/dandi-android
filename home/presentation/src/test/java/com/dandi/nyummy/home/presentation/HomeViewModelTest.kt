package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.HistoryRepository
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.home.domain.GetHomeSummaryUseCase
import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val homeRepository = FakeHomeRepository()
    private val historyRepository = FakeHistoryRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private lateinit var viewModel: HomeViewModel

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(
            navigationHelper = navigationHelper,
            getHomeSummary = GetHomeSummaryUseCase(
                repository = homeRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            getDailyMeals = GetDailyMealsUseCase(
                repository = historyRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.resumeWith(summary: HomeSummaryVO) {
        homeRepository.next = { summary }
        viewModel.onIntent(HomeIntent.ScreenResumed)
        advanceUntilIdle()
    }

    @Test
    fun `화면이 보일 때마다 홈 요약을 다시 읽는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100, todayRecordedCount = 0))
        assertEquals(100, state.summary.coinBalance)
        assertFalse(state.hasRecordedToday)

        // 식사를 기록하고 돌아온 경우
        resumeWith(HomeSummaryVO(coinBalance = 110, todayRecordedCount = 1))

        assertEquals(2, homeRepository.callCount)
        assertEquals(110, state.summary.coinBalance)
        assertTrue(state.hasRecordedToday)
    }

    @Test
    fun `요약을 읽지 못하면 직전 값을 그대로 둔다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(coinBalance = 100))

        homeRepository.next = { throw IOException("offline") }
        viewModel.onIntent(HomeIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(100, state.summary.coinBalance)
    }

    @Test
    fun `오늘 기록이 없으면 오늘 바는 식사 기록으로 보내고 시트를 열지 않는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()

        assertEquals(listOf<Page>(MealRecordPage), navigationHelper.pages)
        assertFalse(state.isTodaySheetVisible)
        assertEquals(0, historyRepository.dailyCalls.size)
    }

    @Test
    fun `오늘 기록이 있으면 시트를 열고 KST 오늘 식사를 읽는다`() = runTest(testDispatcher) {
        val meals = DailyMealHistoryVO(meals = listOf(MealHistoryVO(id = "1", name = "닭가슴살 샐러드")))
        historyRepository.next = { meals }
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))

        viewModel.onIntent(HomeIntent.ClickTodayBar)
        assertTrue(state.isTodaySheetVisible)
        assertTrue(state.isTodayMealsLoading)
        advanceUntilIdle()

        val today = KstTime.now()
        assertEquals(listOf(Triple(today.year, today.month, today.day)), historyRepository.dailyCalls)
        assertEquals(meals, state.todayMeals)
        assertFalse(state.isTodayMealsLoading)
        assertTrue(navigationHelper.pages.isEmpty())
    }

    @Test
    fun `오늘 식사를 읽지 못하면 실패 상태가 되고 다시 시도하면 다시 읽는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))
        historyRepository.next = { throw IOException("offline") }

        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()
        assertTrue(state.isTodayMealsFailed)
        assertNull(state.todayMeals)

        historyRepository.next = { DailyMealHistoryVO.empty }
        viewModel.onIntent(HomeIntent.RetryTodayMeals)
        advanceUntilIdle()

        assertFalse(state.isTodayMealsFailed)
        assertEquals(DailyMealHistoryVO.empty, state.todayMeals)
        assertEquals(2, historyRepository.dailyCalls.size)
    }

    @Test
    fun `시트를 닫았다 다시 열면 방금 기록한 식사가 보이도록 새로 읽는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()

        viewModel.onIntent(HomeIntent.DismissTodaySheet)
        assertFalse(state.isTodaySheetVisible)
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()

        assertTrue(state.isTodaySheetVisible)
        assertEquals(2, historyRepository.dailyCalls.size)
    }

    @Test
    fun `식사 추가하기는 시트를 닫고 식사 기록으로 보낸다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()

        viewModel.onIntent(HomeIntent.ClickAddMeal)

        assertFalse(state.isTodaySheetVisible)
        assertEquals(listOf<Page>(MealRecordPage), navigationHelper.pages)
    }

    @Test
    fun `방 메뉴 버튼은 펼침과 접힘을 오간다`() {
        assertFalse(state.isRoomMenuExpanded)

        viewModel.onIntent(HomeIntent.ToggleRoomMenu)
        assertTrue(state.isRoomMenuExpanded)

        viewModel.onIntent(HomeIntent.ToggleRoomMenu)
        assertFalse(state.isRoomMenuExpanded)
    }

    @Test
    fun `kcal 막대는 목표 대비 비율이고 0에서 1 사이로 자르며 목표가 없으면 0이다`() {
        fun progress(today: Int, goal: Int) =
            HomeUIState(summary = HomeSummaryVO(todayCalorieKcal = today, goalCalorieKcal = goal)).calorieProgress

        assertEquals(0.75f, progress(1350, 1800), 0.0001f)
        assertEquals(1f, progress(2500, 1800), 0f)
        assertEquals(0f, progress(500, 0), 0f)
    }

    private class FakeHomeRepository : HomeRepository {
        var next: () -> HomeSummaryVO = { HomeSummaryVO.empty }
        var callCount = 0
            private set

        override suspend fun getHomeSummary(): HomeSummaryVO {
            callCount++
            return next()
        }
    }

    private class FakeHistoryRepository : HistoryRepository {
        var next: () -> DailyMealHistoryVO = { DailyMealHistoryVO.empty }
        val dailyCalls = mutableListOf<Triple<Int, Int, Int>>()

        override suspend fun getDailyMeals(year: Int, month: Int, day: Int): DailyMealHistoryVO {
            dailyCalls += Triple(year, month, day)
            return next()
        }

        override suspend fun getMonthlyCalendar(year: Int, month: Int) = HistoryCalendarVO.empty
        override suspend fun getMeal(mealId: Long) = MealHistoryVO.empty
        override suspend fun updateMealName(mealId: Long, name: String) = MealHistoryVO.empty
        override suspend fun reanalyzeMeal(mealId: Long) = MealHistoryVO.empty
        override suspend fun deleteMeal(mealId: Long) = Unit
    }

    private class RecordingNavigationHelper : NavigationHelper {
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

    private object SilentMessageHelper : MessageHelper {
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

    private object FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private object FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
