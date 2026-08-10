package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

data class LoginUIState(
    val isLoading: Boolean = false,
) : UiState {
    companion object {
        val empty = LoginUIState()
    }
}