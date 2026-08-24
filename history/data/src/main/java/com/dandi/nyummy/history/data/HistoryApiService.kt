package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.data.dto.DailyMealsDTO
import com.dandi.nyummy.history.data.dto.MealDTO
import com.dandi.nyummy.history.data.dto.MonthlyMealsDTO
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
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

    /** 일일 식사 목록 조회 */
    @GET("$MEALS_PATH/daily")
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

    /** 식사 삭제 */
    @DELETE("$MEALS_PATH/{mealId}")
    suspend fun deleteMeal(@Path("mealId") mealId: Long): Response<Unit>

    companion object {
        const val MEALS_PATH = "/api/v1/meals"
    }
}
