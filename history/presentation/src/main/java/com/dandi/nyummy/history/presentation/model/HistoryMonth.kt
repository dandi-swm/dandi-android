package com.dandi.nyummy.history.presentation.model

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.history.entity.HistoryDateVO

/** 캘린더 한 장이 보여 주는 연/월입니다. */
@Immutable
data class HistoryMonth(val year: Int, val month: Int) : Comparable<HistoryMonth> {

    /** "2026년 10월" 형태의 라벨입니다. */
    val label: String
        get() = monthLabelOf(year, month)

    /** [months]만큼 앞(양수)이나 뒤(음수)로 옮긴 달입니다. */
    fun plusMonths(months: Int): HistoryMonth {
        val index = monthIndex + months
        return HistoryMonth(year = Math.floorDiv(index, 12), month = Math.floorMod(index, 12) + 1)
    }

    /** [other]까지 몇 달 차이인지. [other]가 뒤면 양수입니다. */
    fun monthsUntil(other: HistoryMonth): Int = other.monthIndex - monthIndex

    /** [date]가 이 달의 날짜인지. */
    fun contains(date: HistoryDateVO): Boolean = date.year == year && date.month == month

    override fun compareTo(other: HistoryMonth): Int = monthIndex.compareTo(other.monthIndex)

    private val monthIndex: Int
        get() = year * 12 + (month - 1)

    companion object {
        fun of(date: HistoryDateVO): HistoryMonth = HistoryMonth(year = date.year, month = date.month)
    }
}
