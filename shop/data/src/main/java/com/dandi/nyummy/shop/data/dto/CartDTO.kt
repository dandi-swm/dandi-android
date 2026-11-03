package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import kotlinx.serialization.Serializable

/**
 * 장바구니 전체. 조회·수량·선택·빼기 응답이 모두 이 모양이다.
 *
 * @property cartItems 담은 시각 최신순
 * @property selectedItemCount 선택된 항목 수
 */
@Serializable
data class CartDTO(
    val cartItems: List<CartItemDTO>? = null,
    val selectedItemCount: Int? = null,
    val summary: CartSummaryDTO? = null,
) {
    /** 상품 필수 값이 빠진 항목은 그릴 수 없으므로 뺀다. */
    fun toVO(): CartVO = CartVO(
        items = cartItems.orEmpty().mapNotNull { it.toVOOrNull() },
        selectedItemCount = (selectedItemCount ?: 0).coerceAtLeast(0),
        summary = summary?.toVO() ?: CartSummaryVO(),
    )
}

/**
 * 장바구니 항목 하나.
 *
 * @property maxQuantity 지금 둘 수 있는 최대 수량. 살 수 없으면 0
 * @property purchasePrice 1개 결제가. 구매 요청에 그대로 보낸다
 * @property unavailableReason 살 수 없는 이유("판매가 끝났어요" 등). 살 수 있으면 null
 */
@Serializable
data class CartItemDTO(
    val shopItem: ShopItemDTO? = null,
    val quantity: Int? = null,
    val maxQuantity: Int? = null,
    val isSelected: Boolean? = null,
    val canPurchase: Boolean? = null,
    val purchasePrice: Int? = null,
    val unavailableReason: String? = null,
) {
    fun toVOOrNull(): CartItemVO? = CartItemVO(
        shopItem = shopItem?.toVOOrNull() ?: return null,
        quantity = (quantity ?: 0).coerceAtLeast(0),
        maxQuantity = (maxQuantity ?: 0).coerceAtLeast(0),
        isSelected = isSelected == true,
        canPurchase = canPurchase == true,
        purchasePrice = (purchasePrice ?: 0).coerceAtLeast(0),
        unavailableReason = unavailableReason?.trim().orEmpty(),
    )
}

/**
 * 선택됐고 살 수 있는 항목만 합친 결제 정보.
 *
 * @property balanceAfter 구매 후 남는 코인. 모자라면 음수다
 */
@Serializable
data class CartSummaryDTO(
    val quantity: Int? = null,
    val listPrice: Int? = null,
    val discountPrice: Int? = null,
    val purchasePrice: Int? = null,
    val balance: Int? = null,
    val balanceAfter: Int? = null,
) {
    fun toVO(): CartSummaryVO = CartSummaryVO(
        quantity = quantity ?: 0,
        listPrice = listPrice ?: 0,
        discountPrice = discountPrice ?: 0,
        purchasePrice = purchasePrice ?: 0,
        balance = balance ?: 0,
        balanceAfter = balanceAfter ?: 0,
    )
}

/** POST /api/v1/shop/cart/items 요청. */
@Serializable
data class AddCartItemRequestDTO(
    val productId: Long,
    val quantity: Int,
)

/**
 * POST /api/v1/shop/cart/items 응답.
 *
 * @property quantity 담은 뒤 그 상품의 수량
 * @property cartItemCount 담은 뒤 장바구니 상품 종류 수
 */
@Serializable
data class AddedCartItemDTO(
    val productId: Long? = null,
    val quantity: Int? = null,
    val cartItemCount: Int? = null,
) {
    fun toVO(): AddedCartItemVO = AddedCartItemVO(
        productId = productId ?: 0L,
        quantity = quantity ?: 0,
        cartItemCount = (cartItemCount ?: 0).coerceAtLeast(0),
    )
}

/** PUT /api/v1/shop/cart/items/{productId} 요청. 수량을 이 값으로 맞춘다. */
@Serializable
data class UpdateCartQuantityRequestDTO(
    val quantity: Int,
)

/** PUT /api/v1/shop/cart/selection 요청. 선택 상품을 이 목록으로 맞춘다(전체 해제는 빈 목록). */
@Serializable
data class UpdateCartSelectionRequestDTO(
    val productIds: List<Long>,
)

/** POST /api/v1/shop/purchases 요청. */
@Serializable
data class PurchaseRequestDTO(
    val products: List<PurchaseProductDTO>,
)

/** 구매할 상품 하나. [purchasePrice]는 화면에서 본 1개 결제가다. */
@Serializable
data class PurchaseProductDTO(
    val productId: Long,
    val quantity: Int,
    val purchasePrice: Int,
)

fun PurchaseProductVO.toDTO(): PurchaseProductDTO =
    PurchaseProductDTO(productId = productId, quantity = quantity, purchasePrice = purchasePrice)

/**
 * POST /api/v1/shop/purchases 응답. 산 아이템은 상품마다 우편 한 통으로 우편함에 간다.
 *
 * @property balance 구매 뒤 코인 잔액
 * @property cartItemCount 구매 뒤 장바구니 상품 종류 수
 */
@Serializable
data class PurchaseResultDTO(
    val itemPurchaseId: Long? = null,
    val quantity: Int? = null,
    val purchasePrice: Int? = null,
    val balance: Int? = null,
    val cartItemCount: Int? = null,
) {
    fun toVO(): PurchaseResultVO = PurchaseResultVO(
        itemPurchaseId = itemPurchaseId ?: 0L,
        quantity = quantity ?: 0,
        purchasePrice = purchasePrice ?: 0,
        balance = balance ?: 0,
        cartItemCount = (cartItemCount ?: 0).coerceAtLeast(0),
    )
}
