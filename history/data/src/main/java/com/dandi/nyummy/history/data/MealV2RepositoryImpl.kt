package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.data.dto.MealFoodLineRequestDTO
import com.dandi.nyummy.history.data.dto.MealFoodNutritionRequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealFoodsV2RequestDTO
import com.dandi.nyummy.history.data.dto.UpdateMealNameV2RequestDTO
import com.dandi.nyummy.history.domain.MealV2Repository
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO

class MealV2RepositoryImpl(
    private val dataSource: HistoryDataSource,
) : MealV2Repository {

    override suspend fun getMeal(mealId: Long) =
        dataSource.getMealV2(mealId).toVO()

    override suspend fun updateMealName(mealId: Long, name: String) =
        dataSource.updateMealNameV2(mealId, UpdateMealNameV2RequestDTO(name)).toVO()

    override suspend fun deleteMeal(mealId: Long) =
        dataSource.deleteMealV2(mealId)

    override suspend fun submitFoods(mealId: Long, foods: List<MealFoodLineVO>) =
        dataSource.updateMealFoodsV2(mealId, UpdateMealFoodsV2RequestDTO(foods.map { it.toRequest() })).toVO()

    override suspend fun updateFood(mealId: Long, mealFoodId: Long, change: MealFoodChangeVO) =
        dataSource.updateMealFoodV2(mealId, mealFoodId, change.toRequest())

    override suspend fun deleteFood(mealId: Long, mealFoodId: Long) =
        dataSource.deleteMealFoodV2(mealId, mealFoodId)

    override suspend fun getAnalysisStatus(mealId: Long) =
        dataSource.getMealStatusV2(mealId).toVO()
}

internal fun MealFoodLineVO.toRequest() = MealFoodLineRequestDTO(
    mealFoodId = mealFoodId.takeUnless { it == MealFoodLineVO.NEW_ID },
    name = name,
    portion = portion,
)

internal fun MealFoodChangeVO.toRequest() = UpdateMealFoodV2RequestDTO(
    name = name,
    portion = portion,
    nutrition = nutrition?.let {
        MealFoodNutritionRequestDTO(
            calory = it.calorieKcal,
            carbs = it.carbohydrateGram,
            protein = it.proteinGram,
            fat = it.fatGram,
        )
    },
    iconId = iconId,
)
