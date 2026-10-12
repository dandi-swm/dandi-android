package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.onboarding.data.dto.CreateCatRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 온보딩 고양이 API
 */
interface OnboardingApiService {
    /** 고양이 생성. 이름과 평소 식사 시각을 한 번에 보낸다. 성공은 본문 없는 201이다. */
    @POST(CATS_PATH)
    suspend fun createCat(@Body request: CreateCatRequestDTO): Response<Unit>

    companion object {
        const val CATS_PATH = "/api/v1/cats"
    }
}
