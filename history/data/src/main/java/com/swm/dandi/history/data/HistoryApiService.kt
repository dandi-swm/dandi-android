package com.swm.dandi.history.data

import com.swm.dandi.history.data.dto.HistoryCalendarResponseDTO
import com.swm.dandi.history.data.dto.HistoryMealsResponseDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface HistoryApiService {

    // TODO-API-SPEC: align query names and response fields with the finalized GET /history contract.
    @GET("history")
    suspend fun getHistory(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): Response<HistoryCalendarResponseDTO>

    // TODO-API-SPEC: align query names and response fields with the finalized GET /history-meals contract.
    @GET("history-meals")
    suspend fun getMeals(
        @Query("date") date: String,
    ): Response<HistoryMealsResponseDTO>
}
