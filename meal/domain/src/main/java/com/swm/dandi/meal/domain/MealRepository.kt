package com.swm.dandi.meal.domain

import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.CreateMealVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlRequestVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlVO
import com.swm.dandi.meal.entity.MealStatusSheetVO
import com.swm.dandi.meal.entity.NutritionAnalysisVO
import com.swm.dandi.meal.entity.PreviousMealPageVO

interface MealRepository {
    suspend fun getMealStatusSheet(): MealStatusSheetVO

    suspend fun getPreviousMealPage(): PreviousMealPageVO

    suspend fun getMealPhotoUploadUrl(request: MealPhotoUploadUrlRequestVO): MealPhotoUploadUrlVO

    suspend fun createMeal(request: CreateMealRequestVO): CreateMealVO

    suspend fun getNutritionAnalysis(mealRecordId: String): NutritionAnalysisVO
}
