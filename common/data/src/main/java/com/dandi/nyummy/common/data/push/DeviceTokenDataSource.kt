package com.dandi.nyummy.common.data.push

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.common.data.push.dto.DeviceTokenRequestDTO

class DeviceTokenDataSource(
    private val apiService: DeviceTokenApiService,
) : BaseRemoteDataSource() {
    suspend fun register(token: String) {
        checkResponse(
            apiService.register(DeviceTokenRequestDTO(token = token, platform = DeviceTokenRequestDTO.PLATFORM_ANDROID)),
        )
    }
}
