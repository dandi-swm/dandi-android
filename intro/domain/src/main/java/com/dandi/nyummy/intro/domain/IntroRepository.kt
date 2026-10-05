package com.dandi.nyummy.intro.domain

interface IntroRepository {
    /** 저장된 리프레시 토큰 존재 여부 — 자동로그인 분기 근거. */
    suspend fun hasRefreshToken(): Boolean

    /** 온보딩 미완료 여부 — 앱 재시작 시 온보딩 복원 근거. */
    suspend fun isOnboardingIncomplete(): Boolean

    /** 권한 안내(접근권한 고지)를 이미 노출했는지 — 최초 1회 노출 판단 근거. */
    suspend fun hasShownPermissionNotice(): Boolean

    /** 권한 안내 노출 완료를 기록한다. */
    suspend fun markPermissionNoticeShown()
}
