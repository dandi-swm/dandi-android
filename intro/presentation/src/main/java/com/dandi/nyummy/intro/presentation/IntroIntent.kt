package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface IntroIntent : MviIntent {
    /**
     * 시스템 권한 요청이 끝났음(허용/거부 무관 — 시작 게이트는 비차단).
     * 요청할 권한이 없어 launcher 를 건너뛴 경우에도 발행된다.
     */
    data object OnPermissionsResult : IntroIntent
}
