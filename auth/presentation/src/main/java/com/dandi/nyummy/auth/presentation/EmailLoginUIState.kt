package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

data class EmailLoginUIState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) : UiState {

    val isLoginEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading

    companion object {
        val empty = EmailLoginUIState()
    }
}
