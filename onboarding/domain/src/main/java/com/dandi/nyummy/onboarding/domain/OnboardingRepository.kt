package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.onboarding.entity.CatVO

interface OnboardingRepository {

    /** 온보딩에서 지어 준 이름으로 사용자의 고양이를 등록한다. */
    suspend fun registerCat(name: String): CatVO

    /** 고양이 등록을 마친 뒤 온보딩 완료 상태를 저장한다. */
    suspend fun markOnboardingComplete()
}
