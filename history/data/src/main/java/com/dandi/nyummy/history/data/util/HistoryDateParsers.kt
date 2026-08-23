package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.HistoryDateVO

// java.time 파싱 API(OffsetDateTime/LocalTime)는 API 26+ 전용이므로
// minSdk 24 호환을 위해 문자열을 직접 파싱한다.
private val DISPLAY_TIME_REGEX = Regex("""^(\d{2}):(\d{2})""")

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
    val timePart = if (contains('T')) substringAfter('T') else this
    val match = DISPLAY_TIME_REGEX.find(timePart) ?: return ""
    val (hour, minute) = match.destructured
    if (hour.toInt() > 23 || minute.toInt() > 59) return ""
    return "$hour:$minute"
}
