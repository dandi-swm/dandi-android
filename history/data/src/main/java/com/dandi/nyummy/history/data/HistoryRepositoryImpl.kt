package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.domain.HistoryRepository

class HistoryRepositoryImpl(
    private val dataSource: HistoryDataSource,
) : HistoryRepository {

    override suspend fun getMonthlyCalendar(year: Int, month: Int) =
        dataSource.getMonthlyMeals(year, month).toVO()

    override suspend fun getDailyMeals(year: Int, month: Int, day: Int) =
        dataSource.getDailyMeals(year, month, day).toVO()

    override suspend fun getMeal(mealId: Long) =
        dataSource.getMeal(mealId).toVO()

    override suspend fun updateMealName(mealId: Long, name: String) =
        dataSource.updateMealName(mealId, name).toVO()

    override suspend fun deleteMeal(mealId: Long) =
        dataSource.deleteMeal(mealId)
}
