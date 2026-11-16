package com.dandi.nyummy.meal.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO
import com.dandi.nyummy.meal.data.dto.MealFoodConfirmDTO
import com.dandi.nyummy.meal.data.dto.UpdateMealFoodsRequestDTO

class MealRecordDataSource(
    private val apiService: MealRecordApiService,
) : BaseRemoteDataSource() {
    suspend fun createMeal(request: CreateMealRequestDTO): CreatedMealDTO =
        checkResponse(apiService.createMeal(request))

    /** 본문 없는 204가 성공이다. */
    suspend fun createTutorialMeal(): Unit =
        checkResponse(apiService.createTutorialMeal())

    suspend fun getMealFoods(mealId: Long): MealFoodConfirmDTO =
        checkResponse(apiService.getMealFoods(mealId))

    /** 응답의 분석 상태는 쓰지 않는다. 영양 계산 결과는 알림으로 온다. */
    suspend fun updateMealFoods(mealId: Long, request: UpdateMealFoodsRequestDTO) {
        checkResponse(apiService.updateMealFoods(mealId, request))
    }

    /** 본문 없는 204가 성공이다. */
    suspend fun cancelMeal(mealId: Long): Unit =
        checkResponse(apiService.cancelMeal(mealId))
}
