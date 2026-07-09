package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryDayVO
import kotlinx.serialization.Serializable

/**
 * 캘린더 한 칸에 표시할 날짜별 기록 요약.
 *
 * @property date 서버 기준 날짜 문자열. 예: `2025-07-03`.
 * @property dayOfMonth 캘린더 셀에 표시할 일자 숫자.
 * @property isCurrentMonth 현재 조회 월에 속하는 날짜인지 여부.
 * @property dailyNutritionEvaluation 날짜별 일일 영양 평가 코드.
 * @property foodImageUrls 캘린더 셀에 요약 표시할 대표 음식 아이콘 이미지 URL 목록.
 */
@Serializable
data class HistoryDayDTO(
    val date: String? = null,
    val dayOfMonth: Int? = null,
    val isCurrentMonth: Boolean? = null,
    val dailyNutritionEvaluation: String? = null,
    val foodImageUrls: List<String>? = null,
)

fun HistoryDayDTO.toVO(): HistoryDayVO =
    HistoryDayVO(
        date = date.orEmpty(),
        dayOfMonth = dayOfMonth ?: 0,
        isCurrentMonth = isCurrentMonth ?: true,
        dailyNutritionEvaluation = dailyNutritionEvaluation.toHistoryDailyNutritionEvaluationVO(),
        foodImageUrls = foodImageUrls.orEmpty(),
    )
