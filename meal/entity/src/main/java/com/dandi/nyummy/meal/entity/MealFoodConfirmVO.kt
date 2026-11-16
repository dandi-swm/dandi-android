package com.dandi.nyummy.meal.entity

/**
 * 이미 기록한 식사에서 사용자가 확인할 찾은 음식 목록입니다.
 *
 * @property isConfirmable 아직 사용자의 확인을 기다리는 식사인지 여부. 서버가 이미 영양 계산을 시작했으면 false입니다.
 * @property foods 서버가 찾은 음식 목록
 */
data class MealFoodConfirmVO(
    val isConfirmable: Boolean = false,
    val foods: List<FoundFoodVO> = emptyList(),
) {
    companion object {
        val empty = MealFoodConfirmVO()
    }
}
