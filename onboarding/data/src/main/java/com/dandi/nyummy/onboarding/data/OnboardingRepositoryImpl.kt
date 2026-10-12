package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.onboarding.data.dto.CreateCatRequestDTO
import com.dandi.nyummy.onboarding.domain.OnboardingRepository

class OnboardingRepositoryImpl(
    private val dataSource: OnboardingDataSource,
    private val appPreferenceProvider: AppPreferenceProvider,
) : OnboardingRepository {

    override suspend fun createCat(name: String, mealTimes: MealTimesVO) {
        dataSource.createCat(CreateCatRequestDTO.of(name, mealTimes))
    }

    override suspend fun markOnboardingComplete() {
        appPreferenceProvider.setOnboardingIncomplete(incomplete = false)
    }

    // 서버에는 고양이를 만들 때 함께 보낸다. 설정 화면이 아직 기기 값을 읽어서 기기에도 둔다.
    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        appPreferenceProvider.setMealTimes(mealTimes)
    }
}
