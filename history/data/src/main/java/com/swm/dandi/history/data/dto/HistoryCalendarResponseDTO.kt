package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryVO
import kotlinx.serialization.Serializable

/**
 * 월간 히스토리 캘린더 조회 응답.
 *
 * 화면 문구는 포함하지 않는다. 날짜별 상태 코드와 대표 음식 아이콘 이미지 URL만 받고,
 * presentation 레이어에서 캘린더 셀과 표시 문구로 변환한다.
 */
@Serializable
data class HistoryCalendarResponseDTO(
    val year: Int? = null,
    val month: Int? = null,
    val days: List<HistoryDayDTO>? = null,
)

fun HistoryCalendarResponseDTO.toVO(): HistoryVO =
    HistoryVO(
        year = year ?: 0,
        month = month ?: 0,
        days = days.orEmpty().map { it.toVO() },
    )
