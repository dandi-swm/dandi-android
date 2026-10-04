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
    fun `마지막 장면에서 시작하기를 누르면 홈을 루트로 이동한다`() = runTest(testDispatcher) {
        viewModel.onIntent(OnboardingIntent.Skip)
        viewModel.onIntent(OnboardingIntent.InputCatName("냐미"))
        viewModel.onIntent(OnboardingIntent.SubmitCatName)
        advanceUntilIdle()
        repeat(state.currentLines.size) { revealAndAdvance() }
        // 마지막 장면은 탭으로 넘어가지 않는다.
        assertEquals(OnboardingScript.scenes.lastIndex, state.sceneIndex)
        assertTrue(state.isActionVisible)

        viewModel.onIntent(OnboardingIntent.ClickStart)

        assertEquals(listOf("/home"), navigationHelper.rootPages.map { it.toRoute().path })
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        var error: Exception? = null
        val registeredNames = mutableListOf<String>()

        override suspend fun registerCat(name: String): CatVO {
            error?.let { throw it }
            registeredNames += name
            return CatVO(id = 1L, name = name)
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
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }
}
