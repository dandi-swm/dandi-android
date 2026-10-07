package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/home 응답.
 *
 * @property user 사용자 지갑 정보
 * @property streak 연속 기록 현황
 * @property todayMealSummary 오늘의 식사 현황
 */
@Serializable
data class HomeDTO(
    val user: HomeUserDTO? = null,
    val streak: HomeStreakDTO? = null,
    val todayMealSummary: TodayMealSummaryDTO? = null,
) {
    fun toVO(): HomeSummaryVO = HomeSummaryVO(
        coinBalance = user?.coin ?: 0,
        streakDays = streak?.streakDays ?: 0,
        recordsUntilNextReward = streak?.recordsUntilNextReward ?: 0,
        todayRecordedCount = todayMealSummary?.todayRecordedCount ?: 0,
        todayCalorieKcal = todayMealSummary?.todayCurrentCalory ?: 0,
        goalCalorieKcal = todayMealSummary?.todayTargetCalory ?: 0,
    )
}

/** @property coin 보유 코인 */
@Serializable
data class HomeUserDTO(
    val coin: Int? = null,
)

/**
 * @property streakDays 연속으로 기록한 일수
 * @property recordsUntilNextReward 다음 보상까지 남은 기록 횟수
 */
@Serializable
data class HomeStreakDTO(
    val streakDays: Int? = null,
    val recordsUntilNextReward: Int? = null,
)

/**
 * 서버 필드명은 calorie가 아니라 calory다.
 *
 * @property todayRecordedCount 오늘 기록한 끼니 수
 * @property todayCurrentCalory 오늘 섭취 열량(kcal)
 * @property todayTargetCalory 하루 목표 열량(kcal)
 */
@Serializable
data class TodayMealSummaryDTO(
    val todayRecordedCount: Int? = null,
    val todayCurrentCalory: Int? = null,
    val todayTargetCalory: Int? = null,
)
