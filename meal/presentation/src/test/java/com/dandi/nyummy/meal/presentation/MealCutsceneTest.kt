package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.meal.presentation.component.ceremony.spec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import kotlin.random.Random

class MealCutsceneTest {

    private val rare = setOf(
        MealCutscene.CosmicTaste,
        MealCutscene.GiantNyummy,
        MealCutscene.LuckyNyummy,
        MealCutscene.AllStars,
        MealCutscene.RoyalFeast,
        MealCutscene.PuddingDetective,
    )
    private val weekday = setOf(MealCutscene.MondayBlues, MealCutscene.FridayParty, MealCutscene.WeekendBrunch)
    private val morning = setOf(
        MealCutscene.MorningWakeUp,
        MealCutscene.BlanketBurrito,
        MealCutscene.LateToast,
        MealCutscene.MilkMustache,
        MealCutscene.CerealFlood,
        MealCutscene.LoafMorning,
        MealCutscene.DoubleYolk,
        MealCutscene.MorningStretch,
        MealCutscene.WakeOwner,
        MealCutscene.SparrowGuest,
        MealCutscene.BlenderScare,
        MealCutscene.EatBeforeYouGo,
    )
    private val lateNight = setOf(
        MealCutscene.MidnightAccomplice,
        MealCutscene.RamenCountdown,
        MealCutscene.DeliveryEars,
        MealCutscene.MidnightAsmr,
        MealCutscene.ConvenienceStore,
        MealCutscene.AngelDevil,
        MealCutscene.DrumstickShare,
        MealCutscene.FishCakeCart,
        MealCutscene.HotteokMoon,
        MealCutscene.OvertimeMate,
    )
    private val lunch = setOf(
        MealCutscene.MenuRoulette,
        MealCutscene.LunchLine,
        MealCutscene.LunchboxReveal,
        MealCutscene.FoodComa,
        MealCutscene.SoupSplash,
        MealCutscene.GimbapEnd,
        MealCutscene.SaladTwist,
        MealCutscene.SushiBelt,
    )
    private val dinner = setOf(
        MealCutscene.WelcomeHome,
        MealCutscene.GrillDay,
        MealCutscene.CheesePull,
        MealCutscene.MealShow,
        MealCutscene.LastPiece,
        MealCutscene.TasteTest,
        MealCutscene.TakeoutBag,
        MealCutscene.RainyPajeon,
    )
    private val everyday = MealCutscene.entries.toSet() - rare - weekday - morning - lateNight - lunch - dinner

    private fun pickMany(hour: Int, dayOfWeek: DayOfWeek = DayOfWeek.WEDNESDAY, times: Int = 4_000) =
        (0 until times).map { seed -> pickMealCutscene(hour, dayOfWeek, Random(seed)) }

    /** 희귀 컷신을 뺀 나머지가 [allowed] 안에서만, 편마다 고르게(기대 횟수의 0.6~1.4배) 나오는지 본다. */
    private fun assertEvenly(picked: List<MealCutscene>, allowed: Set<MealCutscene>) {
        val usual = picked.filter { it !in rare }
        assertTrue("시간대 밖 컷신이 나왔다: ${usual.filter { it !in allowed }.distinct()}", usual.all { it in allowed })
        val expected = usual.size.toDouble() / allowed.size
        allowed.forEach { cutscene ->
            val count = usual.count { it == cutscene }
            assertTrue("$cutscene 는 고르게 나온다: $count (기대 $expected)", count > expected * 0.6 && count < expected * 1.4)
        }
    }

    @Test
    fun `평소 컷신은 열두 편이다`() {
        assertEquals(12, everyday.size)
    }

    @Test
    fun `아침 끼니에는 아침 컷신 열두 편을 고르게 고른다`() {
        (5..10).forEach { hour -> assertEvenly(pickMany(hour), morning) }
    }

    @Test
    fun `야식 시간에는 야식 컷신 열 편을 고르게 고른다`() {
        listOf(21, 22, 23, 0, 2, 4).forEach { hour -> assertEvenly(pickMany(hour), lateNight) }
    }

    @Test
    fun `점심에는 점심 컷신과 평소 컷신을 섞어 고르게 고른다`() {
        (11..14).forEach { hour -> assertEvenly(pickMany(hour), lunch + everyday) }
    }

    @Test
    fun `저녁에는 저녁 컷신과 평소 컷신을 섞어 고르게 고른다`() {
        (17..20).forEach { hour -> assertEvenly(pickMany(hour), dinner + everyday) }
    }

    @Test
    fun `끼니 사이 시간에는 평소 컷신만 고르게 고른다`() {
        listOf(15, 16).forEach { hour -> assertEvenly(pickMany(hour), everyday) }
    }

    @Test
    fun `희귀 컷신은 시각과 상관없이 가끔 나오고 여섯 편이 고르게 나온다`() {
        listOf(7, 13, 23).forEach { hour ->
            val picked = pickMany(hour)
            val count = picked.count { it in rare }
            assertTrue("$hour 시 희귀 컷신은 가끔만 나온다: $count", count in 300..500)
            rare.forEach { cutscene ->
                val each = picked.count { it == cutscene }
                assertTrue("$hour 시 $cutscene 는 고르게 나온다: $each", each in 35..100)
            }
        }
    }

    @Test
    fun `월요일과 금요일에는 시각과 상관없이 그 요일 컷신이 가끔 나온다`() {
        mapOf(DayOfWeek.MONDAY to MealCutscene.MondayBlues, DayOfWeek.FRIDAY to MealCutscene.FridayParty).forEach { (day, cutscene) ->
            listOf(7, 13, 19, 23).forEach { hour ->
                val picked = pickMany(hour, day)
                val count = picked.count { it == cutscene }
                assertTrue("$day $hour 시 $cutscene 는 가끔 나온다: $count", count in 550..900)
                assertTrue("$day 에 다른 요일 컷신은 나오지 않는다", picked.none { it in weekday - cutscene })
            }
        }
    }

    @Test
    fun `주말 브런치 컷신은 주말 9시부터 13시 사이에만 나온다`() {
        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { day ->
            (9..13).forEach { hour ->
                val count = pickMany(hour, day).count { it == MealCutscene.WeekendBrunch }
                assertTrue("$day $hour 시 브런치는 가끔 나온다: $count", count in 550..900)
            }
            listOf(8, 14, 19, 23).forEach { hour ->
                assertTrue("$day $hour 시에는 브런치가 나오지 않는다", pickMany(hour, day).none { it in weekday })
            }
        }
    }

    @Test
    fun `요일 컷신이 없는 날에는 요일 컷신이 나오지 않는다`() {
        listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY).forEach { day ->
            listOf(7, 12, 18, 22).forEach { hour ->
                assertTrue("$day $hour 시", pickMany(hour, day).none { it in weekday })
            }
        }
    }

    @Test
    fun `모든 컷신은 카드가 시간 순서대로 놓이고 마지막 컷이 그 뒤에 펼쳐진다`() {
        MealCutscene.entries.forEach { cutscene ->
            val spec = cutscene.spec
            val times = spec.panels.map { it.atMillis }
            assertEquals("$cutscene 카드 순서", times.sorted(), times)
            assertTrue("$cutscene 마지막 컷은 카드 뒤에 놓인다", spec.finale.atMillis > times.last())
            assertTrue("$cutscene 펼치기는 놓인 뒤다", spec.finale.expandAtMillis > spec.finale.atMillis)
            assertTrue("$cutscene 길이는 7초를 넘지 않는다", spec.totalMillis <= 7_000L)
        }
    }
}
