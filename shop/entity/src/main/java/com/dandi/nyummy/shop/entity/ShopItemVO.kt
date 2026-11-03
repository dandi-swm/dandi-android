package com.dandi.nyummy.shop.entity

import com.dandi.nyummy.common.entity.richText.RichText
import com.dandi.nyummy.common.entity.tag.TagVO

/**
 * 상점 상품 하나. 추천 탭·상품 목록·프로모션·장바구니·상세 시트가 같이 쓴다.
 * 그림은 화면이 [itemId]로 CDN 주소(`items/{itemId}.png`)를 만든다.
 *
 * @property productId 담기·구매에 쓰는 상품 id
 * @property itemId 대표 아이템 id. 그림 주소를 만든다
 * @property category 서버 분류 이름(BACKGROUND, BED, CAT_WHEEL, RUG, ETC …). 늘어날 수 있어 문자열로 받는다
 * @property categoryName 서버가 정한 분류 문구("배경", "침대" …). 화면에 그대로 보인다
 * @property price 서버가 꾸민 1개 가격(할인율·코인·결제가·취소선 정가). 앱이 다시 계산하지 않는다
 * @property tag 서버가 고른 태그(한정·할인·신규). 없으면 null
 * @property isLimited 기간 한정 상품
 * @property ownedCount 가진 개수(우편함에서 안 받은 구매분 포함). 1 이상이면 태그 자리에 보유 개수를 보인다
 */
data class ShopItemVO(
    val productId: Long = 0L,
    val itemId: Long = 0L,
    val name: String = "",
    val category: String = "",
    val categoryName: String = "",
    val price: RichText = emptyList(),
    val tag: TagVO? = null,
    val isLimited: Boolean = false,
    val ownedCount: Int = 0,
) {
    /** 방 배경 상품. 한 사람이 하나만 가질 수 있다. */
    val isBackground: Boolean get() = category == CATEGORY_BACKGROUND

    val isOwned: Boolean get() = ownedCount > 0

    companion object {
        const val CATEGORY_BACKGROUND = "BACKGROUND"
    }
}

/** 인테리어 탭 분류 칩. 이름이 서버 `category` 값과 같다. "전체"는 칩 값 없이(null) 조회한다. */
enum class ShopCategory {
    LIMITED,
    BED,
    CAT_WHEEL,
    RUG,
    ETC,
}

/**
 * 상품 목록 한 묶음.
 *
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null
 */
data class ShopItemPageVO(
    val items: List<ShopItemVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean get() = nextCursor != null
}

/**
 * 상점 상단 바 값. 상단 바·프로모션 배지·코인 내역의 "내 코인"이 같이 쓴다.
 *
 * @property balance 코인 잔액
 * @property cartItemCount 장바구니에 담긴 상품 종류 수(수량 합이 아니다)
 */
data class ShopSummaryVO(
    val balance: Int = 0,
    val cartItemCount: Int = 0,
)
