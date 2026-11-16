package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.data.dto.CreateAnalysisFeedbackRequestDTO
import com.dandi.nyummy.history.data.dto.DailyMealsDTO
import com.dandi.nyummy.history.data.dto.MealDTO
import com.dandi.nyummy.history.data.dto.MealStatusDTO
import com.dandi.nyummy.history.data.dto.MealV2DTO
import com.dandi.nyummy.history.data.dto.MonthlyMealsDTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodsV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealNameV2RequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 식사 히스토리 캘린더 API
 */
interface HistoryApiService {
    /** 월간 식사 캘린더 조회 */
    @GET("$MEALS_PATH/monthly")
    suspend fun getMonthlyMeals(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): Response<MonthlyMealsDTO>

    /** 일일 식사 목록 조회 v2 */
    @GET("$MEALS_V2_PATH/daily")
    suspend fun getDailyMeals(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("day") day: Int,
    ): Response<DailyMealsDTO>

    /** 식사 단건 조회 */
    @GET("$MEALS_PATH/{mealId}")
    suspend fun getMeal(@Path("mealId") mealId: Long): Response<MealDTO>

    /** 식사 이름 수정 */
    @PUT("$MEALS_PATH/{mealId}")
    suspend fun updateMealName(
        @Path("mealId") mealId: Long,
        @Query("name") name: String,
    ): Response<MealDTO>

    /** 식사 재분석 요청 v2 */
    @POST("$MEALS_V2_PATH/{mealId}/analysis")
    suspend fun reanalyzeMeal(@Path("mealId") mealId: Long): Response<MealStatusDTO>

    /** 영양 분석 평가 등록 */
    @POST("$MEALS_PATH/{mealId}/analysis/feedback")
    suspend fun createAnalysisFeedback(
        @Path("mealId") mealId: Long,
        @Body request: CreateAnalysisFeedbackRequestDTO,
    ): Response<Unit>

    /** 식사 삭제 */
    @DELETE("$MEALS_PATH/{mealId}")
    suspend fun deleteMeal(@Path("mealId") mealId: Long): Response<Unit>

    /** 식사 단건 조회 v2 */
    @GET("$MEALS_V2_PATH/{mealId}")
    suspend fun getMealV2(@Path("mealId") mealId: Long): Response<MealV2DTO>

    /** 식사 이름 수정 v2 */
    @PUT("$MEALS_V2_PATH/{mealId}")
    suspend fun updateMealNameV2(
        @Path("mealId") mealId: Long,
        @Body request: UpdateMealNameV2RequestDTO,
    ): Response<MealV2DTO>

    /** 식사 삭제 v2 */
    @DELETE("$MEALS_V2_PATH/{mealId}")
    suspend fun deleteMealV2(@Path("mealId") mealId: Long): Response<Unit>

    /** 음식 목록 보내기 v2 */
    @PUT("$MEALS_V2_PATH/{mealId}/foods")
    suspend fun updateMealFoodsV2(
        @Path("mealId") mealId: Long,
        @Body request: UpdateMealFoodsV2RequestDTO,
    ): Response<MealStatusDTO>

    /** 음식 줄 수정 v2 */
    @PATCH("$MEALS_V2_PATH/{mealId}/foods/{mealFoodId}")
    suspend fun updateMealFoodV2(
        @Path("mealId") mealId: Long,
        @Path("mealFoodId") mealFoodId: Long,
        @Body request: UpdateMealFoodV2RequestDTO,
    ): Response<Unit>

    /** 음식 줄 삭제 v2 */
    @DELETE("$MEALS_V2_PATH/{mealId}/foods/{mealFoodId}")
    suspend fun deleteMealFoodV2(
        @Path("mealId") mealId: Long,
        @Path("mealFoodId") mealFoodId: Long,
    ): Response<Unit>

    /** 영양 분석 상태 조회 v2 */
    @GET("$MEALS_V2_PATH/{mealId}/analysis")
    suspend fun getMealStatusV2(@Path("mealId") mealId: Long): Response<MealStatusDTO>

    companion object {
        const val MEALS_PATH = "/api/v1/meals"
        const val MEALS_V2_PATH = "/api/v2/meals"
    }
}
