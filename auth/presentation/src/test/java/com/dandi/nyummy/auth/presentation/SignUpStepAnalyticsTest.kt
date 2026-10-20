package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
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

/** 회원가입 단계에 도착할 때마다 sign_up_step을 남겨 단계별 이탈을 잴 수 있는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class SignUpStepAnalyticsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val socialSignUpSession = SocialSignUpSession()
    private val analytics = RecordingAnalyticsHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `이메일 가입은 계정 단계에서 시작하고 코드를 보내면 인증 코드 단계를 남긴다`() {
        val viewModel = createViewModel(isSocialSignUp = false)
        testDispatcher.scheduler.runCurrent()

        viewModel.onIntent(SignUpIntent.InputEmail("nyummy@cat.com"))
        viewModel.onIntent(SignUpIntent.InputPassword("nyummy1234"))
        viewModel.onIntent(SignUpIntent.InputPasswordConfirm("nyummy1234"))
        viewModel.onIntent(SignUpIntent.ClickSendCode)
        // 코드 발송만 끝내고, 5분 재발송 타이머는 돌리지 않는다.
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("account" to "email", "code" to "email"), recordedSteps())
    }

    @Test
    fun `인증 코드 단계에서 되돌아가면 계정 단계를 다시 남긴다`() {
        val viewModel = createViewModel(isSocialSignUp = false)
        viewModel.onIntent(SignUpIntent.InputEmail("nyummy@cat.com"))
        viewModel.onIntent(SignUpIntent.InputPassword("nyummy1234"))
        viewModel.onIntent(SignUpIntent.InputPasswordConfirm("nyummy1234"))
        viewModel.onIntent(SignUpIntent.ClickSendCode)
        testDispatcher.scheduler.runCurrent()

        viewModel.onIntent(SignUpIntent.ClickBack)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("account", "code", "account"), recordedSteps().map { it.first })
    }

    @Test
    fun `소셜 가입은 프로필 단계부터 social로 남긴다`() {
        socialSignUpSession.start("verified-token")

        createViewModel(isSocialSignUp = true)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("profile" to "social"), recordedSteps())
    }

    private fun recordedSteps(): List<Pair<String, String>> = analytics.events
        .filter { it.name == "sign_up_step" }
        .map { it.params.getValue("step") to it.params.getValue("method") }

    private fun createViewModel(isSocialSignUp: Boolean) = SignUpViewModel(
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
            RecordingAnalyticsHelper(),
            FakeResourceHelper,
            messageHelper,
            navigationHelper,
            FakeTTIHelper,
        ),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        analyticsHelper = analytics,
    )
}
