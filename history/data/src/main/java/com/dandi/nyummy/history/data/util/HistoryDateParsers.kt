package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.HistoryDateVO
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "yyyy-MM-dd" 문자열을 [HistoryDateVO] 로 파싱한다. 형식이 어긋나면 [HistoryDateVO.empty]. */
internal fun String?.toHistoryDateVO(): HistoryDateVO {
    if (this.isNullOrBlank()) return HistoryDateVO.empty
    val parts = split("-")
    if (parts.size != 3) return HistoryDateVO.empty
    val year = parts[0].toIntOrNull() ?: return HistoryDateVO.empty
    val month = parts[1].toIntOrNull() ?: return HistoryDateVO.empty
    val day = parts[2].toIntOrNull() ?: return HistoryDateVO.empty
    return HistoryDateVO(year = year, month = month, day = day)
}

/** ISO date-time 문자열을 "HH:mm" 표시 문자열로 변환한다. 파싱 실패 시 빈 문자열. */
internal fun String?.toDisplayTime(): String {
    if (this.isNullOrBlank()) return ""
    return runCatching { OffsetDateTime.parse(this).toLocalTime() }
        .recoverCatching { LocalTime.parse(this.substringAfter('T', "")) }
        .map { it.format(TIME_FORMATTER) }
        .getOrDefault("")
}
