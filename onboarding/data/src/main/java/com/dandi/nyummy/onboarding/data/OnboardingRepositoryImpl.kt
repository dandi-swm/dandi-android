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
}
