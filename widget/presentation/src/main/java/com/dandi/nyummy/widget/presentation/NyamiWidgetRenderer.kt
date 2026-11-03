package com.dandi.nyummy.widget.presentation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dandi.nyummy.common.domain.navigation.AppLinks
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetStage

/** 정해진 카드 한 장을 위젯 RemoteViews로 그린다. */
internal object NyamiWidgetRenderer {

    // 오늘 아직 기록하지 않았으면 스트릭 줄을 45%로 흐리게 그린다.
    private const val STREAK_ALPHA_LIT = 255
    private const val STREAK_ALPHA_DIM = 115

    private const val REQUEST_OPEN_HOME = 0
    private const val REQUEST_OPEN_APP = 1

    fun render(context: Context, card: WidgetCardVO): RemoteViews {
        val plan = card.plan
        val spec = NyamiWidgetCatalog.cardFor(card)
        val title = context.getString(spec.title, mealName(context, plan.meal))
        val sub = subText(context, spec, plan)

        return RemoteViews(context.packageName, R.layout.widget_nyami).apply {
            setInt(R.id.widget_card, "setColorFilter", ContextCompat.getColor(context, spec.background))
            setImageViewResource(R.id.widget_character, spec.image)
            setImageViewBitmap(R.id.widget_copy, NyamiWidgetTextPainter.copy(context, title, sub))

            if (plan.stage == WidgetStage.LOGGED_OUT) {
                setViewVisibility(R.id.widget_streak, View.GONE)
                setContentDescription(android.R.id.background, "$title. $sub")
            } else {
                val streak = context.getString(R.string.widget_streak_days, plan.streakDays)
                val alpha = if (plan.isStreakLit) STREAK_ALPHA_LIT else STREAK_ALPHA_DIM
                setViewVisibility(R.id.widget_streak, View.VISIBLE)
                setImageViewBitmap(R.id.widget_streak_text, NyamiWidgetTextPainter.streak(context, streak))
                setInt(R.id.widget_streak_icon, "setImageAlpha", alpha)
                setInt(R.id.widget_streak_text, "setImageAlpha", alpha)
                setContentDescription(android.R.id.background, "$title. $sub. $streak")
            }
            setOnClickPendingIntent(android.R.id.background, openIntent(context, plan.stage))
        }
    }

    /** 설명 문구. 기다림 중 끼니 시각이 지나면 바꿔 쓰고, 일 단위는 날수를 알 때만 "N일째 "를 붙인다. */
    private fun subText(context: Context, spec: NyamiWidgetCard, plan: WidgetPlanVO): String {
        val res = if (plan.isMealTimeReached && spec.subAfterMealTime != 0) spec.subAfterMealTime else spec.sub
        val rest = context.getString(res, mealName(context, plan.meal))
        val withDays = plan.stage == WidgetStage.AWAY && spec.showsAwayDays && plan.isAwayDaysExact
        return if (withDays) context.getString(R.string.widget_away_days_prefix, plan.awayDays, rest) else rest
    }

    private fun mealName(context: Context, meal: Meal): String = context.getString(
        when (meal) {
            Meal.BREAKFAST -> R.string.widget_meal_breakfast
            Meal.LUNCH -> R.string.widget_meal_lunch
            Meal.DINNER -> R.string.widget_meal_dinner
        },
    )

    /** 누르면 홈으로 간다. 로그아웃 카드면 앱 첫 화면(인트로)을 연다. */
    private fun openIntent(context: Context, stage: WidgetStage): PendingIntent {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        if (stage == WidgetStage.LOGGED_OUT) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
            return PendingIntent.getActivity(context, REQUEST_OPEN_APP, launch, flags)
        }
        val home = Intent(Intent.ACTION_VIEW, (AppLinks.BASE_URL + HomePage.PATH).toUri())
            .setPackage(context.packageName)
        return PendingIntent.getActivity(context, REQUEST_OPEN_HOME, home, flags)
    }
}
