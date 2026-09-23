package com.dandi.nyummy.history.presentation.util

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryDateVO
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class HistoryCalendarGridTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    /** 캘린더·오늘 계산이 기기 타임존과 무관해야 하므로, 일부러 KST 가 아닌 곳으로 바꿔 둔다. */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `그리드는 항상 42칸이다`() {
        assertEquals(CALENDAR_CELL_COUNT, buildCalendarCells(2026, 7).size)
        assertEquals(CALENDAR_CELL_COUNT, buildCalendarCells(2026, 2).size)
        assertEquals(CALENDAR_CELL_COUNT, buildCalendarCells(2024, 12).size)
    }

    @Test
    fun `2026년 7월은 수요일(4번째 칸)에서 1일이 시작한다`() {
        val cells = buildCalendarCells(2026, 7)

        assertEquals(HistoryDateVO(2026, 6, 28), cells[0].date)
        assertFalse(cells[0].inCurrentMonth)
        assertEquals(HistoryDateVO(2026, 7, 1), cells[3].date)
        assertTrue(cells[3].inCurrentMonth)
        assertEquals(HistoryDateVO(2026, 7, 31), cells[33].date)
        assertTrue(cells[33].inCurrentMonth)
        assertEquals(HistoryDateVO(2026, 8, 8), cells[41].date)
        assertFalse(cells[41].inCurrentMonth)
    }

    @Test
    fun `표시 월의 모든 날짜가 순서대로 포함된다`() {
        val cells = buildCalendarCells(2026, 7)
        val currentMonthDays = cells.filter { it.inCurrentMonth }.map { it.date.day }

        assertEquals((1..31).toList(), currentMonthDays)
    }

    @Test
    fun `1일이 일요일인 달은 앞채움 없이 시작한다`() {
        // 2026년 11월 1일은 일요일
        val cells = buildCalendarCells(2026, 11)

        assertEquals(HistoryDateVO(2026, 11, 1), cells[0].date)
        assertTrue(cells[0].inCurrentMonth)
    }

    @Test
    fun `말일은 윤년 규칙을 따른다`() {
        assertEquals(28, lastDayOf(2026, 2))
        assertEquals(29, lastDayOf(2028, 2))
        assertEquals(31, lastDayOf(2026, 7))
        assertEquals(30, lastDayOf(2026, 11))
    }

    @Test
    fun `오늘은 기기 타임존이 아니라 한국 기준이다`() {
        // KST 2026-09-17 00:30 = LA 로는 아직 9/16 인 순간.
        val kstMidnightish = KstTime.epochMillisOf(2026, 9, 17, 0, 30)

        assertEquals(HistoryDateVO(2026, 9, 17), todayDate(kstMidnightish))
    }

    @Test
    fun `한국 자정 경계 전후로 날짜가 바뀐다`() {
        val beforeKstMidnight = KstTime.epochMillisOf(2026, 9, 16, 23, 59)
        val afterKstMidnight = KstTime.epochMillisOf(2026, 9, 17, 0, 0)

        assertEquals(HistoryDateVO(2026, 9, 16), todayDate(beforeKstMidnight))
        assertEquals(HistoryDateVO(2026, 9, 17), todayDate(afterKstMidnight))
    }

    @Test
    fun `연 경계에서 이전-다음 달 계산이 올바르다`() {
        assertEquals(2025 to 12, previousMonthOf(2026, 1))
        assertEquals(2027 to 1, nextMonthOf(2026, 12))
        assertEquals(2026 to 6, previousMonthOf(2026, 7))
        assertEquals(2026 to 8, nextMonthOf(2026, 7))
    }

    @Test
    fun `날짜 미래 비교가 연-월-일 순으로 동작한다`() {
        assertTrue(HistoryDateVO(2026, 8, 1).isAfter(HistoryDateVO(2026, 7, 31)))
        assertTrue(HistoryDateVO(2027, 1, 1).isAfter(HistoryDateVO(2026, 12, 31)))
        assertFalse(HistoryDateVO(2026, 7, 18).isAfter(HistoryDateVO(2026, 7, 18)))
        assertFalse(HistoryDateVO(2026, 7, 17).isAfter(HistoryDateVO(2026, 7, 18)))
    }
}
