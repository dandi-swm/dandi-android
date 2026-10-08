package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.entity.HistoryDateVO
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyMealsDTOTest {

    @Test
    fun `이번 달에서 식사가 있는 날만 남긴다`() {
        // 서버는 캘린더 범위의 모든 날짜를 주고, 식사가 있는 날에만 아이콘을 채운다.
        val dto = MonthlyMealsDTO(
            year = 2026,
            month = 10,
            days = listOf(
                MonthlyMealDayDTO(date = "2026-09-30", isCurrentMonth = false, foodIconIds = listOf(3)),
                MonthlyMealDayDTO(date = "2026-10-01", isCurrentMonth = true, dailyNutritionEvaluation = "UNRECORDED", foodIconIds = emptyList()),
                MonthlyMealDayDTO(date = "2026-10-07", isCurrentMonth = true, dailyNutritionEvaluation = "UNRECORDED", foodIconIds = listOf(1)),
                MonthlyMealDayDTO(date = "2026-10-08", isCurrentMonth = true, foodIconIds = null),
            ),
        )

        val days = dto.toVO().days

        assertEquals(listOf(HistoryDateVO(2026, 10, 7)), days.map { it.date })
        assertEquals(listOf("1"), days.single().foodIconIds)
    }

    @Test
    fun `칸 아이콘은 최대 두 개다`() {
        val dto = MonthlyMealsDTO(
            year = 2026,
            month = 10,
            days = listOf(MonthlyMealDayDTO(date = "2026-10-07", isCurrentMonth = true, foodIconIds = listOf(1, 2, 3))),
        )

        assertEquals(listOf("1", "2"), dto.toVO().days.single().foodIconIds)
    }
}
