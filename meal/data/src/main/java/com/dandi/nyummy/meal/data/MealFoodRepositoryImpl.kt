package com.dandi.nyummy.meal.data

import com.dandi.nyummy.meal.data.dto.MealFoodRequestDTO
import com.dandi.nyummy.meal.data.dto.UpdateMealFoodsRequestDTO
import com.dandi.nyummy.meal.domain.MealFoodRepository
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO

class MealFoodRepositoryImpl(
    private val dataSource: MealRecordDataSource,
) : MealFoodRepository {

    override suspend fun getFoundFoods(mealId: Long): MealFoodConfirmVO =
        dataSource.getMealFoods(mealId).toVO()

    override suspend fun submitFoods(mealId: Long, foods: List<FoundFoodVO>) {
        dataSource.updateMealFoods(mealId, UpdateMealFoodsRequestDTO(foods.map { it.toRequest() }))
    }

    override suspend fun cancelMeal(mealId: Long) {
        dataSource.cancelMeal(mealId)
    }

    private companion object {
        /** 고르기 화면에서는 인분을 고치지 않는다. 서버가 찾은 줄도 새로 추가한 줄도 1인분으로 보낸다. */
        const val DEFAULT_PORTION = 1.0

        fun FoundFoodVO.toRequest() = MealFoodRequestDTO(
            mealFoodId = mealFoodId.takeUnless { isNew },
            name = name,
            portion = DEFAULT_PORTION,
        )
    }
}
