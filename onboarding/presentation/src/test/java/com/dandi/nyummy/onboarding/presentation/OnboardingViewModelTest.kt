package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.onboarding.domain.CatNameError
import com.dandi.nyummy.onboarding.domain.FinishOnboardingUseCase
import com.dandi.nyummy.onboarding.domain.OnboardingRepository
import com.dandi.nyummy.onboarding.domain.RegisterCatUseCase
import com.dandi.nyummy.onboarding.entity.CatVO
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

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeOnboardingRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private lateinit var viewModel: OnboardingViewModel

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(
            registerCatUseCase = RegisterCatUseCase(
                repository = repository,
                resourceHelper = FakeResourceHelper,
                messageHelper = SilentMessageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper,
            ),
            finishOnboardingUseCase = FinishOnboardingUseCase(
                repository = repository,
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

    /** 현재 대사를 다 보여준 뒤 다음으로 넘기는 탭 두 번. */
    private fun revealAndAdvance() {
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        viewModel.onIntent(OnboardingIntent.TapDialogue)
    }

    @Test
    fun `타이핑 중 탭은 대사를 끝까지 보여주고, 다음 탭에 다음 대사로 넘어간다`() {
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertTrue(state.isLineRevealed)
        assertEquals(0, state.lineIndex)

        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertEquals(1, state.lineIndex)
        assertFalse(state.isLineRevealed)
    }

    @Test
    fun `장면의 마지막 대사 뒤 탭은 다음 장면으로 넘어간다`() {
        repeat(OnboardingScript.scenes[0].lines.size) { revealAndAdvance() }

        assertEquals(1, state.sceneIndex)
        assertEquals(0, state.lineIndex)
    }

    @Test
    fun `선택지가 뜬 동안 탭은 무시되고, 고르면 반응 대사를 본 뒤 다음 장면으로 넘어간다`() {
        repeat(OnboardingScript.scenes[0].lines.size) { revealAndAdvance() }
        val choiceScene = OnboardingScript.scenes[1]
        repeat(choiceScene.lines.size - 1) { revealAndAdvance() }
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertTrue(state.isActionVisible)

        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertEquals(1, state.sceneIndex)

        viewModel.onIntent(OnboardingIntent.SelectChoice(1))
        assertEquals(1, state.choiceIndex)
        assertEquals(OnboardingSpeaker.USER, state.currentLine.speaker)

        val action = choiceScene.action as OnboardingSceneAction.Choice
        repeat(action.options[1].reactionLines.size) { revealAndAdvance() }
        assertEquals(2, state.sceneIndex)
        assertNull(state.choiceIndex)
    }

    @Test
    fun `사용자 실루엣은 선택지를 고른 뒤 사용자가 말할 때만 무대에 선다`() {
        // 첫 장면은 나레이션뿐이라 실루엣이 없다.
        assertFalse(state.isUserOnStage)

        repeat(OnboardingScript.scenes[0].lines.size) { revealAndAdvance() }
        assertFalse(state.isUserOnStage) // 고양이 대사

        repeat(OnboardingScript.scenes[1].lines.size - 1) { revealAndAdvance() }
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertFalse(state.isUserOnStage) // 선택지를 고르는 중에는 아직 없다

        viewModel.onIntent(OnboardingIntent.SelectChoice(0))
        assertTrue(state.isUserOnStage) // 사용자 반응 대사

        revealAndAdvance()
        assertFalse(state.isUserOnStage) // 나레이션
    }

    @Test
    fun `이름 짓기 장면은 이름 입력창이 떠 있어도 실루엣을 세우지 않는다`() {
        viewModel.onIntent(OnboardingIntent.Skip)
        repeat(state.currentLines.size - 1) { revealAndAdvance() }
        viewModel.onIntent(OnboardingIntent.TapDialogue)

        assertTrue(state.isActionVisible)
        assertFalse(state.isUserOnStage)
    }

    @Test
    fun `이름을 받은 축하 장면도 마지막 대사 전까지는 탭해서 계속 칩을 띄운다`() = runTest(testDispatcher) {
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName("냐미"))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        advanceUntilIdle()

        viewModel.onIntent(OnboardingIntent.TypingFinished)
        assertTrue(state.isContinueHintVisible)

        repeat(state.currentLines.size - 1) { revealAndAdvance() }
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        // 마지막 대사에서는 시작 버튼이 뜨고 칩은 숨는다.
        assertTrue(state.isActionVisible)
        assertFalse(state.isContinueHintVisible)
    }

    @Test
    fun `건너뛰기는 이름 짓기 장면으로 바로 보내고, 그 뒤에는 숨는다`() {
        viewModel.onIntent(OnboardingIntent.Skip)

        assertEquals(OnboardingScript.namingSceneIndex, state.sceneIndex)
        assertFalse(state.isSkipVisible)
    }

    @Test
    fun `빈 이름은 서버를 부르지 않고 에러를 보여준다`() = runTest(testDispatcher) {
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName("   "))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        advanceUntilIdle()

        assertEquals(CatNameError.EMPTY, state.catNameError)
        assertTrue(repository.registeredNames.isEmpty())
    }

    @Test
    fun `이름을 등록하면 이름을 받은 축하 장면으로 넘어가고 이름표가 바뀐다`() = runTest(testDispatcher) {
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName(" 냐미 "))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        assertTrue(state.isSubmitting)
        advanceUntilIdle()

        assertEquals(listOf("냐미"), repository.registeredNames)
        assertEquals("냐미", state.catName)
        assertFalse(state.isSubmitting)
        assertEquals(OnboardingCharacter.CELEBRATE, state.currentScene.character)
    }

    @Test
    fun `등록에 실패하면 이름 장면에 머물고 다시 시도할 수 있다`() = runTest(testDispatcher) {
        repository.error = IllegalStateException("boom")
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName("냐미"))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        advanceUntilIdle()

        assertEquals(OnboardingScript.namingSceneIndex, state.sceneIndex)
        assertFalse(state.isSubmitting)
        assertEquals("", state.catName)
    }

    @Test
    fun `이름을 받은 뒤 시작하기를 누르면 평소 식사 시각 장면으로 넘어간다`() = runTest(testDispatcher) {
        registerCatAndReachStart()

        viewModel.onIntent(OnboardingIntent.ClickStart)

        assertEquals(OnboardingSceneAction.MealTime, state.currentScene.action)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `식사 시각 장면은 기본값을 채워 두고 대사가 끝나야 시간 시트를 연다`() = runTest(testDispatcher) {
        reachMealTimeScene(revealLine = false)
        assertEquals(MealTimesVO.default, state.mealTimes)

        viewModel.onIntent(OnboardingIntent.ClickMealTime(Meal.LUNCH))
        assertNull(state.editingMeal)

        viewModel.onIntent(OnboardingIntent.TypingFinished)
        viewModel.onIntent(OnboardingIntent.ClickMealTime(Meal.LUNCH))
        assertEquals(Meal.LUNCH, state.editingMeal)
    }

    @Test
    fun `시간 시트에서 고른 시각과 안 먹어요가 그 끼니에만 반영되고 시트가 닫힌다`() = runTest(testDispatcher) {
        reachMealTimeScene()

        viewModel.onIntent(OnboardingIntent.ClickMealTime(Meal.LUNCH))
        viewModel.onIntent(OnboardingIntent.SelectMealTime(Meal.LUNCH, MealTimeVO(hour = 13)))
        viewModel.onIntent(OnboardingIntent.ClickMealTime(Meal.BREAKFAST))
        viewModel.onIntent(OnboardingIntent.SelectMealTime(Meal.BREAKFAST, MealTimesVO.DefaultBreakfast.copy(isSkipped = true)))

        assertNull(state.editingMeal)
        assertEquals(MealTimeVO(hour = 13), state.mealTimes.lunch)
        assertTrue(state.mealTimes.breakfast.isSkipped)
        assertEquals(MealTimesVO.DefaultDinner, state.mealTimes.dinner)
    }

    @Test
    fun `시트를 닫으면 시각은 그대로다`() = runTest(testDispatcher) {
        reachMealTimeScene()

        viewModel.onIntent(OnboardingIntent.ClickMealTime(Meal.DINNER))
        viewModel.onIntent(OnboardingIntent.DismissMealTimeSheet)

        assertNull(state.editingMeal)
        assertEquals(MealTimesVO.default, state.mealTimes)
    }

    @Test
    fun `식사 시각을 확인하면 한 번만 저장하고 홈을 루트로 이동한다`() = runTest(testDispatcher) {
        reachMealTimeScene()
        viewModel.onIntent(OnboardingIntent.SelectMealTime(Meal.DINNER, MealTimeVO(hour = 19)))

        viewModel.onIntent(OnboardingIntent.ConfirmMealTimes)
        // 권한 응답 등으로 두 번 들어와도 저장과 이동은 한 번이다.
        viewModel.onIntent(OnboardingIntent.ConfirmMealTimes)
        advanceUntilIdle()

        assertTrue(state.isFinishing)
        assertEquals(listOf(MealTimesVO(dinner = MealTimeVO(hour = 19))), repository.savedMealTimes)
        assertEquals(listOf("/home"), navigationHelper.rootPages.map { it.toRoute().path })
    }

    @Test
    fun `식사 시각 장면 전에는 확인을 보내도 아무 일도 없다`() = runTest(testDispatcher) {
        viewModel.onIntent(OnboardingIntent.ConfirmMealTimes)
        advanceUntilIdle()

        assertTrue(repository.savedMealTimes.isEmpty())
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    /** 이름을 등록하고 축하 장면의 마지막 대사까지 넘겨 시작 버튼이 뜬 상태로 만든다. */
    private fun TestScope.registerCatAndReachStart() {
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName("냐미"))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        advanceUntilIdle()
        repeat(state.currentLines.size - 1) { revealAndAdvance() }
        viewModel.onIntent(OnboardingIntent.TapDialogue)
        assertEquals(OnboardingSceneAction.Start, state.currentScene.action)
        assertTrue(state.isActionVisible)
    }

    private fun TestScope.reachMealTimeScene(revealLine: Boolean = true) {
        registerCatAndReachStart()
        viewModel.onIntent(OnboardingIntent.ClickStart)
        if (revealLine) viewModel.onIntent(OnboardingIntent.TypingFinished)
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        var error: Exception? = null
        val registeredNames = mutableListOf<String>()

        override suspend fun registerCat(name: String): CatVO {
            error?.let { throw it }
            registeredNames += name
            return CatVO(id = 1L, name = name)
        }

        override suspend fun markOnboardingComplete() = Unit

        val savedMealTimes = mutableListOf<MealTimesVO>()

        override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
            savedMealTimes += mealTimes
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val rootPages = mutableListOf<Page>()

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) {
            rootPages += page
        }

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
