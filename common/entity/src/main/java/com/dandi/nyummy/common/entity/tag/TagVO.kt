package com.dandi.nyummy.common.entity.tag

/** 태그 색. PROMO는 베리, SUCCESS는 초록, NEUTRAL은 회색. */
enum class TagTone {
    PROMO,
    SUCCESS,
    NEUTRAL,
}

/**
 * 상품 태그와 배너 배지가 같이 쓰는 짧은 표시("한정", "새 집사 전용").
 * 데이터는 같고 그리는 모양만 자리마다 다르다. 보유 태그는 서버가 보내지 않고 앱이 그린다.
 */
data class TagVO(
    val text: String = "",
    val tone: TagTone = TagTone.NEUTRAL,
)
