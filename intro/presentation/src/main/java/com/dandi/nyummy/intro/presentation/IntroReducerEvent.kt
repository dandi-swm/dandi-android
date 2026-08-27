package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface IntroReducerEvent : ReducerEvent {
    /** 권한 안내 바텀시트 노출. */
    data class PermissionNoticeShown(val permissions: List<AppPermission>) : IntroReducerEvent

    /** 권한 흐름 종료 — 바텀시트 숨김. */
    data object PermissionNoticeFinished : IntroReducerEvent

    /** 시작 게이트 완료 — 스플래시 진행바 100% 채움. */
    data object SplashCompleted : IntroReducerEvent
}
