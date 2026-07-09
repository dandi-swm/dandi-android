package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryMealsVO
import kotlinx.serialization.Serializable

/**
 * 선택한 날짜의 식사 목록 조회 응답.
 *
 * 날짜별 상세 목록만 담고, 식사 구간 라벨이나 상태 문구는 presentation 레이어에서 만든다.
 */
@Serializable
data class HistoryMealsResponseDTO(
    val date: String? = null,
    val meals: List<HistoryMealDTO>? = null,
)

fun HistoryMealsResponseDTO.toVO(): HistoryMealsVO =
    HistoryMealsVO(
        date = date.orEmpty(),
        meals = meals.orEmpty().map { it.toVO() },
    )
