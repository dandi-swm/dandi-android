package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 상점·장바구니·구매 API 도메인 에러. `type`은 서버 에러 바디의 `code`와 철자까지 같아야 한다.
 * 구매 실저장(백엔드 #199)이 머지되며 코드가 바뀌면 여기만 고친다.
 */
enum class ShopErrorType(
    override val type: String,
    override val errorMsg: String = "",
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 판매 중인 상품에 없는 productId(404). */
    PRODUCT_NOT_FOUND("api.shop.productNotFound"),

    /** 판매 기간이 아닌 상품(409). */
    PRODUCT_NOT_FOR_SALE("api.shop.productNotForSale"),

    /** 이미 가진 방 배경(409). */
    ALREADY_OWNED("api.shop.alreadyOwned"),

    /** 이 상품을 담을 수 있는 수량(상품당 10개·방 배경 1개)을 넘었다(409). */
    CART_QUANTITY_EXCEEDED("api.shop.cartQuantityExceeded"),

    /** 장바구니 전체 수량(30개)을 넘었다(409). */
    CART_FULL("api.shop.cartFull"),

    /** 장바구니에 없는 상품의 수량을 바꾸려 했다(404). */
    CART_ITEM_NOT_FOUND("api.shop.cartItemNotFound"),

    /** 1인 구매 한도를 넘었다(409, 백엔드 #199). */
    PURCHASE_LIMIT_EXCEEDED("api.shop.purchaseLimitExceeded"),

    /** 구매할 상품이 장바구니 상품·수량과 다르다(409). */
    CART_MISMATCH("api.shop.cartMismatch"),

    /** 화면에서 본 가격과 서버 결제가가 다르다(409). */
    PRICE_CHANGED("api.shop.priceChanged"),

    /** 선물 코인 받기에서 묶음 번호가 1~3 밖이다(404, 코인 충전 이벤트). */
    COIN_PACKAGE_NOT_FOUND("api.coin.coinPackageNotFound"),

    /**
     * 이 회차 선물 코인을 이미 받은 사람이 다시 받으려 했다(409, 코인 충전 이벤트, 코인 변동 없음).
     * 받기 API의 409는 이것뿐이라 코드가 없어도 409면 같은 것으로 본다([ClaimCoinChargeUseCase]).
     */
    COIN_GIFT_ALREADY_CLAIMED("api.coin.coinGiftAlreadyClaimed"),

    /** 코인이 모자란다(409). */
    INSUFFICIENT_BALANCE("api.shop.insufficientBalance"),

    /** 같은 구매 키로 다른 구매를 보냈다(409, 백엔드 #199). */
    IDEMPOTENCY_KEY_REUSED("api.shop.idempotencyKeyReused"),

    /** 앞선 구매가 아직 처리 중이다(409). */
    PURCHASE_IN_PROGRESS("api.shop.purchaseInProgress"),

    /** 진행 중이 아니거나, 대상이 아니거나, 노출 중인 배너가 없는 프로모션(404). */
    PROMOTION_NOT_FOUND("api.shop.promotionNotFound"),
}
