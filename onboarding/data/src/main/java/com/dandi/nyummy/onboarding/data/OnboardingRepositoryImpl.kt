package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
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
}
