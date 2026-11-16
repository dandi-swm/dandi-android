package com.dandi.nyummy.meal.data

import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO
import retrofit2.Response
import com.dandi.nyummy.meal.data.dto.MealFoodConfirmDTO
import com.dandi.nyummy.meal.data.dto.MealFoodsStatusDTO
import com.dandi.nyummy.meal.data.dto.UpdateMealFoodsRequestDTO
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 식사 기록 생성과 찾은 음식 확인 API. 사진 업로드 URL 발급은 공통 이미지 업로드(`ImageUploadDataSource`, 용도 MEAL)가 맡는다.
 */
interface MealRecordApiService {
    /** 식사 생성 v2 */
    @POST(MEALS_V2_PATH)
    suspend fun createMeal(@Body request: CreateMealRequestDTO): Response<CreatedMealDTO>

    /** 식사 단건 조회 v2 */
    @GET("$MEALS_V2_PATH/{mealId}")
    suspend fun getMealFoods(@Path("mealId") mealId: Long): Response<MealFoodConfirmDTO>

    /** 음식 목록 보내기 v2 */
    @PUT("$MEALS_V2_PATH/{mealId}/foods")
    suspend fun updateMealFoods(
        @Path("mealId") mealId: Long,
        @Body request: UpdateMealFoodsRequestDTO,
    ): Response<MealFoodsStatusDTO>

    /** 식사 기록 취소 v2 */
    @POST("$MEALS_V2_PATH/{mealId}/cancel")
    suspend fun cancelMeal(@Path("mealId") mealId: Long): Response<Unit>

    /** 튜토리얼 식사 생성 */
    @POST(TUTORIAL_MEAL_PATH)
    suspend fun createTutorialMeal(): Response<Unit>

    companion object {
        const val MEALS_V2_PATH = "/api/v2/meals"
        const val TUTORIAL_MEAL_PATH = "/api/v1/meals/tutorial"
    }
}
