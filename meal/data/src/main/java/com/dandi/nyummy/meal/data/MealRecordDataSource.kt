package com.dandi.nyummy.meal.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO

class MealRecordDataSource(
    private val apiService: MealRecordApiService,
) : BaseRemoteDataSource() {
    suspend fun createMeal(request: CreateMealRequestDTO): CreatedMealDTO =
        checkResponse(apiService.createMeal(request))

    suspend fun createTutorialMeal(): CreatedMealDTO =
        checkResponse(apiService.createTutorialMeal())
}
