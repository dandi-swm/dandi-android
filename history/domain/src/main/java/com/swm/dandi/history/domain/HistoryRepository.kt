package com.swm.dandi.history.domain

import com.swm.dandi.history.entity.HistoryMealsVO
import com.swm.dandi.history.entity.HistoryVO

interface HistoryRepository {
    suspend fun getHistory(year: Int, month: Int): HistoryVO

    suspend fun getMeals(date: String): HistoryMealsVO
}
