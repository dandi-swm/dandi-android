package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.mvi.UiState

data class IntroUIState(
    val showPermissionNotice: Boolean = false,
    val pendingPermissions: List<AppPermission> = emptyList(),
    /** 시작 게이트 완료 — 스플래시 진행바를 100% 로 채우는 신호. */
    val isSplashComplete: Boolean = false,
) : UiState {
    companion object {
        val empty = IntroUIState()
    }
}
