package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.meal.presentation.component.ceremony.spec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MealCutsceneTest {

    private val rare = setOf(
        MealCutscene.CosmicTaste,
        MealCutscene.GiantNyummy,
        MealCutscene.LuckyNyummy,
        MealCutscene.AllStars,
    )
    private val morning = setOf(
        MealCutscene.MorningWakeUp,
        MealCutscene.BlanketBurrito,
        MealCutscene.LateToast,
        MealCutscene.MilkMustache,
        MealCutscene.CerealFlood,
        MealCutscene.LoafMorning,
    )
    private val lateNight = setOf(
        MealCutscene.MidnightAccomplice,
        MealCutscene.RamenCountdown,
        MealCutscene.DeliveryEars,
        MealCutscene.MidnightAsmr,
        MealCutscene.ConvenienceStore,
    )
    private val everyday = MealCutscene.entries.toSet() - rare - morning - lateNight

    private fun pickMany(hour: Int) = (0 until 2_000).map { seed -> pickMealCutscene(hour, Random(seed)) }

    /** 2,000번 골라 [allowed] 밖은 하나도 없고, [allowed] 의 편마다 [each] 범위만큼 나오는지 본다. */
    private fun assertEvenly(picked: List<MealCutscene>, allowed: Set<MealCutscene>, each: IntRange) {
        val usual = picked.filter { it !in rare }
        assertTrue("시간대 밖 컷신이 나왔다: ${usual.filter { it !in allowed }.distinct()}", usual.all { it in allowed })
        allowed.forEach { cutscene ->
            val count = picked.count { it == cutscene }
            assertTrue("$cutscene 는 고르게 나온다: $count", count in each)
        }
    }

    @Test
    fun `평소 컷신은 열 편이다`() {
        assertEquals(10, everyday.size)
    }

    @Test
    fun `아침 끼니에는 아침 컷신 여섯 편을 고르게 고른다`() {
        (5..10).forEach { hour -> assertEvenly(pickMany(hour), morning, 220..390) }
    }

    @Test
    fun `야식 시간에는 야식 컷신 다섯 편을 고르게 고른다`() {
        listOf(21, 22, 23, 0, 2, 4).forEach { hour -> assertEvenly(pickMany(hour), lateNight, 270..450) }
    }

    @Test
    fun `나머지 시간에는 평소 컷신 열 편을 고르게 고른다`() {
        listOf(11, 13, 17, 20).forEach { hour -> assertEvenly(pickMany(hour), everyday, 110..260) }
    }

    @Test
    fun `희귀 컷신은 시각과 상관없이 가끔 나오고 네 편이 고르게 나온다`() {
        listOf(7, 13, 23).forEach { hour ->
            val picked = pickMany(hour)
            val count = picked.count { it in rare }
            assertTrue("$hour 시 희귀 컷신은 가끔만 나온다: $count", count in 120..280)
            rare.forEach { cutscene ->
                val each = picked.count { it == cutscene }
                assertTrue("$hour 시 $cutscene 는 고르게 나온다: $each", each in 20..90)
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
