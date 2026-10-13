package com.dandi.nyummy.common.entity.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * 기기 타임존 기준 시간 유틸입니다.
 *
 * 서버는 모든 시각을 UTC 순간(`Instant`)으로 주고받고, 앱은 화면에 보이거나 "오늘"을 정할 때만
 * 기기 타임존으로 바꿉니다. 기기 타임존은 이 파일에서만 읽어, 호출부가 각자 기기 설정을 읽지 않게 합니다.
 * 테스트는 `nowMillis`·`zone` 에 고정값을 넘깁니다.
 */
object DeviceTime {

    /** 기기 타임존. 앱 실행 중에도 바뀔 수 있어 매번 읽습니다. */
    fun zone(): ZoneId = ZoneId.systemDefault()

    /** 기기 타임존 기준 현재 시각입니다. */
    fun now(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = zone()): ZonedDateTime =
        atZone(nowMillis, zone)

    /** 기기 타임존 기준 오늘 날짜입니다. */
    fun today(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = zone()): LocalDate =
        now(nowMillis, zone).toLocalDate()

    /** epoch millis 를 기기 타임존 시각으로 바꿉니다. */
    fun atZone(epochMillis: Long, zone: ZoneId = zone()): ZonedDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone)

    /**
     * ISO date-time 문자열을 순간(epoch millis)으로 파싱합니다. 형식이 어긋나면 null 입니다.
     *
     * 오프셋(`Z` / `+09:00`)이 붙어 있으면 그대로 해석하고, 오프셋이 없으면 서버 기준인 UTC 로 봅니다.
     * 날짜와 시각 사이 공백도 허용합니다.
     */
    fun parseIsoToEpochMillisOrNull(raw: String?): Long? {
        val text = raw?.trim()?.replace(' ', 'T')?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            OffsetDateTime.parse(text).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /** ISO date-time 문자열을 기기 타임존 시각으로 파싱합니다. 형식이 어긋나면 null 입니다. */
    fun parseIsoOrNull(raw: String?, zone: ZoneId = zone()): ZonedDateTime? =
        parseIsoToEpochMillisOrNull(raw)?.let { atZone(it, zone) }
}

/** 요일 인덱스입니다. 0 = 일요일 … 6 = 토요일. */
val LocalDate.sundayBasedWeekday: Int
    get() = dayOfWeek.value % 7
