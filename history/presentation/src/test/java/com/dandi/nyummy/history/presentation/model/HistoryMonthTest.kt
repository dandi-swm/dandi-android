package com.dandi.nyummy.history.presentation.model

import com.dandi.nyummy.history.entity.HistoryDateVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMonthTest {

    @Test
    fun `달을 앞뒤로 옮기면 연도 경계를 넘는다`() {
        assertEquals(HistoryMonth(2027, 1), HistoryMonth(2026, 12).plusMonths(1))
        assertEquals(HistoryMonth(2025, 12), HistoryMonth(2026, 1).plusMonths(-1))
        assertEquals(HistoryMonth(2024, 10), HistoryMonth(2026, 10).plusMonths(-24))
    }

    @Test
    fun `두 달 사이의 차이는 뒤쪽이 양수다`() {
        assertEquals(2, HistoryMonth(2025, 11).monthsUntil(HistoryMonth(2026, 1)))
        assertEquals(-2, HistoryMonth(2026, 1).monthsUntil(HistoryMonth(2025, 11)))
        assertTrue(HistoryMonth(2025, 12) < HistoryMonth(2026, 1))
    }

    @Test
    fun `그 달의 날짜인지 알고 월 라벨을 만든다`() {
        val october = HistoryMonth.of(HistoryDateVO(2026, 10, 8))

        assertTrue(october.contains(HistoryDateVO(2026, 10, 31)))
        assertFalse(october.contains(HistoryDateVO(2026, 11, 1)))
        assertEquals("2026년 10월", october.label)
    }
}
