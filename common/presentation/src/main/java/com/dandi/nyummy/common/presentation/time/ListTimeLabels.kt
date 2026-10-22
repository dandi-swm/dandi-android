package com.dandi.nyummy.common.presentation.time

import com.dandi.nyummy.common.entity.time.DeviceTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 목록 행(우편, 문의)의 시각. 오늘은 "오후 3:20", 어제는 "어제", 올해의 다른 날은 "10월 4일", 지난해는 "2025년 10월 4일".
 * 모든 날짜는 기기 타임존 기준이다.
 */
fun listTimeLabel(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = DeviceTime.zone(),
): String {
    val time = DeviceTime.atZone(epochMillis, zone)
    val now = DeviceTime.atZone(nowMillis, zone)
    return when {
        time.toLocalDate() == now.toLocalDate() -> time.meridiemLabel()
        time.toLocalDate() == now.toLocalDate().minusDays(1) -> "어제"
        time.year == now.year -> "${time.monthValue}월 ${time.dayOfMonth}일"
        else -> "${time.year}년 ${time.monthValue}월 ${time.dayOfMonth}일"
    }
}

/** "오전 9:05", "오후 12:42" 형태. */
fun ZonedDateTime.meridiemLabel(): String {
    val minuteLabel = minute.toString().padStart(2, '0')
    return when {
        hour == 0 -> "오전 12:$minuteLabel"
        hour < 12 -> "오전 $hour:$minuteLabel"
        hour == 12 -> "오후 12:$minuteLabel"
        else -> "오후 ${hour - 12}:$minuteLabel"
    }
}
