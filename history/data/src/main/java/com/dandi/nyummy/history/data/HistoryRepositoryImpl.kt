package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.data.dto.CreateAnalysisFeedbackRequestDTO
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

    override suspend fun reanalyzeMeal(mealId: Long) =
        dataSource.reanalyzeMeal(mealId).toVO()

    // 도메인은 별점을 반 칸 수(1~10)로 다루고, 서버에는 0.5 단위 별점으로 보낸다.
    override suspend fun submitAnalysisFeedback(mealId: Long, ratingHalfSteps: Int, reason: String?) =
        dataSource.createAnalysisFeedback(
            mealId,
            CreateAnalysisFeedbackRequestDTO(rating = ratingHalfSteps / 2f, reason = reason),
        )

    override suspend fun deleteMeal(mealId: Long) =
        dataSource.deleteMeal(mealId)
}
