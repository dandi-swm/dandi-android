package com.dandi.nyummy.history.presentation.util

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryDateVO

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
    // 날짜 산술은 epoch day 정수 연산으로 한다 — 기기 타임존/서머타임의 영향을 받지 않는다.
    val firstEpochDay = KstTime.epochDayOf(year, month, 1)
    val startEpochDay = firstEpochDay - KstTime.sundayBasedWeekdayOf(firstEpochDay)

    return List(CALENDAR_CELL_COUNT) { index ->
        val (cellYear, cellMonth, cellDay) = KstTime.civilOf(startEpochDay + index)
        HistoryCalendarCell(
            date = HistoryDateVO(year = cellYear, month = cellMonth, day = cellDay),
            inCurrentMonth = cellYear == year && cellMonth == month,
        )
    }
}

/** 그리드 칸의 열 위치(index % 7)입니다. 0 = 일요일, 6 = 토요일. */
fun columnOf(index: Int): Int = index % 7

/** 해당 연/월의 말일(28~31)을 돌려줍니다. */
fun lastDayOf(year: Int, month: Int): Int = KstTime.lengthOfMonth(year, month)

/** 기준 연/월의 이전 달 (연, 월) 쌍을 돌려줍니다. */
fun previousMonthOf(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to month - 1

/** 기준 연/월의 다음 달 (연, 월) 쌍을 돌려줍니다. */
fun nextMonthOf(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) year + 1 to 1 else year to month + 1

/**
 * **한국 시각(KST) 기준** 오늘 날짜를 [HistoryDateVO]로 돌려줍니다.
 *
 * 기기 타임존이 무엇이든 항상 한국 기준입니다 — 이 값이 그대로
 * `GET /meals/daily?year&month&day` 쿼리로 나가므로 서버의 날짜 구분과 일치해야 합니다.
 * [nowMillis] 는 테스트에서 시각을 고정하기 위한 훅입니다.
 */
fun todayDate(nowMillis: Long = System.currentTimeMillis()): HistoryDateVO {
    val now = KstTime.now(nowMillis)
    return HistoryDateVO(year = now.year, month = now.month, day = now.day)
}

/** 날짜가 다른 날짜보다 뒤인지(미래인지) 비교합니다. */
fun HistoryDateVO.isAfter(other: HistoryDateVO): Boolean =
    (year * 10_000 + month * 100 + day) > (other.year * 10_000 + other.month * 100 + other.day)
