package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.registeredErrorType
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 상품 1개를 장바구니에 담는다. 담기는 늘 1개다(상세 시트에 수량 선택이 없다).
 *
 * 성공하면 "장바구니에 담았어요"를 띄우고, 저장소가 상단 바의 장바구니 종류 수를 바꾼다. 담은 곳([source], [ShopTracker.Source])과 함께 분석 이벤트를 남긴다.
 * 서버가 거절하면 장바구니가 가득 찬 경우만 "장바구니가 가득 찼어요", 그 밖(상품당 수량·구매 한도·판매 종료·
 * 이미 가진 방 배경·없는 상품, 코드 없는 404 포함)은 "더 이상 담을 수 없어요"를 띄운다. 로그인 만료·서버 오류는 공통 안내다.
 */
class AddToCartUseCase @Inject constructor(
    private val repository: ShopRepository,
    private val tracker: ShopTracker,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(productId: Long, source: String): Result<AddedCartItemVO> = try {
        val added = repository.addToCart(productId, QUANTITY)
        messageHelper.showToast(ShopMessages.ADDED_TO_CART)
        tracker.addToCart(source, productId)
        Result.success(added)
    } catch (e: HttpResponseException) {
        if (e.rawCode == HTTP_NOT_FOUND && e.registeredErrorType<ShopErrorType>() == null) {
            // 코드 없는 404도 상품이 사라진 경우로 본다. 공통 처리("준비 중" 다이얼로그 뒤 뒤로 가기)로 보내지 않는다.
            messageHelper.showToast(ShopMessages.CANNOT_ADD_MORE)
        } else {
            handleHttpError<ShopErrorType>(
                e,
                onDomainError = { messageHelper.showToast(rejectionMessageOf(it)) },
                onUnknownError = { messageHelper.showToast(ShopMessages.CANNOT_ADD_MORE) },
            )
        }
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 응답 전 실패. 담겼는지 모르니 다시 해 보라고만 알린다.
        messageHelper.showToast(ShopMessages.ADD_FAILED)
        Result.failure(e)
    }

    private fun rejectionMessageOf(error: ShopErrorType): String =
        if (error == ShopErrorType.CART_FULL) ShopMessages.CART_FULL else ShopMessages.CANNOT_ADD_MORE

    private companion object {
        /** 담기는 늘 1개다. */
        const val QUANTITY = 1
        const val HTTP_NOT_FOUND = 404
    }
}
