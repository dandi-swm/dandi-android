package com.dandi.nyummy.common.entity.meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealTimesVOTest {

    @Test
    fun `기본값은 오전 8시, 오후 12시, 오후 6시이고 모두 알림을 받는다`() {
        val default = MealTimesVO.default

        assertEquals(MealTimeVO(hour = 8), default.breakfast)
        assertEquals(MealTimeVO(hour = 12), default.lunch)
        assertEquals(MealTimeVO(hour = 18), default.dinner)
        assertTrue(default.hasAnyMeal)
    }

    @Test
    fun `한 끼만 바꾸고 나머지는 그대로 둔다`() {
        val changed = MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 13))

        assertEquals(MealTimeVO(hour = 13), changed[Meal.LUNCH])
        assertEquals(MealTimesVO.DefaultBreakfast, changed[Meal.BREAKFAST])
        assertEquals(MealTimesVO.DefaultDinner, changed[Meal.DINNER])
    }

    @Test
    fun `세 끼를 모두 안 먹는다고 고르면 알림 받을 끼니가 없다`() {
        val none = Meal.entries.fold(MealTimesVO.default) { acc, meal -> acc.with(meal, acc[meal].copy(isSkipped = true)) }

        assertFalse(none.hasAnyMeal)
    }

    @Test
    fun `먹는 끼니끼리 시각이 같으면 겹친다고 본다`() {
        assertFalse(MealTimesVO.default.hasDuplicateHours)
        assertTrue(MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 8)).hasDuplicateHours)
    }

    @Test
    fun `건너뛴 끼니의 시각은 겹쳐도 상관없다`() {
        val skippedBreakfast = MealTimesVO.default.with(Meal.BREAKFAST, MealTimeVO(hour = 12, isSkipped = true))

        assertFalse(skippedBreakfast.hasDuplicateHours)
    }
}
