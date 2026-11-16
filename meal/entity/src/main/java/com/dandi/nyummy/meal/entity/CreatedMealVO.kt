package com.dandi.nyummy.meal.entity

/**
 * 식사 생성 직후 서버가 돌려주는 식별자, 음식 인식 결과, 보상 상자 등급입니다.
 *
 * 응답이 왔다는 것은 기록이 접수됐다는 뜻입니다. 음식이 아니라고 판정돼도 기록 횟수에 들어가고 상자가 생깁니다.
 * 기록하지 못한 경우는 이 값이 아니라 에러 코드로 옵니다.
 *
 * @property mealId 생성된 식사 ID
 * @property recognition 사진에서 음식을 찾은 결과
 * @property rewardBoxGrade 이번 기록으로 우편함에 들어간 보상 상자 등급
 * @property name 서버가 지은 식사 이름. 음식을 찾았을 때만 있습니다.
 * @property foods 서버가 찾은 음식 목록. 맨 위 줄이 대표 음식이고, 음식을 찾았을 때만 있습니다.
 */
data class CreatedMealVO(
    val mealId: Long = 0L,
    val recognition: MealRecognition = MealRecognition.RECOGNIZING,
    val rewardBoxGrade: RewardBoxGrade = RewardBoxGrade.NONE,
    val name: String = "",
    val foods: List<FoundFoodVO> = emptyList(),
) {
    companion object {
        val empty = CreatedMealVO()
    }
}
