package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.NutrientAmountVO
import com.swm.dandi.meal.entity.NutritionAnalysisVO
import kotlinx.serialization.Serializable

/**
 * 식사 1건에 대한 영양 분석 조회 응답.
 *
 * 화면 문구는 포함하지 않는다. `analysisStatus`, `nutrientType` 같은 코드값과 수치 원자료만 받는다.
 */
@Serializable
data class NutritionAnalysisResponseDTO(
    val mealRecordId: String? = null,
    val analysisStatus: String? = null,
    val nutrients: List<NutrientAmountDTO>? = null,
    val analyzedAt: String? = null,
    val retryable: Boolean? = null,
)

/**
 * 분석된 영양소별 절대 수치.
 */
@Serializable
data class NutrientAmountDTO(
    val nutrientType: String? = null,
    val amount: Double? = null,
    val unit: String? = null,
)

fun NutritionAnalysisResponseDTO.toVO(): NutritionAnalysisVO =
    NutritionAnalysisVO(
        mealRecordId = mealRecordId.orEmpty(),
        analysisStatus = analysisStatus.toMealAnalysisStatusVO(),
        nutrients = nutrients.orEmpty().map { it.toVO() },
        analyzedAt = analyzedAt.orEmpty(),
        retryable = retryable ?: false,
    )

fun NutrientAmountDTO.toVO(): NutrientAmountVO =
    NutrientAmountVO(
        nutrientType = nutrientType.toNutrientTypeVO(),
        amount = amount ?: 0.0,
        unit = unit.orEmpty(),
    )
