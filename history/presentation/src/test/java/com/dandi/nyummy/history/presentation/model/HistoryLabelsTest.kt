package com.dandi.nyummy.history.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HistoryLabelsTest {

    private val koreanOrdinals = listOf(
        "첫 끼", "두 번째 끼니", "세 번째 끼니", "네 번째 끼니", "다섯 번째 끼니",
        "여섯 번째 끼니", "일곱 번째 끼니", "여덟 번째 끼니", "아홉 번째 끼니", "열 번째 끼니",
    )

    private fun koreanOrder(orderIndex: Int) = mealOrderLabel(orderIndex, koreanOrdinals, "${orderIndex}번째 끼니")

    @Test
    fun `끼니 순서는 첫 끼부터 열 번째까지 우리말로 쓰고 그 뒤는 숫자를 붙여 쓴다`() {
        assertEquals("첫 끼", koreanOrder(1))
        assertEquals("두 번째 끼니", koreanOrder(2))
        assertEquals("여섯 번째 끼니", koreanOrder(6))
        assertEquals("열 번째 끼니", koreanOrder(10))
        assertEquals("11번째 끼니", koreanOrder(11))
    }

    @Test
    fun `순번이 없으면 첫 끼로 쓴다`() {
        assertEquals("첫 끼", koreanOrder(0))
    }

    @Test
    fun `시각은 한국어 패턴으로 오전 오후를 붙이고 형식이 다르면 원문을 돌려준다`() {
        assertEquals("오전 12:05", formatMealTime("00:05", "a h:mm", Locale.KOREAN))
        assertEquals("오전 8:10", formatMealTime("08:10", "a h:mm", Locale.KOREAN))
        assertEquals("오후 12:30", formatMealTime("12:30", "a h:mm", Locale.KOREAN))
        assertEquals("오후 7:45", formatMealTime("19:45", "a h:mm", Locale.KOREAN))
        assertEquals("19:45", formatMealTime("19:45", "H:mm", Locale.JAPANESE))
        assertEquals("unknown", formatMealTime("unknown", "a h:mm", Locale.KOREAN))
    }

    @Test
    fun `날짜 라벨은 패턴과 언어를 따른다`() {
        val date = LocalDate.of(2026, 9, 8)
        assertEquals("9월 8일", formatDateLabel(date, "M월 d일", Locale.KOREAN))
        assertEquals("9월 8일 화요일", formatDateLabel(date, "M월 d일 EEEE", Locale.KOREAN))
        assertEquals("9月8日", formatDateLabel(date, "M月d日", Locale.JAPANESE))
    }
}
