package com.dandi.nyummy.mailbox.data.dto

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.MailVO
import com.dandi.nyummy.mailbox.entity.RewardBoxVO
import com.dandi.nyummy.mailbox.entity.RewardCoinVO
import com.dandi.nyummy.mailbox.entity.RewardItemVO
import com.dandi.nyummy.mailbox.entity.RewardVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/mails 응답(우편 목록 한 묶음, 최신순). 만료된 우편은 들어 있지 않다.
 *
 * @property nextCursor 다음 묶음을 읽을 때 cursor로 넘길 값. 마지막 묶음이면 null.
 * @property hasNext 다음 묶음이 있다. false면 [nextCursor]는 null이다.
 */
@Serializable
data class MailListResponseDTO(
    val mails: List<MailSummaryDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(): MailPageVO = MailPageVO(
        mails = mails.orEmpty().map { it.toVO() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * 목록의 우편 하나.
 *
 * @property category REWARD_BOX / COIN / ITEM. 모르는 값은 ITEM으로 본다.
 * @property imageId 그림 번호. 앱이 [category]로 CDN 경로를 고르고 붙인다(아이템 id, 상자 번호, 코인 그림 번호).
 * @property expiresAt 받지 않으면 사라지는 시각(ISO date-time). null이면 사라지지 않는다.
 * @property createdAt 도착 시각(ISO date-time).
 */
@Serializable
data class MailSummaryDTO(
    val mailId: Long? = null,
    val category: String? = null,
    val title: String? = null,
    val content: String? = null,
    val imageId: Long? = null,
    val isClaimed: Boolean? = null,
    val expiresAt: String? = null,
    val createdAt: String? = null,
) {
    fun toVO(): MailVO = MailVO(
        mailId = mailId ?: 0L,
        category = MailCategory.entries.firstOrNull { it.name == category } ?: MailCategory.ITEM,
        title = title?.trim().orEmpty(),
        content = content?.trim().orEmpty(),
        imageId = imageId,
        isClaimed = isClaimed == true,
        expiresAtMillis = DeviceTime.parseIsoToEpochMillisOrNull(expiresAt),
        createdAtMillis = DeviceTime.parseIsoToEpochMillisOrNull(createdAt) ?: 0L,
    )
}

/**
 * POST /api/v1/mails/{mailId}/claim 응답(우편 하나를 받고 지급된 보상).
 * 해당하는 값이 없으면 [coin]이나 [items]가 null이고, 코인, 아이템 우편이면 [rewardBox]가 null이다.
 */
@Serializable
data class RewardDTO(
    val coin: RewardCoinDTO? = null,
    val items: List<RewardItemDTO>? = null,
    val rewardBox: RewardBoxDTO? = null,
) {
    fun toVO(): RewardVO = RewardVO(
        coin = coin?.toVO(),
        items = items.orEmpty().map { it.toVO() },
        rewardBox = rewardBox?.toVO(),
    )
}

/** POST /api/v1/mails/claim-all 응답(받은 보상의 합계). 받을 우편이 없으면 둘 다 null이다. */
@Serializable
data class TotalRewardDTO(
    val coin: RewardCoinDTO? = null,
    val items: List<RewardItemDTO>? = null,
) {
    fun toVO(): RewardVO = RewardVO(
        coin = coin?.toVO(),
        items = items.orEmpty().map { it.toVO() },
    )
}

/**
 * 받은 코인.
 *
 * @property imageId 코인 그림 번호. 목록과 같이 앱이 CDN 코인 경로에 붙인다(`coins/{imageId}.png`).
 */
@Serializable
data class RewardCoinDTO(
    val quantity: Int? = null,
    val imageId: Long? = null,
) {
    fun toVO(): RewardCoinVO = RewardCoinVO(quantity = quantity ?: 0, imageId = imageId)
}

/**
 * 받은 아이템 하나.
 *
 * @property imageId 아이템 그림 번호. 목록과 같이 앱이 CDN 아이템 경로에 붙인다(`items/{imageId}.png`).
 */
@Serializable
data class RewardItemDTO(
    val imageId: Long? = null,
    val name: String? = null,
    val quantity: Int? = null,
) {
    fun toVO(): RewardItemVO = RewardItemVO(
        imageId = imageId,
        name = name?.trim().orEmpty(),
        quantity = quantity ?: 0,
    )
}

/**
 * 보상 상자 그림과 열기 연출.
 *
 * @property imageId 상자 그림 번호. 목록과 같이 앱이 CDN 상자 경로에 붙인다(`reward-boxes/{imageId}.png`).
 * @property animationUrl 열기 연출 파일 URL. 지금 서버 응답은 null이다.
 */
@Serializable
data class RewardBoxDTO(
    val imageId: Long? = null,
    val animationUrl: String? = null,
) {
    fun toVO(): RewardBoxVO = RewardBoxVO(imageId = imageId, animationUrl = animationUrl)
}
