package com.dandi.nyummy.intro.domain

interface IntroRepository {
    /** 저장된 리프레시 토큰 존재 여부 — 자동로그인 분기 근거. */
    suspend fun hasRefreshToken(): Boolean
}
