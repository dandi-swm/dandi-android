package com.dandi.nyummy.history.presentation.util

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth
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
    fun `그 달 날짜가 있는 주만 그린다`() {
        // 2026년 2월: 일요일 시작, 28일 → 4주
        assertEquals(4 * DAYS_IN_WEEK, buildCalendarCells(2026, 2).size)
        // 2026년 10월: 목요일 시작, 31일이 토요일 → 5주(11월만 있는 주는 없다)
        assertEquals(5 * DAYS_IN_WEEK, buildCalendarCells(2026, 10).size)
        // 2026년 5월: 금요일 시작, 31일 → 6주
        assertEquals(6 * DAYS_IN_WEEK, buildCalendarCells(2026, 5).size)
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
        assertEquals(HistoryDateVO(2026, 8, 1), cells.last().date)
        assertFalse(cells.last().inCurrentMonth)
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

    @Test
    fun `주 수는 1일의 요일과 그 달 길이로 정해진다`() {
        assertEquals(4, weekCountOf(2026, 2))
        assertEquals(5, weekCountOf(2026, 10))
        assertEquals(6, weekCountOf(2026, 5))
        assertEquals(6, weekCountOf(2026, 8))
    }

    @Test
    fun `캘린더 마지막 장이 이번 달이고 앞 장은 지난달이다`() {
        val current = HistoryMonth(2026, 1)
        val last = HISTORY_MONTH_PAGE_COUNT - 1

        assertEquals(current, historyMonthAt(last, current))
        assertEquals(HistoryMonth(2025, 12), historyMonthAt(last - 1, current))
        assertEquals(last, historyPageOf(current, current))
        assertEquals(last - 13, historyPageOf(HistoryMonth(2024, 12), current))
        // 첫 장은 정확히 120개월 전이다.
        assertEquals(HistoryMonth(2016, 1), historyMonthAt(0, current))
    }

    @Test
    fun `범위를 벗어난 달은 가장 가까운 장으로 맞춘다`() {
        val current = HistoryMonth(2026, 10)

        assertEquals(HISTORY_MONTH_PAGE_COUNT - 1, historyPageOf(HistoryMonth(2027, 3), current))
        assertEquals(0, historyPageOf(HistoryMonth(1990, 1), current))
    }
}
