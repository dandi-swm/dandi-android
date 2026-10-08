package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import com.dandi.nyummy.common.data.BaseRemoteDataSource

class CatDataSource(
    private val apiService: CatApiService,
) : BaseRemoteDataSource() {
    suspend fun getAnimations(): CatAnimationSetDTO =
        checkResponse(apiService.getAnimations())
}
