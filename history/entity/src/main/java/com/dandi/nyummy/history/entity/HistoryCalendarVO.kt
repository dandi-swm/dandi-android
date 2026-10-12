package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 서버가 판정한 하루 영양 평가입니다(서버 enum `DailyNutritionEvaluation`과 같은 값).
 * 앱은 판정하지 않고 서버 값을 그대로 씁니다.
 */
@Serializable
enum class DailyNutritionEvaluation {
    /** 목표에 맞게 먹은 날 */
    POSITIVE,

    /** 목표에서 벗어난 날 */
    NEGATIVE,

    /** 기록이 없는 날 */
    UNRECORDED,

    /** 서버 값이 없거나 앱이 모르는 값 */
    UNKNOWN,
}

/**
 * 캘린더 한 칸에 해당하는 하루 기록 요약입니다.
 *
 * @property evaluation 서버가 판정한 하루 영양 평가
 * @property foodIconIds 셀에 노출할 음식 아이콘 식별자, 최대 2개
 * @property mealCount 그날의 식사 기록 수
 */
@Serializable
data class HistoryCalendarDayVO(
    val date: HistoryDateVO = HistoryDateVO(),
    val evaluation: DailyNutritionEvaluation = DailyNutritionEvaluation.UNKNOWN,
    val foodIconIds: List<String> = emptyList(),
    val mealCount: Int = 0,
)

/**
 * 한 달치 히스토리 캘린더 데이터입니다.
 *
 * @property days 기록이 있는 날만 담습니다. 빈 날짜 채움은 presentation 의 그리드 계산이 담당합니다.
 */
@Serializable
data class HistoryCalendarVO(
    val year: Int = 0,
    val month: Int = 0,
    val days: List<HistoryCalendarDayVO> = emptyList(),
) {

    companion object {
        val empty = HistoryCalendarVO()
    }
}
