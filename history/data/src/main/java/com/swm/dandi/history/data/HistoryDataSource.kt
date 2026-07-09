package com.swm.dandi.history.data

import com.swm.dandi.common.data.BaseRemoteDataSource
import com.swm.dandi.history.data.dto.HistoryCalendarResponseDTO
import com.swm.dandi.history.data.dto.HistoryMealsResponseDTO

class HistoryDataSource(
    private val apiService: HistoryApiService,
    private val skeletonResponseLoader: HistorySkeletonResponseLoader,
) : BaseRemoteDataSource() {

    suspend fun getHistory(year: Int, month: Int): HistoryCalendarResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            // TODO-API-SPEC: replace skeleton response with checkResponse(apiService.getHistory(year, month)).
            skeletonResponseLoader.loadHistoryCalendar()
        } else {
            checkResponse(apiService.getHistory(year, month))
        }

    suspend fun getMeals(date: String): HistoryMealsResponseDTO =
        if (USE_SKELETON_RESPONSE) {
            // TODO-API-SPEC: replace skeleton response with checkResponse(apiService.getMeals(date)).
            when (date) {
                SKELETON_SELECTED_DATE -> skeletonResponseLoader.loadHistoryMeals()
                else -> skeletonResponseLoader.emptyHistoryMeals(date)
            }
        } else {
            checkResponse(apiService.getMeals(date))
        }

    private companion object {
        const val USE_SKELETON_RESPONSE = true
        const val SKELETON_SELECTED_DATE = "2025-07-03"
    }
}
