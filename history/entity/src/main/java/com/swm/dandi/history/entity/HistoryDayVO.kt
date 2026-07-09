package com.swm.dandi.history.entity

/**
 * 캘린더 한 칸에 표시할 날짜별 기록 요약.
 *
 * `foodImageUrls`는 상세 식사 목록이 아니라 좁은 캘린더 셀에 보여줄 대표 이미지 요약이다.
 *
 * @property date 날짜 문자열. 예: `2025-07-03`.
 * @property dayOfMonth 셀에 표시할 일자 숫자.
 * @property isCurrentMonth 현재 조회 월에 속하는 날짜인지 여부.
 * @property dailyNutritionEvaluation 날짜별 일일 영양 평가.
 * @property foodImageUrls 날짜 셀에 요약 표시할 음식 아이콘 이미지 URL 목록.
 */
data class HistoryDayVO(
    val date: String = "",
    val dayOfMonth: Int = 0,
    val isCurrentMonth: Boolean = true,
    val dailyNutritionEvaluation: HistoryDailyNutritionEvaluationVO = HistoryDailyNutritionEvaluationVO.UNRECORDED,
    val foodImageUrls: List<String> = emptyList(),
) {
    companion object {
        val empty: HistoryDayVO = HistoryDayVO()
    }
}
