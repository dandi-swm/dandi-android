package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.onboarding.data.dto.CatDTO
import com.dandi.nyummy.onboarding.data.dto.CatRegisterRequestDTO

class OnboardingDataSource(
    private val apiService: OnboardingApiService,
) : BaseRemoteDataSource() {
    suspend fun registerCat(name: String): CatDTO =
        checkResponse(apiService.registerCat(CatRegisterRequestDTO(name = name)))
}
