package com.swm.dandi.meal.data

import android.content.res.AssetManager
import com.swm.dandi.common.domain.error.HttpResponseException
import com.swm.dandi.common.domain.error.HttpResponseStatus
import com.swm.dandi.meal.data.dto.CreateMealResponseDTO
import com.swm.dandi.meal.data.dto.MealPhotoUploadUrlResponseDTO
import com.swm.dandi.meal.data.dto.MealStatusSheetResponseDTO
import com.swm.dandi.meal.data.dto.NutritionAnalysisResponseDTO
import com.swm.dandi.meal.data.dto.PreviousMealPageResponseDTO
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MealSkeletonResponseLoader(
    private val assetManager: AssetManager,
    private val json: Json,
) {
    fun loadMealStatusSheet(): MealStatusSheetResponseDTO =
        decode(MEAL_STATUS_SHEET_FILE)

    fun loadPreviousMealPage(): PreviousMealPageResponseDTO =
        decode(PREVIOUS_MEAL_PAGE_FILE)

    fun loadMealPhotoUploadUrl(): MealPhotoUploadUrlResponseDTO =
        decode(MEAL_PHOTO_UPLOAD_URL_FILE)

    fun loadCreateMeal(): CreateMealResponseDTO =
        decode(CREATE_MEAL_FILE)

    fun loadNutritionAnalysis(): NutritionAnalysisResponseDTO =
        decode(NUTRITION_ANALYSIS_FILE)

    private inline fun <reified T> decode(fileName: String): T {
        val assetPath = "$SKELETON_ASSET_DIR/$fileName"
        return try {
            val responseJson = assetManager.open(assetPath).bufferedReader().use { it.readText() }
            json.decodeFromString(responseJson)
        } catch (exception: Exception) {
            throw exception.toSkeletonResponseException(assetPath)
        }
    }

    private fun Exception.toSkeletonResponseException(assetPath: String): HttpResponseException =
        HttpResponseException(
            status = HttpResponseStatus.InternalError,
            rawCode = HttpResponseStatus.InternalError.code,
            errorRequestUrl = "asset://$assetPath",
            msg = "Skeleton response load failed: $assetPath",
            cause = this,
        )

    private companion object {
        const val SKELETON_ASSET_DIR = "skeleton/meal"
        const val MEAL_STATUS_SHEET_FILE = "meal_status_sheet.json"
        const val PREVIOUS_MEAL_PAGE_FILE = "previous_meal_page.json"
        const val MEAL_PHOTO_UPLOAD_URL_FILE = "meal_photo_upload_url.json"
        const val CREATE_MEAL_FILE = "create_meal.json"
        const val NUTRITION_ANALYSIS_FILE = "nutrition_analysis.json"
    }
}
