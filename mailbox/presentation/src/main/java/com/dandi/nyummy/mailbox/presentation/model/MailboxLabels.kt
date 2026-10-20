package com.dandi.nyummy.mailbox.presentation.model

import androidx.annotation.StringRes
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.MailCategory
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
 * 우편함 목록 행(아이템, 문의)의 시각. 오늘은 "오후 3:20", 어제는 "어제", 올해의 다른 날은 "10월 4일", 지난해는 "2025년 10월 4일".
 * 모든 날짜는 기기 타임존 기준이다.
 */
fun mailboxListTimeLabel(
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

/**
 * 받지 않으면 우편이 사라질 때까지 남은 날. 하루가 안 남았어도 1로 올려 "1일 남았어요"로 보인다. 이미 지났으면 0.
 */
fun mailRemainingDays(expiresAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
    val remainingMillis = expiresAtMillis - nowMillis
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
}

/**
 * 우편 그림 주소. 종류에 맞는 CDN 경로에 그림 번호를 붙인다. 번호가 없으면 빈 값(그림 자리만 보인다).
 * 보상 상자는 받기 전이면 닫힌 상자, 받았으면([isClaimed]) 열린 상자다.
 */
fun mailImageUrl(category: MailCategory, imageId: Long?, isClaimed: Boolean = false): String {
    if (imageId == null) return ""
    return when (category) {
        MailCategory.REWARD_BOX ->
            if (isClaimed) NyummyCdn.rewardBoxOpenedUrl(imageId) else NyummyCdn.rewardBoxClosedUrl(imageId)
        MailCategory.COIN -> NyummyCdn.coinUrl(imageId)
        MailCategory.ITEM -> NyummyCdn.itemUrl(imageId)
    }
}

/** 남은 날이 이만큼 이하이면 빨갛게 보여 준다. */
const val MAIL_EXPIRING_DAYS = 1

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

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
