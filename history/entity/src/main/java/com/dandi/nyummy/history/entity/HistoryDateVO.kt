package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 히스토리에서 다루는 달력 날짜 값 객체입니다.
 *
 * 캘린더 그리드 계산과 직렬화에 시간대(Time zone) 변환이 끼어들지 않도록
 * epoch 시각 대신 연/월/일 정수를 그대로 보관합니다.
 *
 * @property year 연도 (예: 2026)
 * @property month 월, 1..12
 * @property day 일, 1..31
 */
@Serializable
data class HistoryDateVO(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
) {

    val isEmpty: Boolean
        get() = this == empty

    companion object {
        val empty = HistoryDateVO()
    }
}
