package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryNutrientVO
import kotlinx.serialization.Serializable

/**
 * 히스토리 식사 1건에 포함된 단일 영양 성분 응답.
 *
 * @property nutrientType 영양 성분 코드.
 * @property amount 선택한 음식에 포함된 영양 성분 수치.
 * @property unit 수치 단위. 예: `kcal`, `g`, `mg`.
 */
@Serializable
data class HistoryNutrientDTO(
    val nutrientType: String? = null,
    val amount: Double? = null,
    val unit: String? = null,
)

fun HistoryNutrientDTO.toVO(): HistoryNutrientVO =
    HistoryNutrientVO(
        nutrientType = nutrientType.toHistoryNutrientTypeVO(),
        amount = amount ?: 0.0,
        unit = unit.orEmpty(),
    )
