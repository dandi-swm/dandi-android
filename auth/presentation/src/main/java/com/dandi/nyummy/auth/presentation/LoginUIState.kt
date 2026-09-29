package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

data class LoginUIState(
    val isLoading: Boolean = false,
    // debug 빌드에 테스트 계정이 주입됐을 때만 true — 테스트 계정 로그인 버튼 노출 여부.
    val isTestLoginAvailable: Boolean = false,
) : UiState {
    companion object {
        val empty = LoginUIState()
    }
}