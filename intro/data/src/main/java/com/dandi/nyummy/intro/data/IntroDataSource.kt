package com.dandi.nyummy.intro.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.intro.data.dto.IntroDTO

class IntroDataSource(
    private val apiService: IntroApiService,
) : BaseRemoteDataSource() {
    suspend fun getIntro(): IntroDTO = checkResponse(apiService.getIntro())
}
