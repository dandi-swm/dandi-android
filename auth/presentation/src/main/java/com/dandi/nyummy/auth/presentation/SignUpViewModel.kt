package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.CodeVerificationFailedException
import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.SignUpUseCase
import com.dandi.nyummy.auth.domain.SignUpValidator
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val emailVerificationUseCase: EmailVerificationUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val messageHelper: MessageHelper,
) : MviViewModel<SignUpIntent, SignUpUIState, SignUpReducerEvent>(SignUpUIState.empty) {

    private var resendTimerJob: Job? = null

    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.InputEmail -> dispatch(SignUpReducerEvent.EmailChanged(intent.value))
            is SignUpIntent.InputPassword -> dispatch(SignUpReducerEvent.PasswordChanged(intent.value))
            is SignUpIntent.InputPasswordConfirm ->
                dispatch(SignUpReducerEvent.PasswordConfirmChanged(intent.value))
            SignUpIntent.ClickSendCode -> sendCode()
            is SignUpIntent.InputCode -> dispatch(SignUpReducerEvent.CodeChanged(intent.value))
            SignUpIntent.ClickVerifyCode -> verifyCode()
            SignUpIntent.ClickResendCode -> resendCode()
            is SignUpIntent.InputNickname -> dispatch(SignUpReducerEvent.NicknameChanged(intent.value))
            is SignUpIntent.SelectGender -> dispatch(SignUpReducerEvent.GenderChanged(intent.value))
            is SignUpIntent.SelectBirthYear -> changeBirth(year = intent.value)
            is SignUpIntent.SelectBirthMonth -> changeBirth(month = intent.value)
            is SignUpIntent.SelectBirthDay -> changeBirth(day = intent.value)
            is SignUpIntent.SelectHeight -> dispatch(SignUpReducerEvent.HeightChanged(intent.value))
            is SignUpIntent.SelectWeight -> dispatch(SignUpReducerEvent.WeightChanged(intent.value))
            SignUpIntent.ClickSubmit -> submit()
            SignUpIntent.ClickBackStep -> dispatch(SignUpReducerEvent.SteppedBack)
        }
    }

    override fun reduce(state: SignUpUIState, event: SignUpReducerEvent): SignUpUIState =
        when (event) {
            is SignUpReducerEvent.EmailChanged -> state.copy(email = event.value, emailError = null)
            is SignUpReducerEvent.PasswordChanged ->
                state.copy(password = event.value, passwordError = null, passwordConfirmError = null)
            is SignUpReducerEvent.PasswordConfirmChanged ->
                state.copy(passwordConfirm = event.value, passwordConfirmError = null)
            is SignUpReducerEvent.AccountValidationFailed -> state.copy(
                emailError = event.emailError,
                passwordError = event.passwordError,
                passwordConfirmError = event.passwordConfirmError,
            )
            is SignUpReducerEvent.CodeChanged -> state.copy(code = event.value, codeError = null)
            is SignUpReducerEvent.CodeVerificationFailed -> state.copy(codeError = event.message)
            SignUpReducerEvent.MovedToCode ->
                state.copy(step = SignUpStep.CODE, code = "", codeError = null)
            SignUpReducerEvent.MovedToProfile -> state.copy(step = SignUpStep.PROFILE)
            is SignUpReducerEvent.ResendTicked ->
                state.copy(resendRemainingSeconds = event.remainingSeconds)
            is SignUpReducerEvent.NicknameChanged ->
                state.copy(nickname = event.value, nicknameError = null)
            is SignUpReducerEvent.GenderChanged -> state.copy(gender = event.value)
            is SignUpReducerEvent.BirthChanged ->
                state.copy(birthYear = event.year, birthMonth = event.month, birthDay = event.day)
            is SignUpReducerEvent.HeightChanged -> state.copy(height = event.value)
            is SignUpReducerEvent.WeightChanged -> state.copy(weight = event.value)
            is SignUpReducerEvent.ProfileValidationFailed ->
                state.copy(nicknameError = event.nicknameError)
            SignUpReducerEvent.LoadingStarted -> state.copy(isLoading = true)
            SignUpReducerEvent.LoadingFinished -> state.copy(isLoading = false)
            SignUpReducerEvent.SteppedBack -> when (state.step) {
                SignUpStep.ACCOUNT -> state
                SignUpStep.CODE -> state.copy(step = SignUpStep.ACCOUNT)
                SignUpStep.PROFILE -> state.copy(step = SignUpStep.CODE, code = "", codeError = null)
            }
        }

    private fun sendCode() {
        val emailError = SignUpValidator.validateEmail(currentState.email)
        val passwordError = SignUpValidator.validatePassword(currentState.password)
        val passwordConfirmError =
            SignUpValidator.validatePasswordConfirm(currentState.password, currentState.passwordConfirm)
        if (emailError != null || passwordError != null || passwordConfirmError != null) {
            dispatch(
                SignUpReducerEvent.AccountValidationFailed(emailError, passwordError, passwordConfirmError),
            )
            return
        }

        dispatch(SignUpReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.sendCode(currentState.email)
                .onSuccess {
                    dispatch(SignUpReducerEvent.MovedToCode)
                    startResendTimer()
                }
            dispatch(SignUpReducerEvent.LoadingFinished)
        }
    }

    private fun verifyCode() {
        dispatch(SignUpReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.confirmCode(currentState.email, currentState.code)
                .onSuccess {
                    resendTimerJob?.cancel()
                    dispatch(SignUpReducerEvent.ResendTicked(0))
                    dispatch(SignUpReducerEvent.MovedToProfile)
                }
                .onFailure { e ->
                    if (e is CodeVerificationFailedException) {
                        dispatch(SignUpReducerEvent.CodeVerificationFailed(e.message))
                    }
                }
            dispatch(SignUpReducerEvent.LoadingFinished)
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
        dispatch(SignUpReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.sendCode(currentState.email)
                .onSuccess {
                    dispatch(SignUpReducerEvent.CodeChanged(""))
                    startResendTimer()
                }
            dispatch(SignUpReducerEvent.LoadingFinished)
        }
    }

    /** 발송 직후 [RESEND_COOLDOWN_SECONDS]부터 1초 간격으로 감소 — 0이 되면 재발송 가능. */
    private fun startResendTimer() {
        resendTimerJob?.cancel()
        resendTimerJob = viewModelScope.launch {
            var remaining = RESEND_COOLDOWN_SECONDS
            dispatch(SignUpReducerEvent.ResendTicked(remaining))
            while (remaining > 0) {
                delay(TIMER_TICK_MILLIS)
                remaining--
                dispatch(SignUpReducerEvent.ResendTicked(remaining))
            }
        }
    }

    private fun changeBirth(
        year: Int = currentState.birthYear,
        month: Int = currentState.birthMonth,
        day: Int = currentState.birthDay,
    ) {
        val clampedDay = day.coerceAtMost(lengthOfMonth(year, month))
        dispatch(SignUpReducerEvent.BirthChanged(year = year, month = month, day = clampedDay))
    }

    private fun submit() {
        val nicknameError = SignUpValidator.validateNickname(currentState.nickname)
        if (nicknameError != null) {
            dispatch(SignUpReducerEvent.ProfileValidationFailed(nicknameError))
            return
        }

        dispatch(SignUpReducerEvent.LoadingStarted)
        viewModelScope.launch {
            signUpUseCase.signUp(
                email = currentState.email,
                password = currentState.password,
                nickname = currentState.nickname.trim(),
                gender = currentState.gender,
                birth = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    currentState.birthYear,
                    currentState.birthMonth,
                    currentState.birthDay,
                ),
                height = currentState.height,
                weight = currentState.weight,
            )
            dispatch(SignUpReducerEvent.LoadingFinished)
        }
    }

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 5 * 60
        private const val TIMER_TICK_MILLIS = 1_000L
        private const val RESEND_CONFIRM_MESSAGE = "인증 코드를 다시 전송하시겠습니까?"
        private const val RESEND_CONFIRM_CANCEL = "취소"
        private const val RESEND_CONFIRM_OK = "전송"
    }
}
