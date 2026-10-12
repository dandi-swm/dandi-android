package com.dandi.nyummy.mailbox.presentation.model

import androidx.annotation.StringRes
import com.dandi.nyummy.common.entity.time.KstDateTime
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R

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
 * 모든 날짜는 KST 기준이다.
 */
fun inquiryListTimeLabel(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val time = KstTime.atKst(epochMillis)
    val now = KstTime.atKst(nowMillis)
    return when {
        time.isSameDay(now) -> time.meridiemLabel()
        time.year == now.year -> "${time.month}월 ${time.day}일"
        else -> "${time.year}년 ${time.month}월 ${time.day}일"
    }
}

/** 상세 화면의 시각. "10월 5일 오후 12:42"(KST). */
fun inquiryDetailTimeLabel(epochMillis: Long): String {
    val time = KstTime.atKst(epochMillis)
    return "${time.month}월 ${time.day}일 ${time.meridiemLabel()}"
}

private fun KstDateTime.isSameDay(other: KstDateTime): Boolean =
    year == other.year && month == other.month && day == other.day

/** "오전 9:05", "오후 12:42" 형태. */
private fun KstDateTime.meridiemLabel(): String {
    val minuteLabel = minute.toString().padStart(2, '0')
    return when {
        hour == 0 -> "오전 12:$minuteLabel"
        hour < 12 -> "오전 $hour:$minuteLabel"
        hour == 12 -> "오후 12:$minuteLabel"
        else -> "오후 ${hour - 12}:$minuteLabel"
    }
}
