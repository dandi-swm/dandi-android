package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 장바구니를 읽는다. 서버 오류는 공통 안내를 하고 실패를 돌려준다. 응답 전에 끊기면 안내 없이 실패만 돌려준다
 * (화면은 마지막으로 받은 장바구니를 그대로 둔다).
 */
class GetCartUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {
    suspend operator fun invoke(): Result<CartVO> = request(ShopErrorType.entries) { repository.getCart() }
}

/**
 * 항목 수량을 [quantity]로 맞춘다. 화면은 −/+를 누르자마자 수량을 바꾸고 이 값을 보낸다.
 *
 * 서버가 거절하면(상품당 수량·구매 한도·판매 끝남·장바구니에 없음 등) "더 이상 담을 수 없어요"를 띄우고 실패를
 * 돌려준다. 실패면 화면은 장바구니를 다시 읽는다.
 */
class UpdateCartQuantityUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {
    suspend operator fun invoke(productId: Long, quantity: Int): Result<CartVO> = request(
        ShopErrorType.entries,
        onBusinessError = { _, cause ->
            showCannotAddMore()
            Result.failure(cause)
        },
        // 서버가 거절한 응답 오류만 안내한다. 응답 전에 끊기면 화면이 장바구니를 다시 읽는다.
        onUnknownError = { cause -> if (cause is HttpResponseException) showCannotAddMore() },
    ) {
        repository.updateCartItemQuantity(productId, quantity)
    }

    private fun showCannotAddMore() {
        messageHelper.showToast(resourceHelper.getString(StringResource.SHOP_CANNOT_ADD_MORE))
    }
}

/**
 * 선택 상품을 [productIds]로 맞춘다. 전체 해제는 빈 목록이다. 실패하면 안내 없이(서버 오류만 공통 안내) 실패를
 * 돌려주고, 화면은 장바구니를 다시 읽는다.
 */
class UpdateCartSelectionUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {
    suspend operator fun invoke(productIds: List<Long>): Result<CartVO> = request(ShopErrorType.entries) { repository.updateCartSelection(productIds) }
}

/**
 * [productIds] 항목을 장바구니에서 뺀다(줄의 ✕, 선택 삭제). 실패하면 안내 없이(서버 오류만 공통 안내) 실패를
 * 돌려주고, 화면은 장바구니를 다시 읽는다.
 */
class DeleteCartItemsUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {
    suspend operator fun invoke(productIds: List<Long>): Result<CartVO> = request(ShopErrorType.entries) { repository.deleteCartItems(productIds) }
}
