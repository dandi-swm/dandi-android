package com.dandi.nyummy.widget.entity

import com.dandi.nyummy.common.entity.meal.MealTimesVO

/**
 * 위젯이 상태를 계산하려고 기기에 남겨 두는 값. 지난 시간이나 지금 상태는 남기지 않고, 그릴 때마다 계산한다.
 *
 * @property lastRecordedAtMillis 이 기기에서 식사 접수에 성공한 마지막 순간(epoch millis). 0이면 이 기기 기록이 없다.
 * @property homeHint 마지막으로 받은 홈 요약 값. 이 기기 기록이 없을 때 마지막 기록 날짜를 메운다.
 * @property mealTimes 저장한 끼니 시각. 저장한 적 없으면 기본값(8 / 12 / 18시)이다.
 * @property reunionRecordedAtMillis 연속이 끊긴 상태에서 기록한 순간. 이 기록이 마지막 기록인 동안 재회를 보인다. 0이면 없다.
 * @property variantPicks 상태별로 고른 시안. 같은 회차 동안 같은 시안을 보이고, 다음 회차에는 직전 시안을 빼고 고른다.
 */
data class WidgetSnapshotVO(
    val lastRecordedAtMillis: Long = 0L,
    val homeHint: WidgetHomeHintVO = WidgetHomeHintVO(),
    val mealTimes: MealTimesVO = MealTimesVO.default,
    val reunionRecordedAtMillis: Long = 0L,
    val variantPicks: Map<WidgetStage, WidgetVariantPickVO> = emptyMap(),
) {
    companion object {
        val empty = WidgetSnapshotVO()
    }
}

/**
 * 홈 요약(GET /api/v1/home)에서 위젯이 쓰는 값.
 *
 * @property fetchedAtMillis 받은 순간. 0이면 받은 적이 없다.
 * @property streakDays 서버 연속 기록일. 오늘이나 어제까지 이어졌으면 그 일수, 그 전에 끊겼으면 0이다.
 * @property todayRecordedCount 받은 날(서버 기준) 기록한 식사 수. 삭제한 식사는 빠진다.
 */
data class WidgetHomeHintVO(
    val fetchedAtMillis: Long = 0L,
    val streakDays: Int = 0,
    val todayRecordedCount: Int = 0,
) {
    val isEmpty: Boolean get() = fetchedAtMillis <= 0L
}

/**
 * 한 상태에서 고른 시안.
 *
 * @property episodeKey 고른 회차. 같은 키인 동안 [chosen]을 그대로 쓴다.
 * @property chosen 고른 시안 번호(0부터)
 * @property previous 그 직전 회차에 보인 시안 번호. 없으면 -1이다.
 */
data class WidgetVariantPickVO(
    val episodeKey: String = "",
    val chosen: Int = 0,
    val previous: Int = -1,
)
