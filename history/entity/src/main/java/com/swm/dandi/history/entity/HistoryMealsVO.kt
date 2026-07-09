package com.swm.dandi.history.entity

/**
 * 선택한 날짜의 식사 목록 조회 결과.
 *
 * @property date 조회 대상 날짜.
 * @property meals 해당 날짜에 기록된 식사 목록. 빈 날짜면 빈 리스트다.
 */
data class HistoryMealsVO(
    val date: String = "",
    val meals: List<HistoryMealVO> = emptyList(),
) {
    companion object {
        val empty: HistoryMealsVO = HistoryMealsVO()
    }
}
