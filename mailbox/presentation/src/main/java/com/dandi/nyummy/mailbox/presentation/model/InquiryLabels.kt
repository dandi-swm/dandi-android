package com.dandi.nyummy.mailbox.presentation.model

import androidx.annotation.StringRes
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R
import java.time.ZoneId
import java.time.ZonedDateTime

/** 문의 유형 라벨. 고르는 시트도 이 순서로 보여 준다. */
@get:StringRes
val InquiryCategory.labelRes: Int
    get() = when (this) {
        InquiryCategory.BUG_REPORT -> R.string.mailbox_category_bug_report
        InquiryCategory.QUESTION -> R.string.mailbox_category_question
        InquiryCategory.SUGGESTION -> R.string.mailbox_category_suggestion
    }

/**
 * 목록 행의 시각. 오늘 보낸 문의는 "오후 3:20", 올해의 다른 날은 "10월 4일", 지난해는 "2025년 10월 4일".
 * 모든 날짜는 기기 타임존 기준이다.
 */
fun inquiryListTimeLabel(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = DeviceTime.zone(),
): String {
    val time = DeviceTime.atZone(epochMillis, zone)
    val now = DeviceTime.atZone(nowMillis, zone)
    return when {
        time.toLocalDate() == now.toLocalDate() -> time.meridiemLabel()
        time.year == now.year -> "${time.monthValue}월 ${time.dayOfMonth}일"
        else -> "${time.year}년 ${time.monthValue}월 ${time.dayOfMonth}일"
    }
}

/** 상세 화면의 시각. "10월 5일 오후 12:42"(기기 타임존). */
fun inquiryDetailTimeLabel(epochMillis: Long, zone: ZoneId = DeviceTime.zone()): String {
    val time = DeviceTime.atZone(epochMillis, zone)
    return "${time.monthValue}월 ${time.dayOfMonth}일 ${time.meridiemLabel()}"
}

/** "오전 9:05", "오후 12:42" 형태. */
private fun ZonedDateTime.meridiemLabel(): String {
    val minuteLabel = minute.toString().padStart(2, '0')
    return when {
        hour == 0 -> "오전 12:$minuteLabel"
        hour < 12 -> "오전 $hour:$minuteLabel"
        hour == 12 -> "오후 12:$minuteLabel"
        else -> "오후 ${hour - 12}:$minuteLabel"
    }
}
