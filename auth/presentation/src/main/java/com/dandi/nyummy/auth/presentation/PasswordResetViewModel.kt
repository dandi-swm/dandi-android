package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.CodeVerificationFailedException
import com.dandi.nyummy.auth.domain.EmailLoginValidator
import com.dandi.nyummy.auth.domain.EmailVerificationRestartRequiredException
import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.PasswordResetUseCase
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 비밀번호 찾기. 가입한 이메일로 인증 코드를 받아 확인하면, 바로 비밀번호 재설정을 불러
 * 서버가 임시 비밀번호를 메일로 보내게 한다. 앞 화면에서 넘겨받는 값이 없어 어디서 들어와도 같게 동작한다.
 */
@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val emailVerificationUseCase: EmailVerificationUseCase,
    private val passwordResetUseCase: PasswordResetUseCase,
    private val messageHelper: MessageHelper,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<PasswordResetIntent, PasswordResetUIState, PasswordResetReducerEvent>(PasswordResetUIState.empty) {

    private var resendTimerJob: Job? = null

    override fun onIntent(intent: PasswordResetIntent) {
        when (intent) {
            is PasswordResetIntent.InputEmail -> dispatch(PasswordResetReducerEvent.EmailChanged(intent.value))
            PasswordResetIntent.ClickSendCode -> sendCode()
            is PasswordResetIntent.InputCode -> dispatch(PasswordResetReducerEvent.CodeChanged(intent.value))
            PasswordResetIntent.ClickVerifyCode -> verifyCode()
            PasswordResetIntent.ClickResendCode -> resendCode()
            PasswordResetIntent.ClickLogin -> navigationHelper.navigateToBack()
            PasswordResetIntent.ClickBack -> goBack()
        }
    }

    override fun reduce(state: PasswordResetUIState, event: PasswordResetReducerEvent): PasswordResetUIState =
        when (event) {
            is PasswordResetReducerEvent.EmailChanged -> state.copy(email = event.value, emailError = null)
            is PasswordResetReducerEvent.EmailValidationFailed -> state.copy(emailError = event.emailError)
            is PasswordResetReducerEvent.MovedToCode -> state.copy(
                step = PasswordResetStep.CODE,
                emailChallengeToken = event.emailChallengeToken,
                emailVerifiedToken = "",
                code = "",
                codeError = null,
            )
            // 새 코드를 받으면 앞서 확인한 인증은 버리고 새 코드로 다시 확인한다.
            is PasswordResetReducerEvent.ChallengeTokenRefreshed -> state.copy(
                emailChallengeToken = event.emailChallengeToken,
                emailVerifiedToken = "",
                code = "",
                codeError = null,
            )
            is PasswordResetReducerEvent.CodeChanged -> state.copy(code = event.value, codeError = null)
            is PasswordResetReducerEvent.CodeVerificationFailed -> state.copy(codeError = event.message)
            is PasswordResetReducerEvent.CodeVerified -> state.copy(emailVerifiedToken = event.emailVerifiedToken)
            is PasswordResetReducerEvent.ResendTicked -> state.copy(resendRemainingSeconds = event.remainingSeconds)
            PasswordResetReducerEvent.ResetCompleted -> state.copy(
                step = PasswordResetStep.DONE,
                emailChallengeToken = "",
                emailVerifiedToken = "",
                resendRemainingSeconds = 0,
            )
            PasswordResetReducerEvent.Restarted -> state.copy(
                step = PasswordResetStep.EMAIL,
                emailChallengeToken = "",
                emailVerifiedToken = "",
                code = "",
                codeError = null,
                resendRemainingSeconds = 0,
            )
            PasswordResetReducerEvent.LoadingStarted -> state.copy(isLoading = true)
            PasswordResetReducerEvent.LoadingFinished -> state.copy(isLoading = false)
            PasswordResetReducerEvent.SteppedBack -> when (state.step) {
                PasswordResetStep.CODE -> state.copy(
                    step = PasswordResetStep.EMAIL,
                    emailChallengeToken = "",
                    emailVerifiedToken = "",
                    code = "",
                    codeError = null,
                )
                else -> state
            }
        }

    /** 코드 단계는 이메일 단계로 되돌리고, 이메일 단계와 완료 단계는 화면을 닫는다. */
    private fun goBack() {
        if (currentState.step == PasswordResetStep.CODE) {
            dispatch(PasswordResetReducerEvent.SteppedBack)
        } else {
            navigationHelper.navigateToBack()
        }
    }

    private fun sendCode() {
        val email = currentState.email.trim()
        EmailLoginValidator.validateEmail(email)?.let { error ->
            dispatch(PasswordResetReducerEvent.EmailValidationFailed(error))
            return
        }
        dispatch(PasswordResetReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.sendCode(email, EmailVerificationPurpose.RESET_PASSWORD)
                .onSuccess { challengeToken ->
                    dispatch(PasswordResetReducerEvent.MovedToCode(challengeToken))
                    startResendTimer()
                }
            dispatch(PasswordResetReducerEvent.LoadingFinished)
        }
    }

    /**
     * 코드를 확인하고 이어서 재설정을 부른다. 재설정만 실패해 인증 완료 토큰이 남아 있으면
     * 같은 코드를 다시 확인하지 않고 재설정만 다시 부른다.
     */
    private fun verifyCode() {
        dispatch(PasswordResetReducerEvent.LoadingStarted)
        viewModelScope.launch {
            val verifiedToken = currentState.emailVerifiedToken.ifEmpty {
                emailVerificationUseCase.confirmCode(
                    authCode = currentState.code,
                    emailChallengeToken = currentState.emailChallengeToken,
                )
                    .onSuccess { dispatch(PasswordResetReducerEvent.CodeVerified(it)) }
                    .onFailure { e ->
                        if (e is CodeVerificationFailedException) {
                            dispatch(PasswordResetReducerEvent.CodeVerificationFailed(e.message))
                        }
                    }
                    .getOrNull()
            }
            if (verifiedToken != null) resetPassword(verifiedToken)
            dispatch(PasswordResetReducerEvent.LoadingFinished)
        }
    }

    private suspend fun resetPassword(verifiedToken: String) {
        passwordResetUseCase.resetPassword(verifiedToken)
            .onSuccess {
                resendTimerJob?.cancel()
                dispatch(PasswordResetReducerEvent.ResetCompleted)
            }
            .onFailure { e ->
                if (e is EmailVerificationRestartRequiredException) {
                    resendTimerJob?.cancel()
                    dispatch(PasswordResetReducerEvent.Restarted)
                }
            }
    }

    private fun resendCode() {
        if (currentState.isLoading) return
        messageHelper.showTwoButtonDialog(
            descText = RESEND_CONFIRM_MESSAGE,
            leftButtonText = RESEND_CONFIRM_CANCEL,
            rightButtonText = RESEND_CONFIRM_OK,
            onClickRightButton = ::executeResend,
        )
    }

    private fun executeResend() {
        dispatch(PasswordResetReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.sendCode(currentState.email.trim(), EmailVerificationPurpose.RESET_PASSWORD)
                .onSuccess { challengeToken ->
                    dispatch(PasswordResetReducerEvent.ChallengeTokenRefreshed(challengeToken))
                    startResendTimer()
                }
            dispatch(PasswordResetReducerEvent.LoadingFinished)
        }
    }

    /** 발송 직후 [RESEND_COOLDOWN_SECONDS]부터 1초 간격으로 감소 — 0이 되면 다시 보낼 수 있다. */
    private fun startResendTimer() {
        resendTimerJob?.cancel()
        resendTimerJob = viewModelScope.launch {
            var remaining = RESEND_COOLDOWN_SECONDS
            dispatch(PasswordResetReducerEvent.ResendTicked(remaining))
            while (remaining > 0) {
                delay(TIMER_TICK_MILLIS)
                remaining--
                dispatch(PasswordResetReducerEvent.ResendTicked(remaining))
            }
        }
    }

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 5 * 60
        private const val TIMER_TICK_MILLIS = 1_000L
        private const val RESEND_CONFIRM_MESSAGE = "인증 코드를 다시 보낼까요?"
        private const val RESEND_CONFIRM_CANCEL = "취소"
        private const val RESEND_CONFIRM_OK = "보내기"
    }
}
