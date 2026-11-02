package com.dandi.nyummy.shop.entity

/**
 * 장바구니 전체. 조회·수량·선택·빼기가 모두 이 모양을 돌려주고, 화면은 받은 값으로 통째로 다시 그린다.
 *
 * @property items 담은 시각 최신순
 * @property selectedItemCount 선택된 항목 수("전체 선택 n/m"의 n)
 */
data class CartVO(
    val items: List<CartItemVO> = emptyList(),
    val selectedItemCount: Int = 0,
    val summary: CartSummaryVO = CartSummaryVO(),
)

/**
 * 장바구니 항목 하나.
 *
 * @property maxQuantity 지금 이 항목에 둘 수 있는 최대 수량(상품당 10개·방 배경 1개·구매 한도·장바구니 전체 30개 반영).
 *   구매할 수 없으면 0이다
 * @property canPurchase 지금 살 수 있는지. false면 선택되지 않는다
 * @property purchasePrice 1개 결제가. 구매 요청에 화면에서 본 값으로 보낸다
 * @property unavailableReason 살 수 없는 이유("판매가 끝났어요" 등). 살 수 있으면 빈 문자열
 */
data class CartItemVO(
    val shopItem: ShopItemVO = ShopItemVO(),
    val quantity: Int = 0,
    val maxQuantity: Int = 0,
    val isSelected: Boolean = false,
    val canPurchase: Boolean = false,
    val purchasePrice: Int = 0,
    val unavailableReason: String = "",
)

/**
 * 선택됐고 살 수 있는 항목만 합친 결제 정보.
 *
 * @property quantity 선택 상품 수량 합
 * @property listPrice 정가 합
 * @property discountPrice 할인 합(정가 합 − 결제가 합)
 * @property purchasePrice 결제할 코인
 * @property balance 내 코인
 * @property balanceAfter 구매 후 남는 코인. 모자라면 음수다
 */
data class CartSummaryVO(
    val quantity: Int = 0,
    val listPrice: Int = 0,
    val discountPrice: Int = 0,
    val purchasePrice: Int = 0,
    val balance: Int = 0,
    val balanceAfter: Int = 0,
)

/**
 * 장바구니 담기 결과.
 *
 * @property quantity 담은 뒤 그 상품의 수량
 * @property cartItemCount 담은 뒤 장바구니 상품 종류 수(상단 배지)
 */
data class AddedCartItemVO(
    val productId: Long = 0L,
    val quantity: Int = 0,
    val cartItemCount: Int = 0,
)

/**
 * 구매할 상품 하나. 장바구니에 담긴 상품·수량과 화면에서 본 1개 결제가를 그대로 보낸다.
 */
data class PurchaseProductVO(
    val productId: Long = 0L,
    val quantity: Int = 0,
    val purchasePrice: Int = 0,
)

/**
 * 구매 결과. 산 아이템은 상품마다 우편 한 통으로 우편함에 간다.
 *
 * @property quantity 산 개수 합
 * @property purchasePrice 차감한 코인
 * @property balance 구매 뒤 코인 잔액
 * @property cartItemCount 구매 뒤 장바구니 상품 종류 수
 */
data class PurchaseResultVO(
    val itemPurchaseId: Long = 0L,
    val quantity: Int = 0,
    val purchasePrice: Int = 0,
    val balance: Int = 0,
    val cartItemCount: Int = 0,
)
