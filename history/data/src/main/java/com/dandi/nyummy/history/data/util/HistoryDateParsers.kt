package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.history.entity.HistoryDateVO
import java.util.Locale

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

/**
 * ISO date-time 문자열을 **기기 타임존 기준** "HH:mm" 표시 문자열로 변환한다. 파싱 실패 시 빈 문자열.
 *
 * 오프셋(`Z` / `+09:00`)이 붙어 있으면 그대로 해석하고, 오프셋이 없으면 UTC 로 본다.
 */
internal fun String?.toDisplayTime(): String =
    DeviceTime.parseIsoOrNull(this)?.let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) }.orEmpty()

/** ISO date-time 문자열의 **기기 타임존 기준** 날짜. 파싱 실패 시 [HistoryDateVO.empty]. */
internal fun String?.toDeviceHistoryDate(): HistoryDateVO =
    DeviceTime.parseIsoOrNull(this)?.let { HistoryDateVO(year = it.year, month = it.monthValue, day = it.dayOfMonth) }
        ?: HistoryDateVO.empty

/** ISO date-time 문자열을 정렬용 epoch millis 로 변환한다. 파싱 실패 시 null. */
internal fun String?.toEpochMillisOrNull(): Long? =
    DeviceTime.parseIsoToEpochMillisOrNull(this)
