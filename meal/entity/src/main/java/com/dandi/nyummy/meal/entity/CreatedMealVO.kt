package com.dandi.nyummy.meal.entity

/**
 * 식사 생성 직후 서버가 돌려주는 식별자, 분석 상태, 보상 상자 등급입니다.
 *
 * 응답이 왔다는 것은 기록이 접수됐다는 뜻입니다. 영양 분석은 접수 뒤에 따로 진행되므로 [status] 는 늘 처리 중이고,
 * 기록하지 못한 경우는 이 값이 아니라 에러 코드로 옵니다.
 *
 * @property mealId 생성된 식사 ID
 * @property status 영양 분석 상태 문자열 (WAITING/ANALYZING)
 * @property rewardBoxGrade 이번 기록으로 우편함에 들어간 보상 상자 등급
 */
data class CreatedMealVO(
    val mealId: Long = 0L,
    val status: String = "",
    val rewardBoxGrade: RewardBoxGrade = RewardBoxGrade.NONE,
) {
    companion object {
        val empty = CreatedMealVO()
    }
}
