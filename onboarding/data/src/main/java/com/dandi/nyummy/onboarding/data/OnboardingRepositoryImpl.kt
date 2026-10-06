package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.onboarding.domain.OnboardingRepository

class OnboardingRepositoryImpl(
    private val dataSource: OnboardingDataSource,
    private val appPreferenceProvider: AppPreferenceProvider,
) : OnboardingRepository {

    override suspend fun registerCat(name: String) =
        dataSource.registerCat(name).toVO()

    override suspend fun markOnboardingComplete() {
        appPreferenceProvider.setOnboardingIncomplete(incomplete = false)
    }

    // 식사 시각 API가 아직 없어 기기에만 저장한다. 서버가 생기면 여기서 함께 보낸다.
    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        appPreferenceProvider.setMealTimes(mealTimes)
    }
}
