package com.dandi.nyummy.shop.entity

import com.dandi.nyummy.common.entity.richText.RichText

/** 서버가 정한 누름 동작. */
sealed interface ShopActionVO {

    /** 앱 링크(`https://link.nyummy.co.kr/...`) 화면으로 간다. */
    data class Navigate(val link: String) : ShopActionVO

    /** 짧은 안내를 띄운다. */
    data class ShowToast(val message: String) : ShopActionVO
}

/** 배너 모서리. 추천 탭은 둥글게, 프로모션 화면 맨 위는 각지게 그린다. */
enum class BannerCornerStyle {
    ROUNDED,
    SQUARE,
}

/**
 * 프로모션 배너 하나. 그림은 글자 없는 7:4 그림이고, 글자는 서버 서식으로 위에 그린다.
 *
 * @property price 프로모션 상품 전체 묶음 가격. 할인이 없으면 빈 목록이고 가격표를 그리지 않는다
 * @property action 누르면 할 일. 프로모션 화면 맨 위 배너는 null(누를 수 없음)
 */
data class ShopBannerVO(
    val bannerId: Long = 0L,
    val imageUrl: String = "",
    val cornerStyle: BannerCornerStyle = BannerCornerStyle.ROUNDED,
    val title: RichText = emptyList(),
    val description: RichText = emptyList(),
    val price: RichText = emptyList(),
    val action: ShopActionVO? = null,
)

/**
 * 추천 탭 SDUI 칸 하나. 서버가 내려준 순서대로 위에서 아래로 그린다.
 * 앱이 모르는 칸이나 필수 값이 빠진 칸은 data 레이어가 미리 뺀다.
 */
sealed interface SduiComponentVO {

    /** 가로로 넘기는 배너 묶음. */
    data class HeroBanner(val banners: List<ShopBannerVO> = emptyList()) : SduiComponentVO

    /**
     * 칸 제목 줄.
     *
     * @property trailing 오른쪽 영역 글자("전체보기 >", "9시간 후 바뀌어요"). 없으면 빈 목록
     * @property trailingAction 오른쪽 영역을 누르면 할 일. 없으면 누를 수 없다
     */
    data class SectionHeader(
        val title: RichText = emptyList(),
        val trailing: RichText = emptyList(),
        val trailingAction: ShopActionVO? = null,
    ) : SduiComponentVO

    /** 3열 카드. */
    data class ItemGrid(val items: List<ShopItemVO> = emptyList()) : SduiComponentVO

    /** 1부터 순위 번호가 붙은 줄. */
    data class Ranking(val items: List<ShopItemVO> = emptyList()) : SduiComponentVO

    /** 순위 없는 줄. */
    data class ItemList(val items: List<ShopItemVO> = emptyList()) : SduiComponentVO
}

/**
 * 추천 탭 한 장.
 *
 * @property layoutVariant 이 칸 목록을 받을 때 보낸 배치 그룹(A, B). 서버는 그룹마다 칸 순서를 다르게 준다
 */
data class ShopRecommendPageVO(
    val components: List<SduiComponentVO> = emptyList(),
    val layoutVariant: String = "A",
)

/**
 * 프로모션 상품 모음 한 묶음. 맨 위 배너는 첫 묶음과 다음 묶음에 같이 온다.
 *
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null
 */
data class ShopPromotionPageVO(
    val banner: ShopBannerVO = ShopBannerVO(),
    val items: List<ShopItemVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean get() = nextCursor != null
}
