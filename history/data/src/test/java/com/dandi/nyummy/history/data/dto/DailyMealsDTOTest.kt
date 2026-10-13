package com.dandi.nyummy.history.data.dto

import com.dandi.nyummy.history.entity.MealAnalysisStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class DailyMealsDTOTest {

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

    @Test
    fun `서버 status를 그대로 옮기고 없거나 모르는 값은 UNKNOWN으로 둔다`() {
        fun statusOf(status: String?) =
            DailyMealsDTO(meals = listOf(DailyMealDTO(mealId = 1, name = "김밥", calory = 300, status = status)))
                .toVO().meals.single().status

        assertEquals(MealAnalysisStatus.COMPLETED, statusOf("COMPLETED"))
        assertEquals(MealAnalysisStatus.FAILED, statusOf("FAILED"))
        assertEquals(MealAnalysisStatus.WAITING, statusOf("WAITING"))
        // 이름과 열량이 있어도 완료로 추측하지 않는다.
        assertEquals(MealAnalysisStatus.UNKNOWN, statusOf(null))
        assertEquals(MealAnalysisStatus.UNKNOWN, statusOf("RETRYING"))
    }

    @Test
    fun `분석에 실패한 식사만 다시 분석할 수 있다`() {
        fun canRetry(status: String) =
            DailyMealsDTO(meals = listOf(DailyMealDTO(mealId = 1, status = status))).toVO().meals.single().canRetryAnalysis

        assertTrue(canRetry("FAILED"))
        assertFalse(canRetry("UNKNOWN"))
        assertFalse(canRetry("WAITING"))
        assertFalse(canRetry("COMPLETED"))
    }

    @Test
    fun `분석이 끝난 식사만 음식 아이콘을 쓰고 서버 기본 아이콘은 버린다`() {
        fun iconOf(status: String?, iconId: Long?) =
            DailyMealsDTO(meals = listOf(DailyMealDTO(mealId = 1, status = status, iconId = iconId)))
                .toVO().meals.single().foodIconId

        assertEquals("12", iconOf("COMPLETED", 12))
        // 서버는 분석 전이거나 실패한 식사에도 기본 아이콘(1)을 준다.
        assertEquals("", iconOf("FAILED", 1))
        assertEquals("", iconOf("ANALYZING", 1))
        assertEquals("", iconOf("WAITING", 1))
        assertEquals("", iconOf(null, 1))
        assertEquals("", iconOf("COMPLETED", null))
    }
}
