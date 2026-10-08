package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
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
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.HistoryRepository
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionVO
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val homeRepository = FakeHomeRepository()
    private val historyRepository = FakeHistoryRepository()
    private val catRepository = FakeCatRepository()
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
            getCatAnimations = GetCatAnimationsUseCase(
                repository = catRepository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            catMotionPicker = CatMotionPicker(Random(seed = 1)),
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
        val meal = MealHistoryVO(id = "1", name = "닭가슴살 샐러드")
        val nutrition = DailyNutritionVO(currentCalorieKcal = 420)
        historyRepository.next = { DailyMealHistoryVO(meals = listOf(meal), nutrition = nutrition) }
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))

        // 요청 사이에 KST 자정이 지나도 실패하지 않도록 전후 날짜를 모두 허용한다.
        val before = KstTime.now().let { Triple(it.year, it.month, it.day) }
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        assertTrue(state.isTodaySheetVisible)
        assertTrue(state.isTodayMealsLoading)
        advanceUntilIdle()
        val after = KstTime.now().let { Triple(it.year, it.month, it.day) }

        assertTrue(historyRepository.dailyCalls.single() in setOf(before, after))
        assertEquals(nutrition, state.todayNutrition)
        assertEquals(listOf(meal), state.todayMeals)
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
        assertNull(state.todayNutrition)

        historyRepository.next = { DailyMealHistoryVO.empty }
        viewModel.onIntent(HomeIntent.RetryTodayMeals)
        advanceUntilIdle()

        assertFalse(state.isTodayMealsFailed)
        assertEquals(DailyNutritionVO.empty, state.todayNutrition)
        assertTrue(state.todayMeals.isEmpty())
        assertEquals(2, historyRepository.dailyCalls.size)
    }

    @Test
    fun `한 번 읽은 뒤 다시 열 때 실패하면 실패 상태가 된다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.DismissTodaySheet)

        historyRepository.next = { throw IOException("offline") }
        viewModel.onIntent(HomeIntent.ClickTodayBar)
        advanceUntilIdle()

        // 화면은 실패를 먼저 보고 다시 시도를 띄운다(HomeScreenTest 참고).
        assertTrue(state.isTodayMealsFailed)
        assertFalse(state.isTodayMealsLoading)
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

    @Test
    fun `오늘 기록이 없으면 기다리는 냐미를 보여 주고 대사도 그 상태에서 고른다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        val motion = state.catMotion!!
        assertEquals(sheetUrls(CatState.CONFLICTED_CAUTIOUS), motion.sheetUrls)
        assertEquals(listOf(sheetUrl(CatState.CONFLICTED_CAUTIOUS, state.catGroup)), motion.clips.map { it.url })
        assertEquals(state.catPlayId, motion.playId)
        assertTrue(state.catLine in lines(CatState.CONFLICTED_CAUTIOUS))
        assertTrue(motion.restMillis in CatMotionPicker.MIN_REST_MILLIS..CatMotionPicker.MAX_REST_MILLIS)
    }

    @Test
    fun `처음 들어왔을 때 오늘 기록이 있으면 먹는 반응 없이 바로 여유로운 냐미다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))

        assertEquals(CatState.RELAXED, state.catState)
    }

    @Test
    fun `식사를 기록하고 돌아오면 먹는 반응을 보여 주고, 끝나면 여유로 돌아간다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.CONTENT, state.catState)
        assertTrue(state.catLine in lines(CatState.CONTENT))

        // 먹는 반응 중에 다시 돌아와도(기록 수 그대로) 끊지 않는다.
        val playId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        assertEquals(CatState.CONTENT, state.catState)
        assertEquals(playId, state.catPlayId)

        viewModel.onIntent(HomeIntent.CatMotionFinished(playId))
        assertEquals(CatState.RELAXED, state.catState)
        assertTrue(state.catLine in lines(CatState.RELAXED))
    }

    @Test
    fun `동작이 끝나면 같은 상태의 다른 동작으로 넘어가고 대사는 그대로 둔다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        val before = state

        viewModel.onIntent(HomeIntent.CatMotionFinished(before.catPlayId))

        assertEquals(CatState.CONFLICTED_CAUTIOUS, state.catState)
        assertNotEquals(before.catGroup, state.catGroup)
        assertEquals(before.catPlayId + 1, state.catPlayId)
        assertEquals(before.catLine, state.catLine)
    }

    @Test
    fun `늦게 도착한 이전 동작의 종료 신호는 무시한다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        val stalePlayId = state.catPlayId
        resumeWith(HomeSummaryVO(todayRecordedCount = 1))
        val content = state

        viewModel.onIntent(HomeIntent.CatMotionFinished(stalePlayId))

        assertEquals(content, state)
    }

    @Test
    fun `냐미를 누르면 같은 상태의 다른 동작과 새 대사를 보여 준다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 2))
        val before = state

        viewModel.onIntent(HomeIntent.ClickCat)

        assertEquals(CatState.RELAXED, state.catState)
        assertNotEquals(before.catGroup, state.catGroup)
        assertNotEquals(before.catLine, state.catLine)
        assertTrue(state.catLine in lines(CatState.RELAXED))
    }

    @Test
    fun `고른 상태가 응답에 없으면 기본 인사 동작과 대사를 쓴다`() = runTest(testDispatcher) {
        catRepository.next = { animationSet(CatState.FRIENDLY) }

        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(sheetUrls(CatState.FRIENDLY), state.catMotion?.sheetUrls)
        assertTrue(state.catLine in lines(CatState.FRIENDLY))
    }

    @Test
    fun `고양이가 없으면 기본 냐미로 대신하고, 다음에 화면이 보일 때 다시 받는다`() = runTest(testDispatcher) {
        catRepository.next = { throw notFound() }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertTrue(state.isCatAnimationFailed)
        assertNull(state.catMotion)
        assertNull(state.catLine)
        assertTrue(navigationHelper.pages.isEmpty())

        catRepository.next = { animationSet(CatState.CONFLICTED_CAUTIOUS) }
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertFalse(state.isCatAnimationFailed)
        assertEquals(sheetUrls(CatState.CONFLICTED_CAUTIOUS), state.catMotion?.sheetUrls)
        assertEquals(2, catRepository.callCount)
    }

    @Test
    fun `애니메이션은 한 번 받으면 화면에 돌아올 때 다시 받지 않는다`() = runTest(testDispatcher) {
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))
        resumeWith(HomeSummaryVO(todayRecordedCount = 0))

        assertEquals(1, catRepository.callCount)
    }

    private fun lines(state: CatState) = listOf("${state.name} 대사 1", "${state.name} 대사 2", "${state.name} 대사 3")

    private fun sheetUrl(state: CatState, group: Int) = "https://cdn/${state.name}_$group.png"

    private fun sheetUrls(state: CatState) = (0..2).map { sheetUrl(state, it) }

    private fun animation(state: CatState) = CatAnimationVO(
        state = state,
        frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
        groups = (0..2).map { listOf(SpriteClipVO(url = sheetUrl(state, it), frames = 8)) },
        lines = lines(state),
    )

    private fun animationSet(vararg states: CatState) =
        CatAnimationSetVO(weight = "NORMAL", animations = states.associateWith { animation(it) })

    private fun notFound() = HttpResponseException(
        status = HttpResponseStatus.NotFound,
        rawCode = 404,
        errorRequestUrl = "https://test/api/v1/cats/animations",
        cause = Throwable("api.cat.notFound"),
    )

    private inner class FakeCatRepository : CatRepository {
        var next: () -> CatAnimationSetVO = {
            animationSet(CatState.CONFLICTED_CAUTIOUS, CatState.RELAXED, CatState.CONTENT, CatState.FRIENDLY)
        }
        var callCount = 0
            private set

        override suspend fun getAnimations(): CatAnimationSetVO {
            callCount++
            return next()
        }
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
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }
}
