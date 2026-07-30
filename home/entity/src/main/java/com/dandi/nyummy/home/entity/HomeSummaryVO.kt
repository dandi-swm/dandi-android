package com.dandi.nyummy.home.entity

import kotlinx.serialization.Serializable

/**
 * 홈 화면 상단 HUD 와 마이룸 카드에 표시되는 요약 정보입니다.
 *
 * @property coinBalance 보유 코인 수량
 * @property streakDays 연속으로 냐미에게 밥을 챙긴 일수
 * @property recordsUntilNextReward 다음 보상 상자까지 남은 기록 횟수
 * @property todayRecordedCount 오늘 기록한 식사 수
 * @property todayCalorieKcal 오늘 섭취한 열량(kcal)
 * @property goalCalorieKcal 하루 목표 열량(kcal)
 * @property hasUnreadNotice 읽지 않은 공지가 있는지 여부 (공지 버튼 알림 점 표시용)
 * @property speechTitle 냐미 말풍선 첫 줄 (예: "첫 기록을 기다려요")
 * @property speechBody 냐미 말풍선 본문 (예: "첫 끼를 기록하면 같이 챙겨볼게!")
 */
@Serializable
data class HomeSummaryVO(
    val coinBalance: Int = 0,
    val streakDays: Int = 0,
    val recordsUntilNextReward: Int = 0,
    val todayRecordedCount: Int = 0,
    val todayCalorieKcal: Int = 0,
    val goalCalorieKcal: Int = 0,
    val hasUnreadNotice: Boolean = false,
    val speechTitle: String = "",
    val speechBody: String = "",
) {

    companion object {
        val empty = HomeSummaryVO()
    }
}
