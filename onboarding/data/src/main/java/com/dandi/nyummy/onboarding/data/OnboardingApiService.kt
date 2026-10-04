package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.onboarding.data.dto.CatDTO
import com.dandi.nyummy.onboarding.data.dto.CatRegisterRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 온보딩 고양이 API (서버 명세 확정 전 임시 명세)
 */
interface OnboardingApiService {
    /** 고양이 이름 등록 */
    @POST(CATS_PATH)
    suspend fun registerCat(@Body request: CatRegisterRequestDTO): Response<CatDTO>

    companion object {
        const val CATS_PATH = "/api/v1/cats"
    }
}
