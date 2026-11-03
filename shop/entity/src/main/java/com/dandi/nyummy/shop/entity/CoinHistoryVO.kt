package com.dandi.nyummy.shop.entity

import java.time.LocalDateTime

/**
 * 코인을 얻거나 쓴 내역 하나.
 *
 * @property category 서버 내역 종류(MAIL_CLAIM, ROOM_ITEM_PURCHASE, IN_APP_PURCHASE …). 앱은 분기하지 않는다
 * @property title 서버 제목("우편 받기", "발바닥 방석 외 2개 구매")
 * @property quantity 부호 있는 수량(+ 얻음, − 씀)
 * @property balanceAfter 이 내역 뒤 남은 코인
 * @property createdAt 기기 타임존 기준 시각. 서버 UTC 시각을 data 레이어가 바꿔 둔다
 */
data class CoinHistoryVO(
    val coinHistoryId: Long = 0L,
    val category: String = "",
    val title: String = "",
    val quantity: Int = 0,
    val balanceAfter: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.MIN,
)

/**
 * 코인 내역 한 묶음(최신순).
 *
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null
 */
data class CoinHistoryPageVO(
    val items: List<CoinHistoryVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean get() = nextCursor != null
}
