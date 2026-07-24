package com.dandi.nyummy.history.presentation.util

import com.dandi.nyummy.history.entity.HistoryDateVO
import java.util.Calendar
import java.util.GregorianCalendar

/** 캘린더 그리드 한 칸의 날짜와 표시 월 소속 여부입니다. */
data class HistoryCalendarCell(
    val date: HistoryDateVO,
    val inCurrentMonth: Boolean,
)

/** 캘린더는 항상 일요일 시작 7열 x 6주 = 42칸으로 그립니다. */
const val CALENDAR_CELL_COUNT = 42

/**
 * 요청한 연/월의 42칸 캘린더 그리드를 계산합니다.
 *
 * 첫 칸은 해당 월 1일이 속한 주의 일요일이며, 남는 칸은 인접 월 날짜로 채웁니다.
 */
fun buildCalendarCells(year: Int, month: Int): List<HistoryCalendarCell> {
    val calendar = GregorianCalendar(year, month - 1, 1)
    val leadingDays = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    calendar.add(Calendar.DAY_OF_MONTH, -leadingDays)

    return List(CALENDAR_CELL_COUNT) {
        val cell = HistoryCalendarCell(
            date = HistoryDateVO(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
            ),
            inCurrentMonth = calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month - 1,
        )
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        cell
    }
}

/** 그리드 칸의 열 위치(index % 7)입니다. 0 = 일요일, 6 = 토요일. */
fun columnOf(index: Int): Int = index % 7

/** 해당 연/월의 말일(28~31)을 돌려줍니다. */
fun lastDayOf(year: Int, month: Int): Int =
    GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

/** 기준 연/월의 이전 달 (연, 월) 쌍을 돌려줍니다. */
fun previousMonthOf(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to month - 1

/** 기준 연/월의 다음 달 (연, 월) 쌍을 돌려줍니다. */
fun nextMonthOf(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) year + 1 to 1 else year to month + 1

/** 오늘 날짜를 [HistoryDateVO]로 돌려줍니다. */
fun todayDate(): HistoryDateVO {
    val calendar = Calendar.getInstance()
    return HistoryDateVO(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
}

/** 날짜가 다른 날짜보다 뒤인지(미래인지) 비교합니다. */
fun HistoryDateVO.isAfter(other: HistoryDateVO): Boolean =
    (year * 10_000 + month * 100 + day) > (other.year * 10_000 + other.month * 100 + other.day)
