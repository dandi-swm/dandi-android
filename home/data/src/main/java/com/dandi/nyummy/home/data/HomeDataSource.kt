package com.dandi.nyummy.home.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.home.data.dto.HomeDTO

class HomeDataSource(
    private val apiService: HomeApiService,
) : BaseRemoteDataSource() {
    suspend fun getHome(): HomeDTO =
        checkResponse(apiService.getHome())
}
