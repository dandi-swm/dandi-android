package com.swm.dandi.history.data

import com.swm.dandi.history.data.dto.toVO
import com.swm.dandi.history.domain.HistoryRepository
import com.swm.dandi.history.entity.HistoryMealsVO
import com.swm.dandi.history.entity.HistoryVO

class HistoryRepositoryImpl(
    private val dataSource: HistoryDataSource,
) : HistoryRepository {

    override suspend fun getHistory(year: Int, month: Int): HistoryVO =
        dataSource.getHistory(year, month).toVO()

    override suspend fun getMeals(date: String): HistoryMealsVO =
        dataSource.getMeals(date).toVO()
}
