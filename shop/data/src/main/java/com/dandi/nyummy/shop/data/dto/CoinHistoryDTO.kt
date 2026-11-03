package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.CoinHistoryVO
import kotlinx.serialization.Serializable
import java.time.ZoneId

/**
 * GET /api/v1/coins/histories 응답(코인 내역 한 묶음, 최신순).
 *
 * @property nextCursor 다음 묶음을 읽을 때 cursor로 넘길 값. [hasNext]가 false면 null이다.
 */
@Serializable
data class CoinHistoriesDTO(
    val coinHistories: List<CoinHistoryDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(zone: ZoneId = DeviceTime.zone()): CoinHistoryPageVO = CoinHistoryPageVO(
        items = coinHistories.orEmpty().mapNotNull { it.toVOOrNull(zone) },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * 코인을 얻거나 쓴 내역 하나.
 *
 * @property category MAIL_CLAIM / ROOM_ITEM_PURCHASE / IN_APP_PURCHASE / COIN_GIFT(코인 충전 이벤트 선물). 앱은 분기하지 않는다
 * @property quantity 부호 있는 수량(+ 얻음, − 씀)
 * @property createdAt UTC 시각(ISO date-time)
 */
@Serializable
data class CoinHistoryDTO(
    val coinHistoryId: Long? = null,
    val category: String? = null,
    val title: String? = null,
    val quantity: Int? = null,
    val balanceAfter: Int? = null,
    val createdAt: String? = null,
) {
    /** 시각을 읽을 수 없으면 날짜 머리글을 정할 수 없으므로 null. 시각은 기기 타임존으로 바꿔 둔다. */
    fun toVOOrNull(zone: ZoneId = DeviceTime.zone()): CoinHistoryVO? = CoinHistoryVO(
        coinHistoryId = coinHistoryId ?: 0L,
        category = category.orEmpty(),
        title = title?.trim().orEmpty(),
        quantity = quantity ?: 0,
        balanceAfter = balanceAfter ?: 0,
        createdAt = DeviceTime.parseIsoOrNull(createdAt, zone)?.toLocalDateTime() ?: return null,
    )
}
