package com.swm.dandi.history.entity

/**
 * 월간 히스토리 캘린더 조회 결과.
 *
 * @property year 조회한 연도.
 * @property month 조회한 월.
 * @property days 월간 캘린더 그리드에 표시할 날짜별 요약 목록.
 */
data class HistoryVO(
    val year: Int = 0,
    val month: Int = 0,
    val days: List<HistoryDayVO> = emptyList(),
) {
    companion object {
        val empty: HistoryVO = HistoryVO()
    }
}
