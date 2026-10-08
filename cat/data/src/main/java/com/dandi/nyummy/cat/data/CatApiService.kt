package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import retrofit2.Response
import retrofit2.http.GET

/**
 * 고양이 API. 고양이는 토큰의 사용자로 찾으므로 따로 id를 넘기지 않는다.
 */
interface CatApiService {
    /** 내 고양이 체형에 맞는 상태별 애니메이션 메타데이터 조회 */
    @GET(CAT_ANIMATIONS_PATH)
    suspend fun getAnimations(): Response<CatAnimationSetDTO>

    companion object {
        const val CAT_ANIMATIONS_PATH = "/api/v1/cats/animations"
    }
}
