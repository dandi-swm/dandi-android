package com.dandi.nyummy.home.entity

import kotlinx.serialization.Serializable

/**
 * 홈 화면 상단 HUD와 오늘 바에 표시되는 요약 정보(GET /api/v1/home).
 *
 * @property coinBalance 보유 코인 수량
 * @property streakDays 연속으로 냐미에게 밥을 챙긴 일수
 * @property recordsUntilNextReward 다음 보상까지 남은 기록 횟수
 * @property todayRecordedCount 오늘 기록한 식사 수
 * @property todayCalorieKcal 오늘 섭취한 열량(kcal)
 * @property goalCalorieKcal 하루 목표 열량(kcal)
 */
@Serializable
data class HomeSummaryVO(
    val coinBalance: Int = 0,
    val streakDays: Int = 0,
    val recordsUntilNextReward: Int = 0,
    val todayRecordedCount: Int = 0,
    val todayCalorieKcal: Int = 0,
    val goalCalorieKcal: Int = 0,
) {

    companion object {
        val empty = HomeSummaryVO()
    }
}
