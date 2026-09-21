package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryDateVO

// java.time 파싱 API(OffsetDateTime/LocalTime)는 API 26+ 전용이므로
// minSdk 24 호환을 위해 common:entity 의 KstTime(순수 정수 연산)으로 파싱한다.

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
 * ISO date-time 문자열을 **KST 기준** "HH:mm" 표시 문자열로 변환한다. 파싱 실패 시 빈 문자열.
 *
 * 오프셋(`Z` / `+09:00`)이 붙어 있으면 그대로 해석해 한국 시각으로 환산하고,
 * 오프셋이 없으면 이미 KST 로컬 시각인 것으로 본다.
 */
internal fun String?.toDisplayTime(): String =
    KstTime.parseIsoToKstOrNull(this)?.hourMinuteLabel.orEmpty()

/** ISO date-time 문자열을 정렬용 epoch millis 로 변환한다. 파싱 실패 시 null. */
internal fun String?.toKstEpochMillisOrNull(): Long? =
    KstTime.parseIsoToEpochMillisOrNull(this)
