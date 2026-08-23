package com.dandi.nyummy.intro.data

import com.dandi.nyummy.intro.data.dto.IntroDTO
import retrofit2.Response
import retrofit2.http.GET

/** 앱 시작 게이트 API. */
interface IntroApiService {
    @GET("/intro")
    suspend fun getIntro(): Response<IntroDTO>
}
