package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.toMealAnalysisStatus
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import kotlinx.serialization.Serializable

/**
 * POST /api/v1/meals/{mealId}/analysis 응답 (재분석 요청 뒤의 분석 상태).
 *
 * @property id 식사 기록 식별자
 * @property status 영양 분석 상태
 */
@Serializable
data class MealStatusDTO(
    val id: Long? = null,
    val status: String? = null,
) {
    fun toVO(): MealAnalysisStatus = status.toMealAnalysisStatus()
}
