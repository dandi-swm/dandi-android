package com.dandi.nyummy.widget.data

import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.widget.domain.WidgetPlanner
import com.dandi.nyummy.widget.domain.WidgetRefresher
import com.dandi.nyummy.widget.domain.WidgetRepository
import com.dandi.nyummy.widget.entity.WidgetHomeHintVO
import com.dandi.nyummy.widget.entity.WidgetStage
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/**
 * 앱이 넘긴 값을 위젯 스냅샷에 남기고 위젯을 다시 그린다.
 * 저장에 실패하면 조용히 넘어간다. 위젯 갱신은 덤이라 식사 기록이나 로그인 흐름을 막지 않는다.
 */
class WidgetHelperImpl(
    private val repository: WidgetRepository,
    private val refresher: WidgetRefresher,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = DeviceTime::zone,
) : WidgetHelper {

    override suspend fun onMealRecorded() = change {
        val now = nowMillis()
        val currentZone = zone()
        repository.update { snapshot ->
            // 기록 직전 위젯이 일 단위(연속 끊김)였으면 이 기록은 재회다.
            val before = WidgetPlanner.plan(snapshot, isLoggedIn = true, nowMillis = now, zone = currentZone)
            snapshot.copy(
                lastRecordedAtMillis = maxOf(snapshot.lastRecordedAtMillis, now),
                reunionRecordedAtMillis = if (before.stage == WidgetStage.AWAY) now else snapshot.reunionRecordedAtMillis,
            )
        }
    }

    override suspend fun onHomeSummary(streakDays: Int, todayRecordedCount: Int) = change {
        val now = nowMillis()
        repository.update { snapshot ->
            val hint = WidgetHomeHintVO(
                fetchedAtMillis = now,
                streakDays = streakDays.coerceAtLeast(0),
                todayRecordedCount = todayRecordedCount.coerceAtLeast(0),
            )
            // 끊긴 요약을 이어 받으면 처음 끊긴 순간을 지킨다. 매번 지금으로 덮으면 일 단위 날수가 1에 멈춘다.
            val brokenSince = when {
                !hint.isBroken -> 0L
                snapshot.homeHint.brokenSinceMillis > 0L -> snapshot.homeHint.brokenSinceMillis
                else -> now
            }
            snapshot.copy(homeHint = hint.copy(brokenSinceMillis = brokenSince))
        }
    }

    override suspend fun onMealTimesSaved(mealTimes: MealTimesVO) = change {
        repository.update { it.copy(mealTimes = mealTimes) }
    }

    override suspend fun clear() = change {
        repository.clear()
    }

    private suspend fun change(block: suspend () -> Unit) {
        try {
            block()
            refresher.requestRefresh()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 위젯 저장·갱신 실패는 본래 흐름에 영향을 주지 않는다.
        }
    }
}
