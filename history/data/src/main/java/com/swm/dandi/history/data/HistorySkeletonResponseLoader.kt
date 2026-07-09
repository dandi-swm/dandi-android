package com.swm.dandi.history.data

import android.content.res.AssetManager
import com.swm.dandi.common.domain.error.HttpResponseException
import com.swm.dandi.common.domain.error.HttpResponseStatus
import com.swm.dandi.history.data.dto.HistoryCalendarResponseDTO
import com.swm.dandi.history.data.dto.HistoryMealsResponseDTO
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HistorySkeletonResponseLoader(
    private val assetManager: AssetManager,
    private val json: Json,
) {
    fun loadHistoryCalendar(): HistoryCalendarResponseDTO =
        decode(HISTORY_CALENDAR_FILE)

    fun loadHistoryMeals(): HistoryMealsResponseDTO =
        decode(HISTORY_MEALS_FILE)

    fun emptyHistoryMeals(date: String): HistoryMealsResponseDTO =
        HistoryMealsResponseDTO(date = date, meals = emptyList())

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
        const val SKELETON_ASSET_DIR = "skeleton/history"
        const val HISTORY_CALENDAR_FILE = "history_calendar.json"
        const val HISTORY_MEALS_FILE = "history_meals_2025_07_03.json"
    }
}
