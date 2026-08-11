package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface EmailLoginReducerEvent : ReducerEvent {

    data class EmailChanged(val value: String) : EmailLoginReducerEvent

    data class PasswordChanged(val value: String) : EmailLoginReducerEvent

    data object LoginStarted : EmailLoginReducerEvent

    data object LoginFinished : EmailLoginReducerEvent

    data class ValidationFailed(val emailError: EmailLoginFieldError) : EmailLoginReducerEvent
}
