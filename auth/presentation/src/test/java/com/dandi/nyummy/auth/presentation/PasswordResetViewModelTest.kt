package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.AuthErrorType
import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.auth.domain.PasswordResetPage
import com.dandi.nyummy.auth.domain.PasswordResetUseCase
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 비밀번호 찾기: 이메일 → 인증 코드 → 재설정(임시 비밀번호 메일) → 완료. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository().apply {
        emailChallengeToken = "challenge"
        emailVerifiedToken = "verified"
    }
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PasswordResetViewModel(
        emailVerificationUseCase = EmailVerificationUseCase(
            repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper,
        ),
        passwordResetUseCase = PasswordResetUseCase(
            repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper,
        ),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
    )

    /** 코드 발송만 끝내고, 5분 재발송 타이머는 돌리지 않는다. */
    private fun PasswordResetViewModel.moveToCodeStep() {
        onIntent(PasswordResetIntent.InputEmail("nyummy@cat.com"))
        onIntent(PasswordResetIntent.ClickSendCode)
        testDispatcher.scheduler.runCurrent()
    }

    private fun PasswordResetViewModel.verify(code: String = "123456") {
        onIntent(PasswordResetIntent.InputCode(code))
        onIntent(PasswordResetIntent.ClickVerifyCode)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `이메일 형식이 틀리면 코드를 보내지 않고 입력란에 오류를 둔다`() {
        val viewModel = createViewModel()

        viewModel.onIntent(PasswordResetIntent.InputEmail("nyummy@cat"))
        viewModel.onIntent(PasswordResetIntent.ClickSendCode)
        testDispatcher.scheduler.runCurrent()

        assertEquals(EmailLoginFieldError.EMAIL_FORMAT, viewModel.uiState.value.emailError)
        assertTrue(repository.requestEmailVerificationCalls.isEmpty())
    }

    @Test
    fun `비밀번호 찾기 용도로 코드를 받으면 코드 단계로 간다`() {
        val viewModel = createViewModel()

        viewModel.moveToCodeStep()

        assertEquals(listOf("nyummy@cat.com" to EmailVerificationPurpose.RESET_PASSWORD), repository.requestEmailVerificationCalls)
        assertEquals(PasswordResetStep.CODE, viewModel.uiState.value.step)
        assertEquals("challenge", viewModel.uiState.value.emailChallengeToken)
    }

    @Test
    fun `코드를 확인하면 바로 재설정을 불러 완료 단계로 간다`() {
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()

        viewModel.verify()

        assertEquals(listOf("verified"), repository.resetPasswordCalls)
        assertEquals(PasswordResetStep.DONE, viewModel.uiState.value.step)
        assertEquals("nyummy@cat.com", viewModel.uiState.value.email)
    }

    @Test
    fun `코드가 틀리면 재설정을 부르지 않고 입력란 아래에 오류를 보여 준다`() {
        repository.confirmEmailVerificationError = httpException(400, AuthErrorType.EMAIL_CODE_MISMATCH.type)
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()

        viewModel.verify()

        assertTrue(repository.resetPasswordCalls.isEmpty())
        assertEquals(PasswordResetStep.CODE, viewModel.uiState.value.step)
        assertEquals(AuthErrorType.EMAIL_CODE_MISMATCH.errorMsg, viewModel.uiState.value.codeError)
    }

    @Test
    fun `인증이 만료되면 입력한 이메일은 남기고 이메일 단계로 돌아간다`() {
        repository.resetPasswordError = httpException(401, AuthErrorType.VERIFICATION_EXPIRED.type)
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()

        viewModel.verify()

        val state = viewModel.uiState.value
        assertEquals(PasswordResetStep.EMAIL, state.step)
        assertEquals("nyummy@cat.com", state.email)
        assertEquals("", state.emailChallengeToken)
        assertEquals("", state.emailVerifiedToken)
        assertEquals(AuthErrorType.VERIFICATION_EXPIRED.errorMsg, messageHelper.dialogs.single())
    }

    @Test
    fun `메일 발송만 실패하면 코드를 다시 확인하지 않고 재설정만 다시 부른다`() {
        repository.resetPasswordError = httpException(500, AuthErrorType.EMAIL_SEND_FAILED.type)
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()
        viewModel.verify()
        assertEquals(PasswordResetStep.CODE, viewModel.uiState.value.step)

        repository.resetPasswordError = null
        viewModel.onIntent(PasswordResetIntent.ClickVerifyCode)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("123456"), repository.confirmEmailVerificationCalls)
        assertEquals(listOf("verified", "verified"), repository.resetPasswordCalls)
        assertEquals(PasswordResetStep.DONE, viewModel.uiState.value.step)
    }

    @Test
    fun `코드를 다시 받으면 앞서 확인한 인증은 버린다`() {
        repository.resetPasswordError = httpException(500, AuthErrorType.EMAIL_SEND_FAILED.type)
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()
        viewModel.verify()
        assertEquals("verified", viewModel.uiState.value.emailVerifiedToken)

        viewModel.onIntent(PasswordResetIntent.ClickResendCode)
        messageHelper.lastTwoButtonConfirm?.invoke()
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, repository.requestEmailVerificationCalls.size)
        assertEquals("", viewModel.uiState.value.emailVerifiedToken)
        assertEquals("", viewModel.uiState.value.code)
    }

    @Test
    fun `코드 단계의 뒤로 가기는 이메일 단계로, 이메일 단계의 뒤로 가기는 화면을 닫는다`() {
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()

        viewModel.onIntent(PasswordResetIntent.ClickBack)
        assertEquals(PasswordResetStep.EMAIL, viewModel.uiState.value.step)
        assertEquals(0, navigationHelper.backCount)

        viewModel.onIntent(PasswordResetIntent.ClickBack)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `완료 단계의 로그인하기는 이메일 로그인으로 돌아간다`() {
        val viewModel = createViewModel()
        viewModel.moveToCodeStep()
        viewModel.verify()

        viewModel.onIntent(PasswordResetIntent.ClickLogin)

        assertEquals(1, navigationHelper.backCount)
        assertNull(viewModel.uiState.value.codeError)
    }

    @Test
    fun `이메일 로그인의 비밀번호 찾기는 비밀번호 찾기 화면으로 간다`() {
        val viewModel = EmailLoginViewModel(
            navigationHelper = navigationHelper,
            loginUseCase = LoginUseCase(repository, RecordingAnalyticsHelper(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        )

        viewModel.onIntent(EmailLoginIntent.ClickForgotPassword)

        assertEquals(listOf(PasswordResetPage), navigationHelper.pages)
    }
}
