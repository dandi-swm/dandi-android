package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.onboarding.data.dto.CreateCatRequestDTO

class OnboardingDataSource(
    private val apiService: OnboardingApiService,
) : BaseRemoteDataSource() {
    suspend fun createCat(request: CreateCatRequestDTO) {
        checkResponse(apiService.createCat(request))
    }
}
