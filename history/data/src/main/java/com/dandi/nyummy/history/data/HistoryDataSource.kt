package com.dandi.nyummy.history.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.history.data.dto.CreateAnalysisFeedbackRequestDTO
import com.dandi.nyummy.history.data.dto.DailyMealsDTO
import com.dandi.nyummy.history.data.dto.MealDTO
import com.dandi.nyummy.history.data.dto.MealStatusDTO
import com.dandi.nyummy.history.data.dto.MealV2DTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodsV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealNameV2RequestDTO
import com.dandi.nyummy.history.data.dto.MonthlyMealsDTO

class HistoryDataSource(
    private val apiService: HistoryApiService,
) : BaseRemoteDataSource() {
    suspend fun getMonthlyMeals(year: Int, month: Int): MonthlyMealsDTO =
        checkResponse(apiService.getMonthlyMeals(year, month))

    suspend fun getDailyMeals(year: Int, month: Int, day: Int): DailyMealsDTO =
        checkResponse(apiService.getDailyMeals(year, month, day))

    suspend fun getMeal(mealId: Long): MealDTO =
        checkResponse(apiService.getMeal(mealId))

    suspend fun updateMealName(mealId: Long, name: String): MealDTO =
        checkResponse(apiService.updateMealName(mealId, name))

    suspend fun reanalyzeMeal(mealId: Long): MealStatusDTO =
        checkResponse(apiService.reanalyzeMeal(mealId))

    suspend fun createAnalysisFeedback(mealId: Long, request: CreateAnalysisFeedbackRequestDTO) =
        checkResponse(apiService.createAnalysisFeedback(mealId, request))

    suspend fun deleteMeal(mealId: Long) =
        checkResponse(apiService.deleteMeal(mealId))

    suspend fun getMealV2(mealId: Long): MealV2DTO =
        checkResponse(apiService.getMealV2(mealId))

    suspend fun updateMealNameV2(mealId: Long, request: UpdateMealNameV2RequestDTO): MealV2DTO =
        checkResponse(apiService.updateMealNameV2(mealId, request))

    suspend fun deleteMealV2(mealId: Long) =
        checkResponse(apiService.deleteMealV2(mealId))

    suspend fun updateMealFoodsV2(mealId: Long, request: UpdateMealFoodsV2RequestDTO): MealStatusDTO =
        checkResponse(apiService.updateMealFoodsV2(mealId, request))

    suspend fun updateMealFoodV2(mealId: Long, mealFoodId: Long, request: UpdateMealFoodV2RequestDTO) =
        checkResponse(apiService.updateMealFoodV2(mealId, mealFoodId, request))

    suspend fun deleteMealFoodV2(mealId: Long, mealFoodId: Long) =
        checkResponse(apiService.deleteMealFoodV2(mealId, mealFoodId))

    suspend fun getMealStatusV2(mealId: Long): MealStatusDTO =
        checkResponse(apiService.getMealStatusV2(mealId))
}
