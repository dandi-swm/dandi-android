package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.onboarding.domain.OnboardingRepository

class OnboardingRepositoryImpl(
    private val dataSource: OnboardingDataSource,
) : OnboardingRepository {

    override suspend fun registerCat(name: String) =
        dataSource.registerCat(name).toVO()
}
