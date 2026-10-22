package com.dandi.nyummy.mailbox.presentation.model

import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.mailbox.entity.MailCategory

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
