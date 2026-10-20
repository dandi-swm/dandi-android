package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface PasswordResetReducerEvent : ReducerEvent {
    data class EmailChanged(val value: String) : PasswordResetReducerEvent
    data class EmailValidationFailed(val emailError: EmailLoginFieldError) : PasswordResetReducerEvent
    data class MovedToCode(val emailChallengeToken: String) : PasswordResetReducerEvent
    data class ChallengeTokenRefreshed(val emailChallengeToken: String) : PasswordResetReducerEvent

    data class CodeChanged(val value: String) : PasswordResetReducerEvent
    data class CodeVerificationFailed(val message: String) : PasswordResetReducerEvent
    data class CodeVerified(val emailVerifiedToken: String) : PasswordResetReducerEvent
    data class ResendTicked(val remainingSeconds: Int) : PasswordResetReducerEvent

    /** 임시 비밀번호 메일을 보냈다. 완료 단계로 간다. */
    data object ResetCompleted : PasswordResetReducerEvent

    /** 인증이 만료됐거나 무효해 이메일 단계로 돌아간다. 입력한 이메일은 남긴다. */
    data object Restarted : PasswordResetReducerEvent

    data object LoadingStarted : PasswordResetReducerEvent
    data object LoadingFinished : PasswordResetReducerEvent
    data object SteppedBack : PasswordResetReducerEvent
}
