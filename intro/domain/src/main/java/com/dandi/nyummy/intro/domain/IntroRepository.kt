package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.intro.entity.IntroVO

interface IntroRepository {
    /** 앱 시작 게이트 정보 조회. 서버 API(GET /intro) 준비 전까지는 호출하지 않는다. */
    suspend fun getIntro(): IntroVO
}
