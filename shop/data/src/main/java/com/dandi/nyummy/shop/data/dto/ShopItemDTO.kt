package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.common.data.richText.RichTextDTO
import com.dandi.nyummy.common.data.richText.toRichText
import com.dandi.nyummy.common.data.tag.TagDTO
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.shop.entity.BannerCornerStyle
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import kotlinx.serialization.Serializable

/**
 * 상점 상품 하나. 추천 탭·상품 목록·프로모션·장바구니가 같이 쓴다.
 *
 * @property category 분류 이름(BACKGROUND, BED, CAT_WHEEL, RUG, ETC). 늘어날 수 있어 문자열로 받는다.
 * @property categoryName 서버가 정한 분류 문구("배경", "침대" …)
 * @property price 서버가 꾸민 1개 가격(할인율·코인·결제가·취소선 정가)
 * @property tag 한정·할인·신규 태그. 없으면 null
 * @property ownedCount 가진 개수(우편함에서 안 받은 구매분 포함)
 */
@Serializable
data class ShopItemDTO(
    val productId: Long? = null,
    val itemId: Long? = null,
    val name: String? = null,
    val category: String? = null,
    val categoryName: String? = null,
    val price: List<RichTextDTO>? = null,
    val tag: TagDTO? = null,
    val isLimited: Boolean? = null,
    val ownedCount: Int? = null,
) {
    /** 상품 id·그림 id·이름·가격 중 하나라도 없으면 그릴 수도 담을 수도 없으므로 null. */
    fun toVOOrNull(): ShopItemVO? {
        val price = price.toRichText()
        return ShopItemVO(
            productId = productId ?: return null,
            itemId = itemId ?: return null,
            name = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null,
            category = category.orEmpty(),
            categoryName = categoryName?.trim().orEmpty(),
            price = price.takeIf { it.isNotEmpty() } ?: return null,
            tag = tag?.toVO(defaultTone = TagTone.PROMO),
            isLimited = isLimited == true,
            ownedCount = (ownedCount ?: 0).coerceAtLeast(0),
        )
    }
}

/**
 * GET /api/v1/shop/products 응답(인테리어·방 배경 탭 상품 한 묶음).
 *
 * @property nextCursor 다음 묶음을 읽을 때 cursor로 넘길 값. [hasNext]가 false면 null이다.
 */
@Serializable
data class ShopItemsDTO(
    val items: List<ShopItemDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    /** 필수 값이 빠진 상품은 그 상품만 뺀다. */
    fun toVO(): ShopItemPageVO = ShopItemPageVO(
        items = items.orEmpty().mapNotNull { it.toVOOrNull() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * GET /api/v1/shop/promotions/{promotionId}/products 응답(프로모션 상품 모음 한 묶음).
 * 맨 위 [banner]는 다음 묶음에도 같이 온다.
 */
@Serializable
data class PromotionProductsDTO(
    val banner: ShopBannerDTO? = null,
    val items: List<ShopItemDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(): ShopPromotionPageVO = ShopPromotionPageVO(
        banner = banner?.toVOOrNull() ?: ShopBannerVO(),
        items = items.orEmpty().mapNotNull { it.toVOOrNull() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * 프로모션 배너 하나. 그림은 글자 없는 7:4 그림이고 글자는 서버 서식으로 위에 그린다.
 *
 * @property cornerStyle ROUNDED(추천 탭) / SQUARE(프로모션 화면 맨 위). 모르는 값은 ROUNDED로 본다.
 * @property price 묶음 가격. 할인이 없으면 null이다.
 * @property action 누르면 할 일. 프로모션 화면 맨 위 배너는 null이다.
 */
@Serializable
data class ShopBannerDTO(
    val bannerId: Long? = null,
    val imageUrl: String? = null,
    val cornerStyle: String? = null,
    val title: List<RichTextDTO>? = null,
    val description: List<RichTextDTO>? = null,
    val price: List<RichTextDTO>? = null,
    val action: ShopActionDTO? = null,
) {
    /** 그림이나 제목이 없으면 배너를 그릴 수 없으므로 null. */
    fun toVOOrNull(): ShopBannerVO? {
        val title = title.toRichText()
        return ShopBannerVO(
            bannerId = bannerId ?: 0L,
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null,
            cornerStyle = BannerCornerStyle.entries.firstOrNull { it.name == cornerStyle } ?: BannerCornerStyle.ROUNDED,
            title = title.takeIf { it.isNotEmpty() } ?: return null,
            description = description.toRichText(),
            price = price.toRichText(),
            action = action?.toVOOrNull(),
        )
    }
}

/**
 * 서버가 정한 누름 동작.
 *
 * @property actionType navigate / showToast
 * @property navigateLink navigate일 때 열 앱 링크(`https://link.nyummy.co.kr/...`)
 * @property message showToast일 때 띄울 글자
 */
@Serializable
data class ShopActionDTO(
    val actionType: String? = null,
    val navigateLink: String? = null,
    val message: String? = null,
) {
    /** 모르는 동작이거나 링크·글자가 비면 누를 수 없는 것으로 보고 null. */
    fun toVOOrNull(): ShopActionVO? = when (actionType) {
        ACTION_NAVIGATE -> navigateLink?.trim()?.takeIf { it.isNotEmpty() }?.let { ShopActionVO.Navigate(it) }
        ACTION_SHOW_TOAST -> message?.trim()?.takeIf { it.isNotEmpty() }?.let { ShopActionVO.ShowToast(it) }
        else -> null
    }

    private companion object {
        const val ACTION_NAVIGATE = "navigate"
        const val ACTION_SHOW_TOAST = "showToast"
    }
}

/** GET /api/v1/shop/me 응답(상단 바 값). [cartItemCount]는 수량 합이 아니라 상품 종류 수다. */
@Serializable
data class ShopMeDTO(
    val balance: Int? = null,
    val cartItemCount: Int? = null,
) {
    fun toVO(): ShopSummaryVO = ShopSummaryVO(
        balance = balance ?: 0,
        cartItemCount = (cartItemCount ?: 0).coerceAtLeast(0),
    )
}
