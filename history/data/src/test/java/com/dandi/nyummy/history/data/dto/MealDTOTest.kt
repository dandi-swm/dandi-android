package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.entity.HistoryDateVO
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class MealDTOTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    /** 시각·날짜는 기기 타임존을 따르므로, 실행 환경과 무관하게 서울로 고정해 둔다. */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

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

    @Test
    fun `식사 날짜는 기기 타임존 기준으로 옮긴다`() {
        assertEquals(HistoryDateVO(2026, 9, 29), MealDTO(mealAt = "2026-09-29T11:42:29.863Z").toVO().date)
        // UTC로는 전날이어도 기기 타임존(서울)으로 자정을 넘기면 다음 날이다.
        assertEquals(HistoryDateVO(2026, 9, 30), MealDTO(mealAt = "2026-09-29T16:30:00Z").toVO().date)
        assertEquals(HistoryDateVO.empty, MealDTO(mealId = 1).toVO().date)
    }

    @Test
    fun `평가 가능 여부는 서버가 true 를 줄 때만 켠다`() {
        assertEquals(true, MealDTO(mealId = 1, canCreateAnalysisFeedback = true).toVO().canCreateAnalysisFeedback)
        assertEquals(false, MealDTO(mealId = 1, canCreateAnalysisFeedback = false).toVO().canCreateAnalysisFeedback)
        assertEquals(false, MealDTO(mealId = 1).toVO().canCreateAnalysisFeedback)
    }

    @Test
    fun `분석이 끝나지 않은 식사는 서버 기본 아이콘을 버린다`() {
        val vo = MealDTO(mealId = 7, status = "FAILED", iconId = 1).toVO()

        assertEquals("", vo.foodIconId)
    }
}
