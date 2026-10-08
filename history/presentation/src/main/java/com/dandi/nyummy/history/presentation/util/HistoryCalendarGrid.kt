package com.dandi.nyummy.history.presentation.util

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth

/** 캘린더 그리드 한 칸의 날짜와 표시 월 소속 여부입니다. */
data class HistoryCalendarCell(
    val date: HistoryDateVO,
    val inCurrentMonth: Boolean,
)

/** 한 주의 칸 수. 캘린더는 일요일에 시작합니다. */
const val DAYS_IN_WEEK = 7

/**
 * 요청한 연/월의 캘린더 칸을 계산합니다. 그 달 날짜가 있는 주만 그려서 달에 따라 4~6주가 됩니다.
 *
 * 첫 칸은 해당 월 1일이 속한 주의 일요일이며, 첫 주와 마지막 주의 빈칸은 인접 월 날짜로 채웁니다.
 * 다음 달 날짜만 있는 주는 그리지 않습니다.
 */
fun buildCalendarCells(year: Int, month: Int): List<HistoryCalendarCell> {
    // 날짜 산술은 epoch day 정수 연산으로 한다 — 기기 타임존/서머타임의 영향을 받지 않는다.
    val firstEpochDay = KstTime.epochDayOf(year, month, 1)
    val startEpochDay = firstEpochDay - KstTime.sundayBasedWeekdayOf(firstEpochDay)

    return List(weekCountOf(year, month) * DAYS_IN_WEEK) { index ->
        val (cellYear, cellMonth, cellDay) = KstTime.civilOf(startEpochDay + index)
        HistoryCalendarCell(
            date = HistoryDateVO(year = cellYear, month = cellMonth, day = cellDay),
            inCurrentMonth = cellYear == year && cellMonth == month,
        )
    }
}

/** 그 달 날짜가 걸쳐 있는 주 수(4~6). */
fun weekCountOf(year: Int, month: Int): Int {
    val leadingDays = KstTime.sundayBasedWeekdayOf(KstTime.epochDayOf(year, month, 1))
    return (leadingDays + lastDayOf(year, month) + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
}

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

/** 캘린더를 넘겨 볼 수 있는 달 수. 마지막 장인 이번 달에 지난 120개월(10년)을 더한다. */
const val HISTORY_MONTH_PAGE_COUNT = 121

/** 캘린더 [page]번째 장의 달. 마지막 장이 [currentMonth]입니다. */
fun historyMonthAt(page: Int, currentMonth: HistoryMonth): HistoryMonth =
    currentMonth.plusMonths(page - (HISTORY_MONTH_PAGE_COUNT - 1))

/** [month]가 캘린더 몇 번째 장인지. 범위를 벗어나면 가장 가까운 장으로 맞춥니다. */
fun historyPageOf(month: HistoryMonth, currentMonth: HistoryMonth): Int =
    (HISTORY_MONTH_PAGE_COUNT - 1 - month.monthsUntil(currentMonth)).coerceIn(0, HISTORY_MONTH_PAGE_COUNT - 1)
