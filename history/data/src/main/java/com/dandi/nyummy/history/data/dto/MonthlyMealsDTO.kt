package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.toHistoryDateVO
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/meals/monthly 응답.
 *
 * @property year 조회 연도
 * @property month 조회 월 (1~12)
 * @property days 캘린더 범위의 날짜별 요약 (앞뒤 달 날짜 포함)
 */
@Serializable
data class MonthlyMealsDTO(
    val year: Int? = null,
    val month: Int? = null,
    val days: List<MonthlyMealDayDTO>? = null,
) {
    fun toVO(): HistoryCalendarVO = HistoryCalendarVO(
        year = year ?: 0,
        month = month ?: 0,
        // 엔티티 계약상 days 는 현재 월의 기록만 담는다. 인접 월 채움은 presentation 그리드가 담당한다.
        days = days.orEmpty()
            .filter { it.isCurrentMonth == true }
            .map { it.toVO() },
    )
}

/**
 * 월간 캘린더의 하루 요약.
 *
 * @property date 날짜 (yyyy-MM-dd)
 * @property dailyNutritionEvaluation 하루 영양 평가 (POSITIVE / NEGATIVE / UNRECORDED)
 * @property foodIconIds 셀에 노출할 음식 아이콘 식별자 목록
 * @property isCurrentMonth 조회 월에 속하는 날짜인지 여부 (서버 JSON 키: isCurrentMonth)
 */
@Serializable
data class MonthlyMealDayDTO(
    val date: String? = null,
    val dailyNutritionEvaluation: String? = null,
    val foodIconIds: List<Long>? = null,
    val isCurrentMonth: Boolean? = null,
) {
    fun toVO(): HistoryCalendarDayVO {
        // VO 계약상 셀 아이콘은 최대 2개.
        val iconIds = foodIconIds.orEmpty().map { it.toString() }.take(2)
        return HistoryCalendarDayVO(
            date = date.toHistoryDateVO(),
            status = dailyNutritionEvaluation.toDailyNutritionStatus(),
            foodIconIds = iconIds,
            // 월간 API 는 식사 수를 내려주지 않으므로 아이콘 수로 대체한다(캘린더 그리드는 미사용).
            mealCount = iconIds.size,
        )
    }
}

/** 서버의 하루 영양 평가 문자열을 [DailyNutritionStatus] 로 매핑한다. */
private fun String?.toDailyNutritionStatus(): DailyNutritionStatus = when (this) {
    "POSITIVE" -> DailyNutritionStatus.IN_RANGE
    "NEGATIVE" -> DailyNutritionStatus.OUT_OF_RANGE
    "UNRECORDED" -> DailyNutritionStatus.NOT_RECORDED
    else -> DailyNutritionStatus.NONE
}
