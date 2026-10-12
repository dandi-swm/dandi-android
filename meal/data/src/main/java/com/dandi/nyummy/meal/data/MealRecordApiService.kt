package com.dandi.nyummy.meal.data

import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 식사 기록 생성 API. 사진 업로드 URL 발급은 공통 이미지 업로드(`ImageUploadDataSource`, 용도 MEAL)가 맡는다.
 */
interface MealRecordApiService {
    /** 식사 생성 */
    @POST(MEALS_PATH)
    suspend fun createMeal(@Body request: CreateMealRequestDTO): Response<CreatedMealDTO>

    companion object {
        const val MEALS_PATH = "/api/v1/meals"
    }
}
