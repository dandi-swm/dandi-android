package com.dandi.nyummy.common.presentation.time

import com.dandi.nyummy.common.entity.time.DeviceTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * 목록 행(우편, 문의)의 시각. 오늘은 "오후 3:20", 어제는 "어제", 올해의 다른 날은 "10월 4일", 지난해는 "2025년 10월 4일".
 * 모든 날짜는 기기 타임존 기준이고, 글자는 앱 언어([Locale.getDefault])를 따른다(한국어·일본어, 그 밖은 영어).
 *
 * ViewModel에서 Context 없이 부르는 함수라 문자열 리소스 대신 [ListTimeFormat]에 언어별 표기를 둔다.
 */
fun listTimeLabel(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = DeviceTime.zone(),
    locale: Locale = Locale.getDefault(),
): String {
    val time = DeviceTime.atZone(epochMillis, zone)
    val now = DeviceTime.atZone(nowMillis, zone)
    val format = ListTimeFormat.of(locale)
    return when {
        time.toLocalDate() == now.toLocalDate() -> time.meridiemLabel(locale)
        time.toLocalDate() == now.toLocalDate().minusDays(1) -> format.yesterday
        time.year == now.year -> format.monthDay(time)
        else -> format.yearMonthDay(time)
    }
}

/** "오전 9:05", "오후 12:42" 형태(일본어 "午前9:05", 영어 "9:05 AM"). */
fun ZonedDateTime.meridiemLabel(locale: Locale = Locale.getDefault()): String {
    val clock = "${(hour + 11) % 12 + 1}:${minute.toString().padStart(2, '0')}"
    return ListTimeFormat.of(locale).time(isAm = hour < 12, clock = clock)
}

/** "10월 4일" 형태(일본어 "10月4日", 영어 "Oct 4"). */
fun ZonedDateTime.monthDayLabel(locale: Locale = Locale.getDefault()): String =
    ListTimeFormat.of(locale).monthDay(this)

/** 언어별 목록 시각 표기. */
private enum class ListTimeFormat(val yesterday: String) {
    KOREAN("어제") {
        override fun time(isAm: Boolean, clock: String) = "${if (isAm) "오전" else "오후"} $clock"
        override fun monthDay(time: ZonedDateTime) = "${time.monthValue}월 ${time.dayOfMonth}일"
        override fun yearMonthDay(time: ZonedDateTime) = "${time.year}년 ${monthDay(time)}"
    },
    JAPANESE("昨日") {
        override fun time(isAm: Boolean, clock: String) = "${if (isAm) "午前" else "午後"}$clock"
        override fun monthDay(time: ZonedDateTime) = "${time.monthValue}月${time.dayOfMonth}日"
        override fun yearMonthDay(time: ZonedDateTime) = "${time.year}年${monthDay(time)}"
    },
    ENGLISH("Yesterday") {
        override fun time(isAm: Boolean, clock: String) = "$clock ${if (isAm) "AM" else "PM"}"
        override fun monthDay(time: ZonedDateTime) = "${EnglishMonths[time.monthValue - 1]} ${time.dayOfMonth}"
        override fun yearMonthDay(time: ZonedDateTime) = "${monthDay(time)}, ${time.year}"
    },
    ;

    abstract fun time(isAm: Boolean, clock: String): String
    abstract fun monthDay(time: ZonedDateTime): String
    abstract fun yearMonthDay(time: ZonedDateTime): String

    companion object {
        fun of(locale: Locale): ListTimeFormat = when (locale.language) {
            Locale.KOREAN.language -> KOREAN
            Locale.JAPANESE.language -> JAPANESE
            else -> ENGLISH
        }
    }
}

private val EnglishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
