package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatDTO
import retrofit2.Response
import retrofit2.http.GET

/**
 * 고양이 API. 고양이는 토큰의 사용자로 찾으므로 따로 id를 넘기지 않는다.
 */
interface CatApiService {
    /** 내 고양이 조회 */
    @GET(CAT_PATH)
    suspend fun getCat(): Response<CatDTO>

    companion object {
        const val CAT_PATH = "/api/v1/cats"
    }
}
