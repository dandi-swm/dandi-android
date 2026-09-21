package com.dandi.nyummy.history.data.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyMealsDTOTest {

    @Test
    fun `오프셋 표기가 섞여 있어도 실제 시간순으로 정렬한다`() {
        val dto = DailyMealsDTO(
            date = "2026-09-17",
            meals = listOf(
                DailyMealDTO(mealId = 1, name = "점심", mealAt = "2026-09-17T12:30:00+09:00", calory = 500),
                DailyMealDTO(mealId = 2, name = "아침", mealAt = "2026-09-17T00:10:00Z", calory = 300),
            ),
        )

        val meals = dto.toVO().meals

        assertEquals(listOf("2", "1"), meals.map { it.id })
        assertEquals(listOf(1, 2), meals.map { it.orderIndex })
        assertEquals("09:10", meals.first().recordedAt)
        assertEquals("12:30", meals.last().recordedAt)
    }

    @Test
    fun `시각을 파싱하지 못한 식사는 맨 뒤로 보낸다`() {
        val dto = DailyMealsDTO(
            meals = listOf(
                DailyMealDTO(mealId = 1, name = "알 수 없음", mealAt = null, calory = 100),
                DailyMealDTO(mealId = 2, name = "아침", mealAt = "2026-09-17T00:10:00Z", calory = 300),
            ),
        )

        val meals = dto.toVO().meals

        assertEquals(listOf("2", "1"), meals.map { it.id })
        assertEquals("", meals.last().recordedAt)
    }

    @Test
    fun `식사 목록이 없어도 빈 결과를 돌려준다`() {
        assertEquals(0, DailyMealsDTO().toVO().meals.size)
    }
}
