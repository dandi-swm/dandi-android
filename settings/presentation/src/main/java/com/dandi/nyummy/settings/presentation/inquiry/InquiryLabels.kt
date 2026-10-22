package com.dandi.nyummy.settings.presentation.inquiry

import androidx.annotation.StringRes
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.time.meridiemLabel
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.presentation.R
import java.time.ZoneId

/** 문의 유형 라벨. 고르는 시트도 이 순서로 보여 준다. */
@get:StringRes
val InquiryCategory.labelRes: Int
    get() = when (this) {
        InquiryCategory.BUG_REPORT -> R.string.inquiry_category_bug_report
        InquiryCategory.QUESTION -> R.string.inquiry_category_question
        InquiryCategory.SUGGESTION -> R.string.inquiry_category_suggestion
    }

/** 상세 화면의 시각. "10월 5일 오후 12:42"(기기 타임존). */
fun inquiryDetailTimeLabel(epochMillis: Long, zone: ZoneId = DeviceTime.zone()): String {
    val time = DeviceTime.atZone(epochMillis, zone)
    return "${time.monthValue}월 ${time.dayOfMonth}일 ${time.meridiemLabel()}"
}
