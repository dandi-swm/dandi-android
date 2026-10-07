package com.dandi.nyummy.home.data

import com.dandi.nyummy.home.data.dto.HomeDTO
import retrofit2.Response
import retrofit2.http.GET

/**
 * 홈 화면 API. 홈을 그리는 데 필요한 여러 도메인의 값(코인, 연속 기록, 오늘 식사 현황)을 한 번에 받는다.
 */
interface HomeApiService {
    /** 홈 화면 조회 */
    @GET(HOME_PATH)
    suspend fun getHome(): Response<HomeDTO>

    companion object {
        const val HOME_PATH = "/api/v1/home"
    }
}
