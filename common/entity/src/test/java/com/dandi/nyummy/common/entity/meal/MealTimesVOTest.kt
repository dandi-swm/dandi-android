package com.dandi.nyummy.common.entity.meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealTimesVOTest {

    @Test
    fun `기본값은 오전 8시, 오후 12시 30분, 오후 6시 30분이고 모두 알림을 받는다`() {
        val default = MealTimesVO.default

        assertEquals(MealTimeVO(hour = 8, minute = 0), default.breakfast)
        assertEquals(MealTimeVO(hour = 12, minute = 30), default.lunch)
        assertEquals(MealTimeVO(hour = 18, minute = 30), default.dinner)
        assertTrue(default.hasAnyMeal)
    }

    @Test
    fun `자정부터 몇 분째인지로 저장하고 그대로 되돌린다`() {
        val time = MealTimeVO(hour = 18, minute = 30, isSkipped = true)

        assertEquals(1110, time.minuteOfDay)
        assertEquals(time, MealTimeVO.ofMinuteOfDay(time.minuteOfDay, isSkipped = true))
    }

    @Test
    fun `범위를 벗어난 저장값은 하루 안으로 맞춘다`() {
        assertEquals(MealTimeVO(hour = 0, minute = 0), MealTimeVO.ofMinuteOfDay(-5))
        assertEquals(MealTimeVO(hour = 23, minute = 59), MealTimeVO.ofMinuteOfDay(5000))
    }

    @Test
    fun `한 끼만 바꾸고 나머지는 그대로 둔다`() {
        val changed = MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 13, minute = 10))

        assertEquals(MealTimeVO(hour = 13, minute = 10), changed[Meal.LUNCH])
        assertEquals(MealTimesVO.DefaultBreakfast, changed[Meal.BREAKFAST])
        assertEquals(MealTimesVO.DefaultDinner, changed[Meal.DINNER])
    }

    @Test
    fun `세 끼를 모두 안 먹는다고 고르면 알림 받을 끼니가 없다`() {
        val none = Meal.entries.fold(MealTimesVO.default) { acc, meal -> acc.with(meal, acc[meal].copy(isSkipped = true)) }

        assertFalse(none.hasAnyMeal)
    }
}
