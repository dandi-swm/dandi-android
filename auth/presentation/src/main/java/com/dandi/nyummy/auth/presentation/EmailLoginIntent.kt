package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface EmailLoginIntent : MviIntent {

    data class InputEmail(val value: String) : EmailLoginIntent

    data class InputPassword(val value: String) : EmailLoginIntent

    data object ClickLogin : EmailLoginIntent

    data object ClickForgotPassword : EmailLoginIntent

    data object ClickSignUp : EmailLoginIntent

}
