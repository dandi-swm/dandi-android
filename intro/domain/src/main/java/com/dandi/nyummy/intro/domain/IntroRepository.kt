package com.dandi.nyummy.intro.domain

interface IntroRepository {
    /** 저장된 리프레시 토큰 존재 여부 — 자동로그인 분기 근거. */
    suspend fun hasRefreshToken(): Boolean

    /** 권한 안내(접근권한 고지)를 이미 노출했는지 — 최초 1회 노출 판단 근거. */
    suspend fun hasShownPermissionNotice(): Boolean

    /** 권한 안내 노출 완료를 기록한다. */
    suspend fun markPermissionNoticeShown()
}
