package com.dandi.nyummy.widget.presentation

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetStage

/**
 * 위젯 시안 한 장.
 *
 * @property image 냐미 그림
 * @property background 카드 배경색
 * @property title 제목. 끼니 이름(%1$s)이 들어갈 수 있다.
 * @property sub 설명. 끼니 이름(%1$s)이 들어갈 수 있다. 일 단위는 "N일째 "를 뺀 나머지다.
 * @property subAfterMealTime 기다림 중 끼니 시각이 지나면 대신 쓰는 설명. 0이면 [sub]를 그대로 쓴다.
 * @property showsAwayDays 일 단위에서 날수를 알면 설명 앞에 "N일째 "를 붙이는지
 */
internal data class NyamiWidgetCard(
    @DrawableRes val image: Int,
    @ColorRes val background: Int,
    @StringRes val title: Int,
    @StringRes val sub: Int,
    @StringRes val subAfterMealTime: Int = 0,
    val showsAwayDays: Boolean = false,
)

/**
 * 상태별 위젯 시안 목록. 피그마 `v2 · 60 Widget` > `Widget · 20 사용 시안 · 상태별`과 같은 순서다.
 * 톤 확인이 필요했던 2장("이거 떨어뜨린다?", "흥, 다 엎었어")은 위젯에서 뺐다.
 */
internal object NyamiWidgetCatalog {

    /** 일 단위는 7일부터 같은 시안(망부석)이다. */
    const val AWAY_DAYS_MAX = 7

    private val night = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_night_a, R.color.widget_bg_dawn, R.string.widget_night_a_title, R.string.widget_night_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_night_b, R.color.widget_bg_dawn, R.string.widget_night_b_title, R.string.widget_night_b_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_night_c, R.color.widget_bg_dawn, R.string.widget_night_c_title, R.string.widget_night_c_sub),
    )

    private val waiting = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_wait_a, R.color.widget_bg_sky, R.string.widget_wait_a_title, R.string.widget_wait_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_wait_b, R.color.widget_bg_sky, R.string.widget_wait_b_title, R.string.widget_wait_b_sub),
        NyamiWidgetCard(
            R.drawable.widget_nyami_wait_c,
            R.color.widget_bg_sky,
            R.string.widget_wait_c_title,
            R.string.widget_wait_c_sub,
            subAfterMealTime = R.string.widget_wait_c_sub_after,
        ),
        NyamiWidgetCard(R.drawable.widget_nyami_wait_d, R.color.widget_bg_sky, R.string.widget_wait_d_title, R.string.widget_wait_d_sub),
    )

    private val full = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_full_a, R.color.widget_bg_orange, R.string.widget_full_a_title, R.string.widget_full_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_full_b, R.color.widget_bg_orange, R.string.widget_full_b_title, R.string.widget_full_b_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_full_c, R.color.widget_bg_orange, R.string.widget_full_c_title, R.string.widget_full_c_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_full_d, R.color.widget_bg_orange, R.string.widget_full_d_title, R.string.widget_full_d_sub),
    )

    private val missedOne = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_a, R.color.widget_bg_pink, R.string.widget_miss1_a_title, R.string.widget_miss1_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_b, R.color.widget_bg_pink, R.string.widget_miss1_b_title, R.string.widget_miss1_b_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_c, R.color.widget_bg_pink, R.string.widget_miss1_c_title, R.string.widget_miss1_c_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_e, R.color.widget_bg_sky, R.string.widget_miss1_e_title, R.string.widget_miss1_e_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_f, R.color.widget_bg_mustard, R.string.widget_miss1_f_title, R.string.widget_miss1_f_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss1_g, R.color.widget_bg_coral, R.string.widget_miss1_g_title, R.string.widget_miss1_g_sub),
    )

    private val missedTwo = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_miss2_a, R.color.widget_bg_purple, R.string.widget_miss2_a_title, R.string.widget_miss2_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss2_b, R.color.widget_bg_purple, R.string.widget_miss2_b_title, R.string.widget_miss2_b_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss2_c, R.color.widget_bg_purple, R.string.widget_miss2_c_title, R.string.widget_miss2_c_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss2_d, R.color.widget_bg_purple, R.string.widget_miss2_d_title, R.string.widget_miss2_d_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_miss2_e, R.color.widget_bg_teal, R.string.widget_miss2_e_title, R.string.widget_miss2_e_sub),
    )

    private val missedAll = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_missall_a, R.color.widget_bg_navy, R.string.widget_missall_a_title, R.string.widget_missall_a_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_missall_b, R.color.widget_bg_navy, R.string.widget_missall_b_title, R.string.widget_missall_b_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_missall_c, R.color.widget_bg_navy, R.string.widget_missall_c_title, R.string.widget_missall_c_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_missall_d, R.color.widget_bg_navy, R.string.widget_missall_d_title, R.string.widget_missall_d_sub),
    )

    /** 거른 날 수(1~7)별 시안. 7은 7일 이상이다. */
    private val away: Map<Int, List<NyamiWidgetCard>> = mapOf(
        1 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away1, R.color.widget_bg_mustard, R.string.widget_away1_title, R.string.widget_away1_rest, showsAwayDays = true),
        ),
        2 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away2, R.color.widget_bg_teal, R.string.widget_away2_title, R.string.widget_away2_rest),
        ),
        3 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away3, R.color.widget_bg_magenta, R.string.widget_away3_title, R.string.widget_away3_rest, showsAwayDays = true),
        ),
        4 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away4, R.color.widget_bg_slate, R.string.widget_away4_title, R.string.widget_away4_rest, showsAwayDays = true),
        ),
        5 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away5_a, R.color.widget_bg_brick, R.string.widget_away5_a_title, R.string.widget_away5_a_rest, showsAwayDays = true),
            NyamiWidgetCard(R.drawable.widget_nyami_away5_b, R.color.widget_bg_purple, R.string.widget_away5_b_title, R.string.widget_away5_b_rest, showsAwayDays = true),
        ),
        6 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away6_a, R.color.widget_bg_taupe, R.string.widget_away6_a_title, R.string.widget_away6_a_rest, showsAwayDays = true),
            NyamiWidgetCard(R.drawable.widget_nyami_away6_b, R.color.widget_bg_green, R.string.widget_away6_b_title, R.string.widget_away6_b_rest),
            NyamiWidgetCard(R.drawable.widget_nyami_away6_c, R.color.widget_bg_taupe, R.string.widget_away6_c_title, R.string.widget_away6_c_rest, showsAwayDays = true),
        ),
        7 to listOf(
            NyamiWidgetCard(R.drawable.widget_nyami_away7, R.color.widget_bg_forest, R.string.widget_away7_title, R.string.widget_away7_rest),
        ),
    )

    private val reunion = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_reunion, R.color.widget_bg_coral, R.string.widget_reunion_title, R.string.widget_reunion_sub),
    )

    private val loggedOut = listOf(
        NyamiWidgetCard(R.drawable.widget_nyami_logout_a, R.color.widget_bg_midnight, R.string.widget_logout_title, R.string.widget_logout_sub),
        NyamiWidgetCard(R.drawable.widget_nyami_logout_b, R.color.widget_bg_midnight, R.string.widget_logout_title, R.string.widget_logout_sub),
    )

    /** [plan] 상태의 시안들. 비어 있는 상태는 없다. */
    fun cardsFor(plan: WidgetPlanVO): List<NyamiWidgetCard> = when (plan.stage) {
        WidgetStage.NIGHT -> night
        WidgetStage.WAITING -> waiting
        WidgetStage.FULL -> full
        WidgetStage.MISSED_ONE -> missedOne
        WidgetStage.MISSED_TWO -> missedTwo
        WidgetStage.MISSED_ALL -> missedAll
        WidgetStage.AWAY -> away.getValue(plan.awayDays.coerceIn(1, AWAY_DAYS_MAX))
        WidgetStage.REUNION -> reunion
        WidgetStage.LOGGED_OUT -> loggedOut
    }

    fun variantCount(plan: WidgetPlanVO): Int = cardsFor(plan).size

    fun cardFor(card: WidgetCardVO): NyamiWidgetCard {
        val cards = cardsFor(card.plan)
        return cards[card.variant.coerceIn(0, cards.lastIndex)]
    }
}
