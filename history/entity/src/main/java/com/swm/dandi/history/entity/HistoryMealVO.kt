package com.swm.dandi.history.entity

/**
 * 히스토리 하단 식사 목록의 단일 식사 기록.
 *
 * 화면 표시용 라벨(`아침`, `점심` 등)은 `mealType`을 presentation 레이어에서 변환해 만든다.
 *
 * @property historyId 식사 상세 또는 영양 조회에 사용할 히스토리 기록 id.
 * @property mealType 먹은 시간에서 파생된 식사 구간.
 * @property displayName 사용자에게 표시할 음식명.
 * @property foodImageUrl 대표 음식 아이콘 이미지 URL.
 * @property nutrients 선택한 음식 자체의 영양 성분 목록.
 */
data class HistoryMealVO(
    val historyId: String = "",
    val mealType: HistoryMealTypeVO = HistoryMealTypeVO.UNKNOWN,
    val displayName: String = "",
    val foodImageUrl: String = "",
    val nutrients: List<HistoryNutrientVO> = emptyList(),
) {
    companion object {
        val empty: HistoryMealVO = HistoryMealVO()
    }
}
