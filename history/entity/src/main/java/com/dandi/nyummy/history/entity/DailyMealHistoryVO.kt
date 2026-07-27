package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 선택한 날짜의 상세 기록(식사 목록 + 하루 영양 요약)입니다.
 */
@Serializable
data class DailyMealHistoryVO(
    val date: HistoryDateVO = HistoryDateVO(),
    val meals: List<MealHistoryVO> = emptyList(),
    val nutrition: DailyNutritionVO = DailyNutritionVO(),
) {

    companion object {
        val empty = DailyMealHistoryVO()
    }
}
