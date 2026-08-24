package com.dandi.nyummy.history.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.history.data.dto.DailyMealsDTO
import com.dandi.nyummy.history.data.dto.MealDTO
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

    suspend fun deleteMeal(mealId: Long) =
        checkResponse(apiService.deleteMeal(mealId))
}
