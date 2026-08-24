package com.dandi.nyummy.history.domain

import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.MealHistoryVO

interface HistoryRepository {
    /** 월간 식사 캘린더 조회 (GET /api/v1/meals/monthly). */
    suspend fun getMonthlyCalendar(year: Int, month: Int): HistoryCalendarVO

    /** 일일 식사 목록 + 하루 영양 조회 (GET /api/v1/meals/daily). */
    suspend fun getDailyMeals(year: Int, month: Int, day: Int): DailyMealHistoryVO

    /** 식사 단건 상세 조회 (GET /api/v1/meals/{mealId}). 사진 URL 포함. */
    suspend fun getMeal(mealId: Long): MealHistoryVO

    /** 식사 이름 수정 (PUT /api/v1/meals/{mealId}). 수정된 식사를 반환. */
    suspend fun updateMealName(mealId: Long, name: String): MealHistoryVO

    /** 식사 삭제 (DELETE /api/v1/meals/{mealId}). */
    suspend fun deleteMeal(mealId: Long)
}
