package com.swm.dandi.history.entity

/**
 * 날짜별 일일 영양 평가.
 *
 * 캘린더 셀에서 하루 식사가 건강 기준을 얼마나 충족했는지 구분하는 서버 코드다.
 */
enum class HistoryDailyNutritionEvaluationVO {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    UNRECORDED,
    UNKNOWN,
}
