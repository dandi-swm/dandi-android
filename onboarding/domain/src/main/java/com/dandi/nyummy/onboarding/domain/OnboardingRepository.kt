package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.entity.meal.MealTimesVO

interface OnboardingRepository {

    /** 온보딩에서 지어 준 이름과 고른 평소 식사 시각으로 사용자의 고양이를 만든다. */
    suspend fun createCat(name: String, mealTimes: MealTimesVO)

    /** 고양이를 만든 뒤 온보딩 완료 상태를 저장한다. */
    suspend fun markOnboardingComplete()
}
