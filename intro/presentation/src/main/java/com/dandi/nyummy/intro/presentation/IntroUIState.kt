package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.mvi.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class IntroUIState(
    val showPermissionNotice: Boolean = false,
    /** 권한 안내를 확인하면 시스템 팝업으로 물을 권한. */
    val requestedPermissions: ImmutableList<AppPermission> = persistentListOf(),
    /** 권한 안내를 확인해 시스템 팝업을 띄울 차례다. 답할 때까지 안내는 시스템 팝업 뒤에 그대로 둔다. */
    val isRequestingPermissions: Boolean = false,
    /** 시작 게이트 완료 — 스플래시 진행바를 100% 로 채우는 신호. */
    val isSplashComplete: Boolean = false,
) : UiState {
    companion object {
        val empty = IntroUIState()
    }
}
