package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.auth.domain.SocialLoginUseCase
import com.dandi.nyummy.auth.domain.SocialSignUpPage
import com.dandi.nyummy.auth.domain.SocialSignUpSession
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.auth.presentation.social.SocialLoginResult
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    private val credential = SocialCredentialVO(SocialLoginType.KAKAO, token = "id-token", nonce = "nonce-1")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `카카오를 누르면 화면에 실행을 요청하고 진행 중 재탭은 무시한다`() {
        val viewModel = createViewModel()

        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        viewModel.launchRequested()
        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))

        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        // 재탭이 실행 요청을 다시 만들지 않아야 SDK 가 두 번 뜨지 않는다.
        assertNull(state.socialLoginToLaunch)
        assertEquals(SocialLoginType.KAKAO, state.awaitingSocialLogin?.socialType)
    }

    @Test
    fun `실행을 알리면 요청이 비워져 화면이 재생성돼도 다시 실행되지 않는다`() {
        val viewModel = createViewModel()

        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.socialLoginToLaunch?.socialType)

        viewModel.launchRequested()

        assertNull(viewModel.uiState.value.socialLoginToLaunch)
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.awaitingSocialLogin?.socialType)
    }

    @Test
    fun `취소 결과면 서버 검증 없이 조용히 로딩이 풀린다`() {
        val viewModel = launchedKakao()

        viewModel.onIntent(LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Cancelled))

        assertIdle(viewModel)
        assertTrue(repository.socialLoginCalls.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `자격 증명을 받으면 서버 검증 후 신규 회원은 소셜 가입 화면으로 이동하고 로딩이 풀린다`() = runTest(testDispatcher) {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")
        val viewModel = launchedKakao()

        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Success(credential)),
        )
        // 로그인 창이 닫힌 뒤 서버 응답 전까지 검증 중 로딩이 보여야 한다.
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.verifyingSocialLogin?.socialType)
        advanceUntilIdle()

        assertEquals(listOf(credential), repository.socialLoginCalls)
        assertEquals(listOf<Page>(SocialSignUpPage), navigationHelper.pages)
        assertIdle(viewModel)
    }

    @Test
    fun `로그인 창을 띄운 뒤 서버 검증이 끝날 때까지 내내 로딩 대상이다`() = runTest(testDispatcher) {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")
        val viewModel = createViewModel()

        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.socialLoginInProgress)
        viewModel.launchRequested()
        // 로그인 창에서 돌아와 카카오 토큰을 받는 동안에도 로딩이 보여야 한다.
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.socialLoginInProgress)
        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Success(credential)),
        )
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.socialLoginInProgress)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.socialLoginInProgress)
    }

    @Test
    fun `카카오 응답을 기다리는 중 뒤로가기면 기다림을 그만두고 늦게 온 결과는 무시한다`() = runTest(testDispatcher) {
        val viewModel = launchedKakao()
        val attempt = viewModel.awaiting()

        viewModel.onIntent(LoginIntent.SocialLoginBackPressed)
        assertIdle(viewModel)

        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(attempt, SocialLoginResult.Success(credential)),
        )
        advanceUntilIdle()
        assertTrue(repository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `서버 검증 중 뒤로가기는 무시하고 검증을 마친다`() = runTest(testDispatcher) {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")
        val viewModel = launchedKakao()
        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Success(credential)),
        )

        viewModel.onIntent(LoginIntent.SocialLoginBackPressed)
        assertEquals(SocialLoginType.KAKAO, viewModel.uiState.value.verifyingSocialLogin?.socialType)
        advanceUntilIdle()

        assertEquals(listOf(credential), repository.socialLoginCalls)
        assertEquals(listOf<Page>(SocialSignUpPage), navigationHelper.pages)
    }

    @Test
    fun `기다리지 않던 결과는 무시한다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        // 실행 요청 전에 도착한 결과
        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Success(credential)),
        )
        // 이미 처리된 시도의 늦은 결과
        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        viewModel.launchRequested()
        val attempt = viewModel.awaiting()
        viewModel.onIntent(LoginIntent.SocialLoginResultReceived(attempt, SocialLoginResult.Cancelled))
        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(attempt, SocialLoginResult.Success(credential)),
        )
        advanceUntilIdle()

        assertTrue(repository.socialLoginCalls.isEmpty())
        assertIdle(viewModel)
    }

    @Test
    fun `취소 후 같은 제공자로 다시 시도하면 지난 시도의 늦은 결과는 무시한다`() = runTest(testDispatcher) {
        val viewModel = launchedKakao()
        val firstAttempt = viewModel.awaiting()
        viewModel.onIntent(LoginIntent.SocialLoginBackPressed)

        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        viewModel.launchRequested()
        val secondAttempt = viewModel.awaiting()
        viewModel.onIntent(
            LoginIntent.SocialLoginResultReceived(firstAttempt, SocialLoginResult.Success(credential)),
        )
        advanceUntilIdle()

        assertTrue(firstAttempt != secondAttempt)
        assertTrue(repository.socialLoginCalls.isEmpty())
        assertEquals(secondAttempt, viewModel.uiState.value.awaitingSocialLogin)
    }

    @Test
    fun `SDK 실패면 실패를 안내하고 로딩이 풀린다`() {
        val viewModel = launchedKakao()

        viewModel.onIntent(LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Failed))

        assertIdle(viewModel)
        assertEquals("카카오 로그인에 실패했어요. 다시 시도해주세요.", messageHelper.dialogs.single())
    }

    @Test
    fun `연동되지 않은 제공자는 준비 중 안내 후 로딩이 풀린다`() {
        val viewModel = createViewModel()

        viewModel.onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.GOOGLE))
        viewModel.launchRequested()
        viewModel.onIntent(LoginIntent.SocialLoginResultReceived(viewModel.awaiting(), SocialLoginResult.Unavailable))

        assertIdle(viewModel)
        assertEquals(listOf(IconType.WARNING to "구글 로그인은 준비 중이에요."), messageHelper.snackBars)
    }

    private fun launchedKakao(): LoginViewModel = createViewModel().apply {
        onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
        launchRequested()
    }

    /** 화면이 하듯 실행 요청된 시도를 그대로 실행했다고 알린다. */
    private fun LoginViewModel.launchRequested() {
        onIntent(LoginIntent.SocialLoginLaunched(checkNotNull(uiState.value.socialLoginToLaunch)))
    }

    /** 지금 결과를 기다리는 시도. 없으면(실행 요청 전 등) 발급된 적 없는 시도를 돌려준다. */
    private fun LoginViewModel.awaiting(): SocialLoginAttempt =
        uiState.value.awaitingSocialLogin ?: SocialLoginAttempt(id = -1, socialType = SocialLoginType.KAKAO)

    private fun assertIdle(viewModel: LoginViewModel) {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.socialLoginToLaunch)
        assertNull(state.awaitingSocialLogin)
        assertNull(state.verifyingSocialLogin)
    }

    private fun createViewModel(): LoginViewModel = LoginViewModel(
        navigationHelper = navigationHelper,
        loginUseCase = LoginUseCase(
            repository = repository,
            resourceHelper = FakeResourceHelper,
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper,
        ),
        socialLoginUseCase = SocialLoginUseCase(
            repository = repository,
            socialSignUpSession = SocialSignUpSession(),
            resourceHelper = FakeResourceHelper,
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper,
        ),
    )

    private class FakeAuthRepository : AuthRepository {
        var socialLoginResult: SocialLoginVO = SocialLoginVO.empty
        val socialLoginCalls = mutableListOf<SocialCredentialVO>()

        override suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO {
            socialLoginCalls += credential
            return socialLoginResult
        }

        override suspend fun login(email: String, password: String) = AuthTokenVO.empty
        override suspend fun signUp(
            verifiedToken: String,
            password: String?,
            confirmPassword: String?,
            nickname: String,
            gender: Gender?,
            birth: String?,
            height: Int?,
            weight: Int?,
        ) = Unit

        override suspend fun requestEmailVerification(email: String, purpose: EmailVerificationPurpose) =
            EmailChallengeVO()

        override suspend fun confirmEmailVerification(authCode: String, emailChallengeToken: String) =
            EmailVerifiedVO()
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

    private class RecordingMessageHelper : MessageHelper {
        val dialogs = mutableListOf<String>()
        val snackBars = mutableListOf<Pair<IconType, String>>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += iconType to messageText
        }

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
        ) {
            dialogs += descText
        }

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
