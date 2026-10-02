package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.CodeVerificationFailedException
import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.SignUpUseCase
import com.dandi.nyummy.auth.domain.SignUpValidator
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 회원가입 퍼널. [isSocialSignUp] 이면 소셜 로그인 신규 회원의 가입으로, 프로필 단계만 진행하고
 * 가입 토큰은 SignUpUseCase 가 보관 중인 소셜 가입 세션에서 꺼내 쓴다.
 */
@HiltViewModel(assistedFactory = SignUpViewModel.Factory::class)
class SignUpViewModel @AssistedInject constructor(
    @Assisted isSocialSignUp: Boolean,
    private val emailVerificationUseCase: EmailVerificationUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val messageHelper: MessageHelper,
) : MviViewModel<SignUpIntent, SignUpUIState, SignUpReducerEvent>(SignUpUIState.initial(isSocialSignUp)) {

    @AssistedFactory
    interface Factory {
        fun create(isSocialSignUp: Boolean): SignUpViewModel
    }

    private var resendTimerJob: Job? = null

    init {
        // 프로세스 종료 후 복원됐거나 외부 링크로 들어오면 가입에 쓸 토큰이 없다 — 다시 로그인하도록 돌려보낸다.
        if (isSocialSignUp && !signUpUseCase.hasPendingSocialSignUp()) {
            signUpUseCase.leaveSocialSignUpWithoutSession()
        }
    }

    override fun onCleared() {
        // 가입을 마치지 않고 화면을 떠나면(뒤로가기 등) 남은 가입 토큰을 버린다. 회전으로는 호출되지 않는다.
        if (currentState.isSocialSignUp) signUpUseCase.abandonSocialSignUp()
        super.onCleared()
    }

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
            is SignUpReducerEvent.MovedToCode -> state.copy(
                step = SignUpStep.CODE,
                emailChallengeToken = event.emailChallengeToken,
                code = "",
                codeError = null,
            )
            is SignUpReducerEvent.ChallengeTokenRefreshed -> state.copy(
                emailChallengeToken = event.emailChallengeToken,
                code = "",
                codeError = null,
            )
            is SignUpReducerEvent.MovedToProfile ->
                state.copy(step = SignUpStep.PROFILE, emailVerifiedToken = event.emailVerifiedToken)
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
            SignUpReducerEvent.SteppedBack -> when {
                // 소셜 가입은 이전 단계가 없다 — 뒤로가기는 라우트 자체를 닫는다.
                state.isSocialSignUp -> state
                else -> state.stepBack()
            }
        }

    /** 이메일 가입의 한 단계 뒤로가기. ACCOUNT 에서는 그대로 두고 라우트가 닫힌다. */
    private fun SignUpUIState.stepBack(): SignUpUIState = when (step) {
        SignUpStep.ACCOUNT -> this
        SignUpStep.CODE -> copy(step = SignUpStep.ACCOUNT, emailChallengeToken = "")
        SignUpStep.PROFILE -> copy(
            step = SignUpStep.CODE,
            emailVerifiedToken = "",
            code = "",
            codeError = null,
        )
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
            emailVerificationUseCase.sendCode(currentState.email, EmailVerificationPurpose.SIGNUP)
                .onSuccess { challengeToken ->
                    dispatch(SignUpReducerEvent.MovedToCode(challengeToken))
                    startResendTimer()
                }
            dispatch(SignUpReducerEvent.LoadingFinished)
        }
    }

    private fun verifyCode() {
        dispatch(SignUpReducerEvent.LoadingStarted)
        viewModelScope.launch {
            emailVerificationUseCase.confirmCode(
                authCode = currentState.code,
                emailChallengeToken = currentState.emailChallengeToken,
            )
                .onSuccess { verifiedToken ->
                    resendTimerJob?.cancel()
                    dispatch(SignUpReducerEvent.ResendTicked(0))
                    dispatch(SignUpReducerEvent.MovedToProfile(verifiedToken))
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
            emailVerificationUseCase.sendCode(currentState.email, EmailVerificationPurpose.SIGNUP)
                .onSuccess { challengeToken ->
                    dispatch(SignUpReducerEvent.ChallengeTokenRefreshed(challengeToken))
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
            val state = currentState
            val birth = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                state.birthYear,
                state.birthMonth,
                state.birthDay,
            )
            if (state.isSocialSignUp) {
                signUpUseCase.signUpWithSocial(
                    nickname = state.nickname.trim(),
                    gender = state.gender,
                    birth = birth,
                    height = state.height,
                    weight = state.weight,
                )
            } else {
                signUpUseCase.signUp(
                    emailVerifiedToken = state.emailVerifiedToken,
                    password = state.password,
                    confirmPassword = state.passwordConfirm,
                    nickname = state.nickname.trim(),
                    gender = state.gender,
                    birth = birth,
                    height = state.height,
                    weight = state.weight,
                )
            }
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
