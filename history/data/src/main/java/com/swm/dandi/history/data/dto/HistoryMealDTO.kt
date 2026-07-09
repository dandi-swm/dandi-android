package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryMealVO
import kotlinx.serialization.Serializable

/**
 * 히스토리 하단 식사 목록의 단일 식사 기록.
 *
 * @property historyId 식사 상세 또는 영양 조회에 사용할 히스토리 기록 id.
 * @property mealType 서버가 먹은 시간에서 파생한 식사 구간 코드.
 * @property displayName 화면에 표시할 음식명.
 * @property foodImageUrl 대표 음식 아이콘 이미지 URL.
 * @property nutrients 선택한 음식 자체의 영양 성분 원자료 목록.
 */
@Serializable
data class HistoryMealDTO(
    val historyId: String? = null,
    val mealType: String? = null,
    val displayName: String? = null,
    val foodImageUrl: String? = null,
    val nutrients: List<HistoryNutrientDTO>? = null,
)

fun HistoryMealDTO.toVO(): HistoryMealVO =
    HistoryMealVO(
        historyId = historyId.orEmpty(),
        mealType = mealType.toHistoryMealTypeVO(),
        displayName = displayName.orEmpty(),
        foodImageUrl = foodImageUrl.orEmpty(),
        nutrients = nutrients.orEmpty().map { it.toVO() },
    )
