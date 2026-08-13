package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface SignUpReducerEvent : ReducerEvent {
    data class EmailChanged(val value: String) : SignUpReducerEvent
    data class PasswordChanged(val value: String) : SignUpReducerEvent
    data class PasswordConfirmChanged(val value: String) : SignUpReducerEvent
    data class AccountValidationFailed(
        val emailError: SignUpFieldError?,
        val passwordError: SignUpFieldError?,
        val passwordConfirmError: SignUpFieldError?,
    ) : SignUpReducerEvent

    data class CodeChanged(val value: String) : SignUpReducerEvent
    data object MovedToCode : SignUpReducerEvent
    data object MovedToProfile : SignUpReducerEvent
    data class ResendTicked(val remainingSeconds: Int) : SignUpReducerEvent

    data class NicknameChanged(val value: String) : SignUpReducerEvent
    data class GenderChanged(val value: Gender) : SignUpReducerEvent
    data class BirthChanged(val year: Int, val month: Int, val day: Int) : SignUpReducerEvent
    data class HeightChanged(val value: Int) : SignUpReducerEvent
    data class WeightChanged(val value: Int) : SignUpReducerEvent
    data class ProfileValidationFailed(val nicknameError: SignUpFieldError) : SignUpReducerEvent

    data object LoadingStarted : SignUpReducerEvent
    data object LoadingFinished : SignUpReducerEvent
    data object SteppedBack : SignUpReducerEvent
}
