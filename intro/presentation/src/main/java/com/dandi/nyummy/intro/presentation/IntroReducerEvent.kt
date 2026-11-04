package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface IntroReducerEvent : ReducerEvent {
    /** 권한 안내 바텀시트 노출. [permissions]는 확인하면 시스템 팝업으로 물을 권한이다. */
    data class PermissionNoticeShown(val permissions: ImmutableList<AppPermission>) : IntroReducerEvent

    /** 권한 안내를 확인했다 — 시스템 권한 팝업을 띄울 차례. */
    data object PermissionRequestStarted : IntroReducerEvent

    /** 시스템 권한 팝업에 답했다 — 바텀시트 숨김. */
    data object PermissionNoticeFinished : IntroReducerEvent

    /** 시작 게이트 완료 — 스플래시 진행바 100% 채움. */
    data object SplashCompleted : IntroReducerEvent
}
