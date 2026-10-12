package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.UserProfileDTO

class SettingsRemoteDataSource(
    private val apiService: SettingsApiService,
) : BaseRemoteDataSource() {

    suspend fun getMyProfile(): UserProfileDTO = checkResponse(apiService.getMyProfile())

    suspend fun updateMealTime(request: MealTimeRequestDTO) {
        checkResponse(apiService.updateMealTime(request))
    }
}
