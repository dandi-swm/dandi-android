package com.dandi.nyummy.common.entity.time

import java.util.Locale

/**
 * 한국 표준시(KST) 고정 시간 유틸입니다.
 *
 * 이 앱은 다루는 타임존이 KST 하나뿐이고 KST 는 1988-10-09 이후 서머타임이 없는
 * 고정 UTC+9 이므로, API 26+ 전용인 `java.time`(desugaring 미적용) 대신
 * 순수 정수 연산으로 구현합니다. 순수 Kotlin/JVM 이라 entity/domain/data/presentation
 * 어느 레이어에서도 그대로 쓸 수 있습니다.
 *
 * 다국가 타임존 지원이 요구사항이 되면 이 object 의 public API 만 `java.time` 구현으로
 * 교체하면 호출부는 그대로 둘 수 있습니다.
 *
 * `TimeZone.getTimeZone("Asia/Seoul")` 을 쓰지 않는 이유는 그 구현이
 * 1987-05-10 ~ 1988-10-08 의 서머타임(UTC+10)까지 반영하기 때문입니다.
 * 고정 오프셋 산술에는 그 함정이 없습니다.
 */
object KstTime {

    const val ZONE_ID: String = "Asia/Seoul"
    const val OFFSET_MILLIS: Long = 9L * 60L * 60L * 1000L

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    private const val MILLIS_PER_MINUTE = 60L * 1000L
    private const val MILLIS_PER_SECOND = 1000L

    /**
     * ISO-8601 date-time 패턴입니다.
     * 날짜/시각 구분자는 `T`·`t`·공백을, 오프셋은 `Z`·`±HH:mm`·`±HHmm` 를 허용하고
     * 초와 소수점 이하는 선택입니다.
     */
    private val ISO_REGEX = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[Tt ](\d{2}):(\d{2})(?::(\d{2}))?(?:[.,]\d{1,9})?\s*([Zz]|[+-]\d{2}:?\d{2})?$""",
    )

    /** KST 기준 현재 시각입니다. 테스트는 [nowMillis] 에 고정값을 넘깁니다. */
    fun now(nowMillis: Long = System.currentTimeMillis()): KstDateTime = atKst(nowMillis)

    /** epoch millis 를 KST 벽시계 필드로 변환합니다. */
    fun atKst(epochMillis: Long): KstDateTime {
        val local = epochMillis + OFFSET_MILLIS
        val epochDay = Math.floorDiv(local, MILLIS_PER_DAY)
        val millisOfDay = Math.floorMod(local, MILLIS_PER_DAY)
        val (year, month, day) = civilOf(epochDay)
        return KstDateTime(
            year = year,
            month = month,
            day = day,
            hour = (millisOfDay / MILLIS_PER_HOUR).toInt(),
            minute = ((millisOfDay / MILLIS_PER_MINUTE) % 60L).toInt(),
            second = ((millisOfDay / MILLIS_PER_SECOND) % 60L).toInt(),
        )
    }

    /** KST 벽시계 연/월/일/시/분을 epoch millis 로 변환합니다. */
    fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        epochDayOf(year, month, day) * MILLIS_PER_DAY +
            hour * MILLIS_PER_HOUR + minute * MILLIS_PER_MINUTE - OFFSET_MILLIS

    /**
     * ISO date-time 문자열을 epoch millis 로 파싱합니다. 형식이 어긋나면 null 입니다.
     *
     * 오프셋(`Z` / `+09:00`)이 붙어 있으면 그대로 해석하고, 오프셋이 없으면
     * 이미 KST 로컬 벽시계인 것으로 간주합니다.
     */
    fun parseIsoToEpochMillisOrNull(raw: String?): Long? {
        val match = ISO_REGEX.matchEntire(raw?.trim().orEmpty()) ?: return null
        val groups = match.groupValues
        val year = groups[1].toInt()
        val month = groups[2].toInt()
        val day = groups[3].toInt()
        val hour = groups[4].toInt()
        val minute = groups[5].toInt()
        val second = groups[6].ifEmpty { "0" }.toInt()
        if (month !in 1..12 || day !in 1..lengthOfMonth(year, month)) return null
        // 윤초(60)는 59 로 절삭한다.
        if (hour > 23 || minute > 59 || second > 60) return null

        // 벽시계 필드를 UTC 로 본 epoch 을 만든 뒤, 실제 오프셋만큼 되돌린다.
        val asUtc = epochDayOf(year, month, day) * MILLIS_PER_DAY +
            hour * MILLIS_PER_HOUR +
            minute * MILLIS_PER_MINUTE +
            second.coerceAtMost(59) * MILLIS_PER_SECOND
        val offsetMillis = offsetMillisOrNull(groups[7]) ?: OFFSET_MILLIS
        return asUtc - offsetMillis
    }

    /** ISO date-time 문자열을 KST 벽시계 필드로 파싱합니다. 형식이 어긋나면 null 입니다. */
    fun parseIsoToKstOrNull(raw: String?): KstDateTime? =
        parseIsoToEpochMillisOrNull(raw)?.let(::atKst)

    /** 오프셋 토큰을 millis 로 바꿉니다. 토큰이 없으면 null(= 오프셋 미표기). */
    private fun offsetMillisOrNull(token: String): Long? {
        if (token.isEmpty()) return null
        if (token.equals("Z", ignoreCase = true)) return 0L
        val sign = if (token[0] == '-') -1L else 1L
        val digits = token.drop(1).replace(":", "")
        if (digits.length != 4) return null
        val hours = digits.substring(0, 2).toLongOrNull() ?: return null
        val minutes = digits.substring(2, 4).toLongOrNull() ?: return null
        return sign * (hours * MILLIS_PER_HOUR + minutes * MILLIS_PER_MINUTE)
    }

    /** 해당 연·월의 말일(28~31)입니다. 월이 범위를 벗어나면 0. */
    fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** 요일 인덱스입니다. 0 = 일요일 … 6 = 토요일. */
    fun sundayBasedWeekdayOf(year: Int, month: Int, day: Int): Int =
        sundayBasedWeekdayOf(epochDayOf(year, month, day))

    /** epoch day 0(1970-01-01)은 목요일이므로 +4 만큼 밀어 일요일 기준으로 맞춘다. */
    fun sundayBasedWeekdayOf(epochDay: Long): Int =
        (((epochDay + 4L) % 7L + 7L) % 7L).toInt()

    /** 1970-01-01 을 0 으로 세는 일 수입니다. */
    fun epochDayOf(year: Int, month: Int, day: Int): Long {
        val shiftedYear = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (shiftedYear >= 0) shiftedYear else shiftedYear - 399) / 400
        val yearOfEra = shiftedYear - era * 400
        val shiftedMonth = if (month > 2) month - 3 else month + 9
        val dayOfYear = (153L * shiftedMonth + 2L) / 5L + day - 1L
        val dayOfEra = yearOfEra * 365L + yearOfEra / 4L - yearOfEra / 100L + dayOfYear
        return era * 146_097L + dayOfEra - 719_468L
    }

    /** epoch day 를 (연, 월, 일)로 되돌립니다. */
    fun civilOf(epochDay: Long): Triple<Int, Int, Int> {
        val shifted = epochDay + 719_468L
        val era = (if (shifted >= 0) shifted else shifted - 146_096L) / 146_097L
        val dayOfEra = shifted - era * 146_097L
        val yearOfEra =
            (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
        val year = yearOfEra + era * 400L
        val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
        val shiftedMonth = (5L * dayOfYear + 2L) / 153L
        val day = (dayOfYear - (153L * shiftedMonth + 2L) / 5L + 1L).toInt()
        val month = (if (shiftedMonth < 10L) shiftedMonth + 3L else shiftedMonth - 9L).toInt()
        return Triple((if (month <= 2) year + 1L else year).toInt(), month, day)
    }
}

/**
 * KST 기준 벽시계 값입니다.
 *
 * 타임존 변환이 끝난 뒤의 값만 담습니다 — 서버 문자열에서 이 타입으로의 변환은
 * data 레이어 `toVO()` 에서만 수행합니다.
 */
data class KstDateTime(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
) {

    /** "08:10" 형태의 24시간 표시 문자열입니다. */
    val hourMinuteLabel: String
        get() = String.format(Locale.US, "%02d:%02d", hour, minute)

    /** "2026-09-17" 형태의 서버 교환용 날짜 문자열입니다. */
    val isoDate: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    /** 요일 인덱스입니다. 0 = 일요일 … 6 = 토요일. */
    val sundayBasedWeekday: Int
        get() = KstTime.sundayBasedWeekdayOf(year, month, day)

    companion object {
        val empty = KstDateTime()
    }
}
