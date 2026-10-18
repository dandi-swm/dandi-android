package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 한 달 캘린더와 그 화면에서 고른 날의 상세 기록을 함께 받은 결과입니다.
 */
@Serializable
data class HistoryMonthWithDayVO(
    val calendar: HistoryCalendarVO = HistoryCalendarVO(),
    val dailyDetail: DailyMealHistoryVO = DailyMealHistoryVO(),
) {

    companion object {
        val empty = HistoryMonthWithDayVO()
    }
}
