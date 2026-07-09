package com.swm.dandi.meal.data

import com.swm.dandi.meal.data.dto.CreateMealRequestDTO
import com.swm.dandi.meal.data.dto.CreateMealResponseDTO
import com.swm.dandi.meal.data.dto.MealStatusSheetResponseDTO
import com.swm.dandi.meal.data.dto.MealPhotoUploadUrlRequestDTO
import com.swm.dandi.meal.data.dto.MealPhotoUploadUrlResponseDTO
import com.swm.dandi.meal.data.dto.NutritionAnalysisResponseDTO
import com.swm.dandi.meal.data.dto.PreviousMealPageResponseDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface MealApiService {

    @GET("meal-status-sheet")
    suspend fun getMealStatusSheet(): Response<MealStatusSheetResponseDTO>

    @GET("previous-meal-page")
    suspend fun getPreviousMealPage(): Response<PreviousMealPageResponseDTO>

    @POST("meal-photo-upload-urls")
    suspend fun getMealPhotoUploadUrl(
        @Body request: MealPhotoUploadUrlRequestDTO,
    ): Response<MealPhotoUploadUrlResponseDTO>

    @POST("meals")
    suspend fun createMeal(
        @Body request: CreateMealRequestDTO,
    ): Response<CreateMealResponseDTO>

    @GET("nutrition-analyses/{mealRecordId}")
    suspend fun getNutritionAnalysis(
        @Path("mealRecordId") mealRecordId: String,
    ): Response<NutritionAnalysisResponseDTO>
}
