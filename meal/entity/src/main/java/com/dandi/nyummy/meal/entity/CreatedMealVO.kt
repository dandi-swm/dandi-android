package com.dandi.nyummy.meal.entity

/**
 * 식사 생성 직후 서버가 돌려주는 식별자와 분석 상태입니다.
 *
 * @property mealId 생성된 식사 ID
 * @property status 영양 분석 상태 문자열 (WAITING/ANALYZING/COMPLETED/FAILED/UNKNOWN)
 */
data class CreatedMealVO(
    val mealId: Long = 0L,
    val status: String = "",
) {
    companion object {
        val empty = CreatedMealVO()
    }
}
