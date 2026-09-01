package com.dandi.nyummy.meal.data

import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO
import com.dandi.nyummy.meal.data.dto.MealImageUploadDTO
import com.dandi.nyummy.meal.data.dto.UploadImageUrlRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 식사 기록 생성 API
 */
interface MealRecordApiService {
    /** 이미지 업로드 URL 발급 */
    @POST("$MEALS_PATH/images/presigned-url")
    suspend fun issueImageUploadUrl(@Body request: UploadImageUrlRequestDTO): Response<MealImageUploadDTO>

    /** 식사 생성 */
    @POST(MEALS_PATH)
    suspend fun createMeal(@Body request: CreateMealRequestDTO): Response<CreatedMealDTO>

    companion object {
        const val MEALS_PATH = "/api/v1/meals"
    }
}
