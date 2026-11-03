package com.dandi.nyummy.widget.entity

import com.dandi.nyummy.common.entity.meal.Meal

/**
 * 지금 위젯에 보일 상태를 계산한 결과.
 *
 * @property stage 지금 상태
 * @property meal 문구에 넣을 끼니. 밤·기다림은 다음 끼니, 한 끼 놓침은 놓친 끼니다. 다른 상태에서는 쓰지 않는다.
 * @property isMealTimeReached 기다림 중 그 끼니 시각이 지났는지. "점심까지 1시간"을 "점심 시간이에요"로 바꾼다.
 * @property awayDays 일 단위 상태의 거른 날 수(1 이상). 다른 상태에서는 0이다.
 * @property isAwayDaysExact [awayDays]가 정확한 값인지. false면 아는 최솟값이라 문구에 날수를 넣지 않는다.
 * @property streakDays 스트릭 줄에 보일 연속 기록일
 * @property isStreakLit 오늘 기록했는지. true면 스트릭 줄을 흰색으로, 아니면 반투명하게 그린다.
 * @property episodeKey 같은 상태가 이어지는 동안 같은 값. 바뀌면 시안을 새로 고른다.
 * @property nextChangeAtMillis 다음에 다시 그려야 하는 순간(epoch millis)
 */
data class WidgetPlanVO(
    val stage: WidgetStage = WidgetStage.LOGGED_OUT,
    val meal: Meal = Meal.BREAKFAST,
    val isMealTimeReached: Boolean = false,
    val awayDays: Int = 0,
    val isAwayDaysExact: Boolean = true,
    val streakDays: Int = 0,
    val isStreakLit: Boolean = false,
    val episodeKey: String = "",
    val nextChangeAtMillis: Long = 0L,
)

/**
 * 위젯에 그릴 카드 한 장.
 *
 * @property plan 계산한 상태
 * @property variant 그 상태의 시안 중 고른 번호(0부터)
 */
data class WidgetCardVO(
    val plan: WidgetPlanVO = WidgetPlanVO(),
    val variant: Int = 0,
)
