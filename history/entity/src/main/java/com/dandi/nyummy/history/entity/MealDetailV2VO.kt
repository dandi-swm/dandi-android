package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 음식 목록 방식(V2) 식사 상세입니다.
 *
 * @property meal 식사 정보(이름, 사진, 시각, 상태, 영양 합계, 냐미 한마디, 분석 버전)
 * @property foods 식사에 든 음식 줄. 맨 위가 대표 음식입니다. V1 식사이거나 음식을 아직 찾지 못했으면 비어 있습니다.
 */
@Serializable
data class MealDetailV2VO(
    val meal: MealHistoryVO = MealHistoryVO.empty,
    val foods: List<MealFoodVO> = emptyList(),
) {
    companion object {
        val empty = MealDetailV2VO()
    }
}
