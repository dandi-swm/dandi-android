package com.dandi.nyummy.widget.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetStage

/**
 * debug 빌드 전용: 위젯에 원하는 상태·시안을 고정해 그린다. 피그마 시안과 한 장씩 비교할 때 쓴다.
 * 다음 예약 알람이나 앱의 갱신 요청이 오면 원래 계산한 카드로 돌아간다(REFRESH로 바로 되돌릴 수도 있다).
 *
 * adb shell am broadcast -n <applicationId>/com.dandi.nyummy.widget.presentation.NyummyWidgetDebugReceiver \
 *   -a com.dandi.nyummy.widget.debug.SHOW --es stage WAITING --ei variant 2 --es meal LUNCH \
 *   [--ei days 3] [--ez exact true] [--ei streak 12] [--ez lit true] [--ez mealTimeReached false]
 */
class NyummyWidgetDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return
        val ids = NyummyWidgetReceiver.widgetIds(context)
        if (ids.isEmpty()) return
        val stage = WidgetStage.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_STAGE) } ?: WidgetStage.WAITING
        val meal = Meal.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_MEAL) } ?: Meal.LUNCH
        val plan = WidgetPlanVO(
            stage = stage,
            meal = meal,
            isMealTimeReached = intent.getBooleanExtra(EXTRA_MEAL_TIME_REACHED, false),
            awayDays = intent.getIntExtra(EXTRA_DAYS, 1),
            isAwayDaysExact = intent.getBooleanExtra(EXTRA_EXACT, true),
            streakDays = intent.getIntExtra(EXTRA_STREAK, if (stage == WidgetStage.AWAY) 0 else 12),
            isStreakLit = intent.getBooleanExtra(EXTRA_LIT, stage in LIT_BY_DEFAULT),
        )
        NyummyWidgetReceiver.show(context, ids, WidgetCardVO(plan = plan, variant = intent.getIntExtra(EXTRA_VARIANT, 0)))
    }

    private companion object {
        const val ACTION_SHOW = "com.dandi.nyummy.widget.debug.SHOW"
        const val EXTRA_STAGE = "stage"
        const val EXTRA_VARIANT = "variant"
        const val EXTRA_MEAL = "meal"
        const val EXTRA_DAYS = "days"
        const val EXTRA_EXACT = "exact"
        const val EXTRA_STREAK = "streak"
        const val EXTRA_LIT = "lit"
        const val EXTRA_MEAL_TIME_REACHED = "mealTimeReached"

        val LIT_BY_DEFAULT = setOf(WidgetStage.WAITING, WidgetStage.FULL, WidgetStage.MISSED_ONE, WidgetStage.REUNION)
    }
}
