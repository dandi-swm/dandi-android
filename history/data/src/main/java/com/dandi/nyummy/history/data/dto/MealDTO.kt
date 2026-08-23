package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.data.util.toDisplayTime
import com.dandi.nyummy.history.entity.MealHistoryVO
import kotlinx.serialization.Serializable

/**
 * GET · PUT /api/v1/meals/{mealId} 응답 (식사 단건 상세).
 *
 * @property mealId 식사 기록 식별자
 * @property name 음식 이름
 * @property mealAt 식사 시각 (ISO date-time)
 * @property status 영양 분석 상태 (WAITING / ANALYZING / COMPLETED / FAILED / UNKNOWN)
 * @property nutrition 이 식사의 영양 정보
 * @property imageUrl 촬영 사진 URL
 */
@Serializable
data class MealDTO(
    val mealId: Long? = null,
    val name: String? = null,
    val mealAt: String? = null,
    val status: String? = null,
    val nutrition: NutritionDTO? = null,
    val imageUrl: String? = null,
) {
    // 단건 조회에는 하루 내 순서 정보가 없으므로 orderIndex 는 0.
    fun toVO(): MealHistoryVO = MealHistoryVO(
        id = mealId?.toString() ?: "",
        name = name ?: "",
        photoUrl = imageUrl ?: "",
        foodIconId = "",
        recordedAt = mealAt.toDisplayTime(),
        calorieKcal = nutrition?.calory ?: 0,
        carbohydrateGram = nutrition?.carbs ?: 0,
        proteinGram = nutrition?.protein ?: 0,
        fatGram = nutrition?.fat ?: 0,
        orderIndex = 0,
    )
}
