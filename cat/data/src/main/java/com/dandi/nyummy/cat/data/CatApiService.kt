package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatDTO
import com.dandi.nyummy.cat.data.dto.UpdateCatRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * 고양이 API. 고양이는 토큰의 사용자로 찾으므로 따로 id를 넘기지 않는다.
 */
interface CatApiService {
    /** 내 고양이 조회 */
    @GET(CAT_PATH)
    suspend fun getCat(): Response<CatDTO>

    /** 내 고양이 변경(지금은 이름만). 성공은 본문 없는 200 */
    @PATCH(CAT_PATH)
    suspend fun updateCat(@Body request: UpdateCatRequestDTO): Response<Unit>

    companion object {
        const val CAT_PATH = "/api/v1/cats"
    }
}
