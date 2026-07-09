package com.swm.dandi.meal.data

import com.swm.dandi.meal.data.dto.toDTO
import com.swm.dandi.meal.data.dto.toVO
import com.swm.dandi.meal.domain.MealRepository
import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.CreateMealVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlRequestVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlVO
import com.swm.dandi.meal.entity.MealStatusSheetVO
import com.swm.dandi.meal.entity.NutritionAnalysisVO
import com.swm.dandi.meal.entity.PreviousMealPageVO

class MealRepositoryImpl(
    private val dataSource: MealDataSource,
) : MealRepository {

    override suspend fun getMealStatusSheet(): MealStatusSheetVO =
        dataSource.getMealStatusSheet().toVO()

    override suspend fun getPreviousMealPage(): PreviousMealPageVO =
        dataSource.getPreviousMealPage().toVO()

    override suspend fun getMealPhotoUploadUrl(request: MealPhotoUploadUrlRequestVO): MealPhotoUploadUrlVO =
        dataSource.getMealPhotoUploadUrl(request.toDTO()).toVO()

    override suspend fun createMeal(request: CreateMealRequestVO): CreateMealVO =
        dataSource.createMeal(request.toDTO()).toVO()

    override suspend fun getNutritionAnalysis(mealRecordId: String): NutritionAnalysisVO =
        dataSource.getNutritionAnalysis(mealRecordId).toVO()
}
