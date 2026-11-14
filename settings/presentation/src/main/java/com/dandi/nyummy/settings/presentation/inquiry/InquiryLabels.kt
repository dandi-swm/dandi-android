package com.dandi.nyummy.settings.presentation.inquiry

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.presentation.R
import java.time.format.DateTimeFormatter

/** 문의 유형 라벨. 고르는 시트도 이 순서로 보여 준다. */
@get:StringRes
val InquiryCategory.labelRes: Int
    get() = when (this) {
        InquiryCategory.BUG_REPORT -> R.string.inquiry_category_bug_report
        InquiryCategory.QUESTION -> R.string.inquiry_category_question
        InquiryCategory.SUGGESTION -> R.string.inquiry_category_suggestion
    }

/** 상세 화면의 시각. 한국어는 "10월 5일 오후 12:42"(기기 타임존). 패턴은 언어마다 리소스에서 고른다. */
@Composable
fun inquiryDetailTimeLabel(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.inquiry_detail_time_pattern)
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return DeviceTime.atZone(epochMillis, DeviceTime.zone()).format(formatter)
}
