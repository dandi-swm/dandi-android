package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.MealSessionStatusVO
import com.swm.dandi.meal.entity.MealStatusSheetVO
import com.swm.dandi.meal.entity.NutrientRatioVO
import com.swm.dandi.meal.entity.NutritionSummaryVO
import kotlinx.serialization.Serializable

/**
 * 식사 상태 바텀시트 조회 응답.
 *
 * 화면 문구는 포함하지 않는다. `mealType`, `status`, `nutrientType` 같은 코드값과 수치 원자료만 받는다.
 */
@Serializable
data class MealStatusSheetResponseDTO(
    val mealSessions: List<MealSessionStatusDTO>? = null,
    val nutritionSummary: NutritionSummaryDTO? = null,
)

/**
 * 아침/점심/저녁/간식 중 한 식사 구간의 기록 또는 분석 상태.
 *
 * @property mealType 서버가 먹은 시간에서 파생한 식사 구간 코드.
 * @property status 기록/분석 상태 코드.
 * @property mealRecordId 기록 또는 분석 상태를 추적할 식사 기록 id.
 * @property displayName 기록된 음식명이 있을 때만 내려오는 음식 표시명.
 */
@Serializable
data class MealSessionStatusDTO(
    val mealType: String? = null,
    val status: String? = null,
    val mealRecordId: String? = null,
    val displayName: String? = null,
)

/**
 * 오늘 누적 영양 요약.
 */
@Serializable
data class NutritionSummaryDTO(
    val nutrients: List<NutrientRatioDTO>? = null,
)

/**
 * 영양소별 현재 섭취량과 목표량.
 *
 * `ratio`가 있으므로 `percentLabel`은 받지 않는다.
 */
@Serializable
data class NutrientRatioDTO(
    val nutrientType: String? = null,
    val currentAmount: Double? = null,
    val targetAmount: Double? = null,
    val unit: String? = null,
    val ratio: Float? = null,
)

fun MealStatusSheetResponseDTO.toVO(): MealStatusSheetVO = MealStatusSheetVO(
    mealSessions = mealSessions.orEmpty().map { it.toVO() },
    nutritionSummary = nutritionSummary?.toVO() ?: NutritionSummaryVO.empty,
)

fun MealSessionStatusDTO.toVO(): MealSessionStatusVO = MealSessionStatusVO(
    mealType = mealType.toMealTypeVO(),
    status = status.toMealSessionStatusTypeVO(),
    mealRecordId = mealRecordId.orEmpty(),
    displayName = displayName.orEmpty(),
)

fun NutritionSummaryDTO.toVO(): NutritionSummaryVO = NutritionSummaryVO(
    nutrients = nutrients.orEmpty().map { it.toVO() },
)

fun NutrientRatioDTO.toVO(): NutrientRatioVO = NutrientRatioVO(
    nutrientType = nutrientType.toNutrientTypeVO(),
    currentAmount = currentAmount ?: 0.0,
    targetAmount = targetAmount ?: 0.0,
    unit = unit.orEmpty(),
    ratio = ratio ?: 0f,
)
