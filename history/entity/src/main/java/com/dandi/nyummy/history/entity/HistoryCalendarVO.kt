package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 하루의 영양 목표 달성 상태입니다. 캘린더 날짜 셀의 상태 마커와 범례에 사용합니다.
 */
@Serializable
enum class DailyNutritionStatus {
    /** 기록 자체가 없는 날 (마커 없음) */
    NONE,

    /** 목표 범위 안 */
    IN_RANGE,

    /** 목표 범위 밖 */
    OUT_OF_RANGE,

    /** 기록은 있으나 영양 판정이 없는 날 */
    NOT_RECORDED,
    ;

    companion object {
        private const val IN_RANGE_MIN_PERCENT = 85
        private const val IN_RANGE_MAX_PERCENT = 105

        /** 하루 누적 열량과 목표 열량으로 목표 범위 상태를 판정합니다. */
        fun of(totalCalorieKcal: Int, targetCalorieKcal: Int, hasRecord: Boolean): DailyNutritionStatus =
            when {
                !hasRecord -> NOT_RECORDED
                targetCalorieKcal <= 0 -> NOT_RECORDED
                totalCalorieKcal * 100 in
                    (targetCalorieKcal * IN_RANGE_MIN_PERCENT)..(targetCalorieKcal * IN_RANGE_MAX_PERCENT)
                -> IN_RANGE

                else -> OUT_OF_RANGE
            }
    }
}

/**
 * 캘린더 한 칸에 해당하는 하루 기록 요약입니다.
 *
 * @property foodIconIds 셀에 노출할 음식 아이콘 식별자, 최대 2개
 * @property mealCount 그날의 식사 기록 수
 */
@Serializable
data class HistoryCalendarDayVO(
    val date: HistoryDateVO = HistoryDateVO(),
    val status: DailyNutritionStatus = DailyNutritionStatus.NONE,
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
