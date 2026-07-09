package com.swm.dandi.meal.data

import com.swm.dandi.common.data.BaseRemoteDataSource
import com.swm.dandi.meal.data.dto.CreateMealRequestDTO
import com.swm.dandi.meal.data.dto.CreateMealResponseDTO
import com.swm.dandi.meal.data.dto.MealPhotoUploadUrlRequestDTO
import com.swm.dandi.meal.data.dto.MealPhotoUploadUrlResponseDTO
import com.swm.dandi.meal.data.dto.MealStatusSheetResponseDTO
import com.swm.dandi.meal.data.dto.NutritionAnalysisResponseDTO
import com.swm.dandi.meal.data.dto.PreviousMealPageResponseDTO

class MealDataSource(
    private val apiService: MealApiService,
    private val skeletonResponseLoader: MealSkeletonResponseLoader,
) : BaseRemoteDataSource() {

    suspend fun getMealStatusSheet(): MealStatusSheetResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            skeletonResponseLoader.loadMealStatusSheet()
        } else {
            checkResponse(apiService.getMealStatusSheet())
        }

    suspend fun getPreviousMealPage(): PreviousMealPageResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            skeletonResponseLoader.loadPreviousMealPage()
        } else {
            checkResponse(apiService.getPreviousMealPage())
        }

    suspend fun getMealPhotoUploadUrl(request: MealPhotoUploadUrlRequestDTO): MealPhotoUploadUrlResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            skeletonResponseLoader.loadMealPhotoUploadUrl()
        } else {
            checkResponse(apiService.getMealPhotoUploadUrl(request))
        }

    suspend fun createMeal(request: CreateMealRequestDTO): CreateMealResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            skeletonResponseLoader.loadCreateMeal()
        } else {
            checkResponse(apiService.createMeal(request))
        }

    suspend fun getNutritionAnalysis(mealRecordId: String): NutritionAnalysisResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            skeletonResponseLoader.loadNutritionAnalysis()
        } else {
            checkResponse(apiService.getNutritionAnalysis(mealRecordId))
        }

    private companion object {
        // TODO 백엔드 stub API가 준비되면 false로 바꾸고 실제 HTTP 경로를 사용한다.
        const val USE_SKELETON_RESPONSE = true
    }
}
