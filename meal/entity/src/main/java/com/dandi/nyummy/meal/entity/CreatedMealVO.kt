package com.dandi.nyummy.meal.entity

/**
 * 식사 생성 직후 서버가 돌려주는 식별자, 분석 상태, 보상 상자 등급입니다.
 *
 * @property mealId 생성된 식사 ID
 * @property status 영양 분석 상태 문자열 (WAITING/ANALYZING/COMPLETED/FAILED/UNKNOWN)
 * @property rewardBoxGrade 이번 기록으로 우편함에 들어간 보상 상자 등급
 */
data class CreatedMealVO(
    val mealId: Long = 0L,
    val status: String = "",
    val rewardBoxGrade: RewardBoxGrade = RewardBoxGrade.NONE,
) {
    /** 응답이 왔지만 영양 분석은 아직 끝나지 않았는지. 분석이 비동기로 바뀌면 기록 직후 이 상태가 된다. */
    val isAnalysisPending: Boolean
        get() = status == STATUS_WAITING || status == STATUS_ANALYZING

    companion object {
        val empty = CreatedMealVO()

        private const val STATUS_WAITING = "WAITING"
        private const val STATUS_ANALYZING = "ANALYZING"
    }
}
