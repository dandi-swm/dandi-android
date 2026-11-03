package com.dandi.nyummy.widget.data.dto

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.widget.entity.WidgetHomeHintVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import com.dandi.nyummy.widget.entity.WidgetStage
import com.dandi.nyummy.widget.entity.WidgetVariantPickVO
import kotlinx.serialization.Serializable

/**
 * 위젯이 기기 DataStore에 JSON 하나로 남기는 값. 모르는 필드는 버리고, 빠진 필드는 기본값으로 읽는다.
 *
 * @property lastRecordedAtMillis 이 기기에서 식사 접수에 성공한 마지막 순간
 * @property homeFetchedAtMillis 홈 요약을 받은 순간
 * @property homeStreakDays 홈 요약의 연속 기록일
 * @property homeTodayRecordedCount 홈 요약의 오늘 기록 수
 * @property homeBrokenSinceMillis 연속이 끊긴 홈 요약을 처음 받은 순간
 * @property breakfast 저장한 아침 시각
 * @property lunch 저장한 점심 시각
 * @property dinner 저장한 저녁 시각
 * @property reunionRecordedAtMillis 연속이 끊긴 상태에서 기록한 순간
 * @property variantPicks 상태별로 고른 시안
 */
@Serializable
data class WidgetSnapshotDTO(
    val lastRecordedAtMillis: Long? = null,
    val homeFetchedAtMillis: Long? = null,
    val homeStreakDays: Int? = null,
    val homeTodayRecordedCount: Int? = null,
    val homeBrokenSinceMillis: Long? = null,
    val breakfast: WidgetMealTimeDTO? = null,
    val lunch: WidgetMealTimeDTO? = null,
    val dinner: WidgetMealTimeDTO? = null,
    val reunionRecordedAtMillis: Long? = null,
    val variantPicks: List<WidgetVariantPickDTO>? = null,
) {
    fun toVO(): WidgetSnapshotVO = WidgetSnapshotVO(
        lastRecordedAtMillis = lastRecordedAtMillis ?: 0L,
        homeHint = WidgetHomeHintVO(
            fetchedAtMillis = homeFetchedAtMillis ?: 0L,
            streakDays = homeStreakDays ?: 0,
            todayRecordedCount = homeTodayRecordedCount ?: 0,
            brokenSinceMillis = homeBrokenSinceMillis ?: 0L,
        ),
        mealTimes = MealTimesVO(
            breakfast = breakfast.toVO(MealTimesVO.DefaultBreakfast),
            lunch = lunch.toVO(MealTimesVO.DefaultLunch),
            dinner = dinner.toVO(MealTimesVO.DefaultDinner),
        ),
        reunionRecordedAtMillis = reunionRecordedAtMillis ?: 0L,
        variantPicks = variantPicks.orEmpty().mapNotNull { it.toEntry() }.toMap(),
    )

    companion object {
        fun from(vo: WidgetSnapshotVO): WidgetSnapshotDTO = WidgetSnapshotDTO(
            lastRecordedAtMillis = vo.lastRecordedAtMillis,
            homeFetchedAtMillis = vo.homeHint.fetchedAtMillis,
            homeStreakDays = vo.homeHint.streakDays,
            homeTodayRecordedCount = vo.homeHint.todayRecordedCount,
            homeBrokenSinceMillis = vo.homeHint.brokenSinceMillis,
            breakfast = WidgetMealTimeDTO.from(vo.mealTimes.breakfast),
            lunch = WidgetMealTimeDTO.from(vo.mealTimes.lunch),
            dinner = WidgetMealTimeDTO.from(vo.mealTimes.dinner),
            reunionRecordedAtMillis = vo.reunionRecordedAtMillis,
            variantPicks = vo.variantPicks.map { (stage, pick) -> WidgetVariantPickDTO.from(stage, pick) },
        )
    }
}

/**
 * @property hour 끼니 시각(0~23시)
 * @property isSkipped 이 끼니를 건너뛰는지
 */
@Serializable
data class WidgetMealTimeDTO(
    val hour: Int? = null,
    val isSkipped: Boolean? = null,
) {
    companion object {
        fun from(vo: MealTimeVO): WidgetMealTimeDTO = WidgetMealTimeDTO(hour = vo.hour, isSkipped = vo.isSkipped)
    }
}

private fun WidgetMealTimeDTO?.toVO(default: MealTimeVO): MealTimeVO = MealTimeVO(
    hour = this?.hour?.coerceIn(0, 23) ?: default.hour,
    isSkipped = this?.isSkipped ?: default.isSkipped,
)

/**
 * @property stage 상태 이름([WidgetStage])
 * @property episodeKey 고른 회차
 * @property chosen 고른 시안 번호
 * @property previous 직전 회차에 보인 시안 번호
 */
@Serializable
data class WidgetVariantPickDTO(
    val stage: String? = null,
    val episodeKey: String? = null,
    val chosen: Int? = null,
    val previous: Int? = null,
) {
    fun toEntry(): Pair<WidgetStage, WidgetVariantPickVO>? {
        val known = WidgetStage.entries.firstOrNull { it.name == stage } ?: return null
        return known to WidgetVariantPickVO(
            episodeKey = episodeKey.orEmpty(),
            chosen = (chosen ?: 0).coerceAtLeast(0),
            previous = previous ?: -1,
        )
    }

    companion object {
        fun from(stage: WidgetStage, pick: WidgetVariantPickVO): WidgetVariantPickDTO = WidgetVariantPickDTO(
            stage = stage.name,
            episodeKey = pick.episodeKey,
            chosen = pick.chosen,
            previous = pick.previous,
        )
    }
}
