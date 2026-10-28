package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatDTO
import com.dandi.nyummy.common.data.BaseRemoteDataSource

class CatDataSource(
    private val apiService: CatApiService,
) : BaseRemoteDataSource() {
    suspend fun getCat(): CatDTO =
        checkResponse(apiService.getCat())
}
