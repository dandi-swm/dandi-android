package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.intro.entity.IntroVO

interface IntroRepository {
    /** 앱 시작 게이트 정보 조회. 서버 API(GET /intro) 준비 전까지는 호출하지 않는다. */
    suspend fun getIntro(): IntroVO

    /** 저장된 리프레시 토큰 존재 여부 — 자동로그인 분기 근거. */
    suspend fun hasRefreshToken(): Boolean
}
