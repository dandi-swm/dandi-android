package com.dandi.nyummy.home.presentation.prepare

import com.dandi.nyummy.common.presentation.mvi.UiState

data class HomePrepareUIState(
    /** 냐미 미리 받기가 끝났다 — 진행바를 100% 로 채우는 신호. */
    val isComplete: Boolean = false,
) : UiState {
    companion object {
        val empty = HomePrepareUIState()
    }
}
