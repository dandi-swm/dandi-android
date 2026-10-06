package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.auth.domain.SignUpUseCase
import com.dandi.nyummy.auth.domain.SocialSignUpSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** 상단 바 뒤로 가기(ClickBack)가 화면을 닫을지, 회원가입 단계를 되돌릴지 정하는 규칙. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthBackNavigationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val socialSignUpSession = SocialSignUpSession()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `이메일 로그인의 뒤로 가기는 화면을 닫는다`() {
        val viewModel = EmailLoginViewModel(
            navigationHelper = navigationHelper,
            loginUseCase = LoginUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        )

        viewModel.onIntent(EmailLoginIntent.ClickBack)

        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `회원가입 첫 단계의 뒤로 가기는 화면을 닫는다`() {
        val viewModel = createSignUpViewModel(isSocialSignUp = false)

        viewModel.onIntent(SignUpIntent.ClickBack)

        assertEquals(1, navigationHelper.backCount)
        assertEquals(SignUpStep.ACCOUNT, viewModel.uiState.value.step)
    }

    @Test
    fun `인증 코드 단계의 뒤로 가기는 화면을 닫지 않고 계정 단계로 되돌린다`() {
        val viewModel = createSignUpViewModel(isSocialSignUp = false)
        viewModel.onIntent(SignUpIntent.InputEmail("nyummy@cat.com"))
        viewModel.onIntent(SignUpIntent.InputPassword("nyummy1234"))
        viewModel.onIntent(SignUpIntent.InputPasswordConfirm("nyummy1234"))
        viewModel.onIntent(SignUpIntent.ClickSendCode)
        // 코드 발송만 끝내고, 5분 재발송 타이머는 돌리지 않는다.
        testDispatcher.scheduler.runCurrent()
        assertEquals(SignUpStep.CODE, viewModel.uiState.value.step)

        viewModel.onIntent(SignUpIntent.ClickBack)

        assertEquals(0, navigationHelper.backCount)
        assertEquals(SignUpStep.ACCOUNT, viewModel.uiState.value.step)
    }

    @Test
    fun `소셜 가입의 뒤로 가기는 이전 단계가 없으므로 화면을 닫는다`() {
        socialSignUpSession.start("verified-token")
        val viewModel = createSignUpViewModel(isSocialSignUp = true)

        viewModel.onIntent(SignUpIntent.ClickBack)

        assertEquals(1, navigationHelper.backCount)
        assertEquals(SignUpStep.PROFILE, viewModel.uiState.value.step)
    }

    private fun createSignUpViewModel(isSocialSignUp: Boolean) = SignUpViewModel(
        isSocialSignUp = isSocialSignUp,
        emailVerificationUseCase = EmailVerificationUseCase(
            repository,
            FakeResourceHelper,
            messageHelper,
            navigationHelper,
            FakeTTIHelper,
        ),
        signUpUseCase = SignUpUseCase(
            repository,
            socialSignUpSession,
            FakeResourceHelper,
            messageHelper,
            navigationHelper,
            FakeTTIHelper,
        ),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
    )
}
