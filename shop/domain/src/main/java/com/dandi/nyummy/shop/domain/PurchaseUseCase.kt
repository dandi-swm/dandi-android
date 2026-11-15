package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/** 구매 결과. 화면 전환(완료 화면, 코인 부족 시트, 장바구니 다시 읽기)은 ViewModel이 이 값으로 정한다. */
sealed interface PurchaseOutcome {
    /** 샀다. 저장소가 잔액·장바구니 배지를 바꾸고 구매 완료를 냈다. */
    data class Completed(val result: PurchaseResultVO) : PurchaseOutcome

    /** 가격 바뀜·판매 끝남·이미 가진 방 배경·구매 한도로 막혔다. 다이얼로그를 띄웠고, 화면은 장바구니를 다시 읽는다. */
    data object Blocked : PurchaseOutcome

    /** 코인이 모자란다. 화면은 코인 부족 시트를 보이고 장바구니를 다시 읽는다. */
    data object InsufficientBalance : PurchaseOutcome

    /**
     * 결과를 모른다: 앞 구매가 처리 중, 구매 키가 엉킴, 다시 보내도 응답이 없음, 서버 오류.
     * 화면은 안내 없이 장바구니와 상단 바를 다시 읽는다(서버 오류는 공통 안내가 이미 떴다).
     */
    data object Unknown : PurchaseOutcome
}

/**
 * 장바구니에서 고른 상품을 코인으로 산다.
 *
 * 부를 때마다 구매 키를 새로 만든다. 응답을 받지 못하면(시간 초과·연결 끊김) 서버가 샀는지 모르므로 같은 키로
 * 한 번만 다시 보낸다. 이미 샀다면 서버가 처음 결과를 돌려줘서 두 번 결제되지 않는다. 새 키로 자동 재요청하지 않는다.
 * 구매를 마치면 산 개수와 차감한 코인을 분석 이벤트로 남긴다(추천 탭 배치 A/B의 주 측정항목).
 */
class PurchaseUseCase @Inject constructor(
    private val repository: ShopRepository,
    private val tracker: ShopTracker,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /** [products]는 선택됐고 살 수 있는 항목의 상품 id·수량·화면에서 본 1개 결제가다. */
    suspend operator fun invoke(products: List<PurchaseProductVO>): PurchaseOutcome {
        val idempotencyKey = UUID.randomUUID().toString()
        return try {
            val result = purchaseWithOneRetry(idempotencyKey, products)
            tracker.purchase(result)
            PurchaseOutcome.Completed(result)
        } catch (e: HttpResponseException) {
            outcomeOf(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PurchaseOutcome.Unknown
        }
    }

    private suspend fun purchaseWithOneRetry(
        idempotencyKey: String,
        products: List<PurchaseProductVO>,
    ): PurchaseResultVO = try {
        repository.purchase(idempotencyKey, products)
    } catch (e: IOException) {
        repository.purchase(idempotencyKey, products)
    }

    private fun outcomeOf(e: HttpResponseException): PurchaseOutcome {
        var outcome: PurchaseOutcome = PurchaseOutcome.Unknown
        handleHttpError<ShopErrorType>(
            e,
            onDomainError = { error ->
                outcome = when (error) {
                    ShopErrorType.INSUFFICIENT_BALANCE -> PurchaseOutcome.InsufficientBalance
                    else -> PurchaseBlockedDialog.of(error)?.let { dialog ->
                        messageHelper.showOneButtonDialog(
                            titleText = resourceHelper.getString(dialog.title),
                            descText = resourceHelper.getString(dialog.body),
                            buttonText = resourceHelper.getString(dialog.button),
                        )
                        PurchaseOutcome.Blocked
                    } ?: PurchaseOutcome.Unknown
                }
            },
            // 등록하지 않은 4xx. 사지 못했으니 일시 오류로 알리고 장바구니를 다시 읽게 한다.
            onUnknownError = { executeCommonErrorHanding(e) },
        )
        return outcome
    }
}

/** 구매가 막혔을 때의 다이얼로그(피그마 `Shop · 21c`~`21f`). */
internal enum class PurchaseBlockedDialog(val title: StringResource, val body: StringResource, val button: StringResource) {
    PRICE_CHANGED(StringResource.SHOP_PRICE_CHANGED_TITLE, StringResource.SHOP_PRICE_CHANGED_BODY, StringResource.SHOP_RELOAD_CART),
    NOT_FOR_SALE(StringResource.SHOP_NOT_FOR_SALE_TITLE, StringResource.SHOP_NOT_FOR_SALE_BODY, StringResource.CONFIRM),
    ALREADY_OWNED(StringResource.SHOP_ALREADY_OWNED_TITLE, StringResource.SHOP_ALREADY_OWNED_BODY, StringResource.CONFIRM),
    PURCHASE_LIMIT(StringResource.SHOP_PURCHASE_LIMIT_TITLE, StringResource.SHOP_PURCHASE_LIMIT_BODY, StringResource.SHOP_RELOAD_CART),
    ;

    companion object {
        /** 막힘이 아닌 오류(처리 중, 키 엉킴, 코인 부족 등)는 null이다. */
        fun of(error: ShopErrorType): PurchaseBlockedDialog? = when (error) {
            ShopErrorType.PRICE_CHANGED, ShopErrorType.CART_MISMATCH -> PRICE_CHANGED
            ShopErrorType.PRODUCT_NOT_FOR_SALE, ShopErrorType.PRODUCT_NOT_FOUND -> NOT_FOR_SALE
            ShopErrorType.ALREADY_OWNED -> ALREADY_OWNED
            ShopErrorType.PURCHASE_LIMIT_EXCEEDED -> PURCHASE_LIMIT
            else -> null
        }
    }
}
