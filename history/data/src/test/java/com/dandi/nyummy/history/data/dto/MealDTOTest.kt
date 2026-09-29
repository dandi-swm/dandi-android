package com.dandi.nyummy.history.data.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class MealDTOTest {

    @Test
    fun `냐미 한마디와 아이콘 식별자를 VO 로 옮긴다`() {
        val dto = MealDTO(
            mealId = 7,
            name = "연어 포케",
            mealAt = "2026-09-29T11:42:29.863Z",
            status = "COMPLETED",
            nutrition = NutritionDTO(calory = 520, carbs = 48, protein = 32, fat = 18),
            imageUrl = "https://cdn/photo.jpg",
            catComment = "  단백질이 든든해서 냐미가 만족했어!  ",
            iconId = 12,
        )

        val vo = dto.toVO()

        assertEquals("7", vo.id)
        assertEquals("단백질이 든든해서 냐미가 만족했어!", vo.catComment)
        assertEquals("12", vo.foodIconId)
        assertEquals("20:42", vo.recordedAt)
        assertEquals(520, vo.calorieKcal)
    }

    @Test
    fun `한마디와 아이콘이 없으면 빈 값으로 둔다`() {
        val vo = MealDTO(mealId = 1, name = "아침").toVO()

        assertEquals("", vo.catComment)
        assertEquals("", vo.foodIconId)
    }
}
