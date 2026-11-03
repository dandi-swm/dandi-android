package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 상점 API를 흉내 내는 저장소. [error]가 있으면 요청이 그 예외를 던진다.
 * [purchaseErrors]가 있으면 구매 요청은 순서대로 그 예외를 던지고, 다 쓰면 [purchaseResult]를 돌려준다.
 */
internal class FakeShopRepository(
    var error: Exception? = null,
    var promotionPage: ShopPromotionPageVO = ShopPromotionPageVO(),
    var productsPage: ShopItemPageVO = ShopItemPageVO(),
    var added: AddedCartItemVO = AddedCartItemVO(),
    var cart: CartVO = CartVO(),
    var purchaseResult: PurchaseResultVO = PurchaseResultVO(),
) : ShopRepository {
    private val summaryState = MutableStateFlow(ShopSummaryVO())
    override val summary: StateFlow<ShopSummaryVO> = summaryState
    override val purchaseCompleted: Flow<Unit> = MutableSharedFlow()

    val addRequests = mutableListOf<Pair<Long, Int>>()
    val productRequests = mutableListOf<List<Any?>>()
    val purchaseRequests = mutableListOf<Pair<String, List<PurchaseProductVO>>>()
    val purchaseErrors = ArrayDeque<Exception>()

    override suspend fun refreshSummary(): ShopSummaryVO {
        error?.let { throw it }
        return summaryState.value
    }

    override suspend fun getRecommendPage(): ShopRecommendPageVO {
        error?.let { throw it }
        return ShopRecommendPageVO()
    }

    override suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO {
        productRequests += listOf(tab, category, sort, cursor, size)
        error?.let { throw it }
        return productsPage
    }

    override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): ShopPromotionPageVO {
        error?.let { throw it }
        return promotionPage
    }

    override suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO {
        addRequests += productId to quantity
        error?.let { throw it }
        return added
    }

    override suspend fun getCart(): CartVO {
        error?.let { throw it }
        return cart
    }

    override suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO {
        error?.let { throw it }
        return cart
    }

    override suspend fun updateCartSelection(productIds: List<Long>): CartVO {
        error?.let { throw it }
        return cart
    }

    override suspend fun deleteCartItems(productIds: List<Long>): CartVO {
        error?.let { throw it }
        return cart
    }

    override suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>): PurchaseResultVO {
        purchaseRequests += idempotencyKey to products
        purchaseErrors.removeFirstOrNull()?.let { throw it }
        error?.let { throw it }
        return purchaseResult
    }

    override suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO {
        error?.let { throw it }
        return CoinHistoryPageVO()
    }
}

internal class RecordingMessageHelper : MessageHelper {
    data class DialogCall(val titleText: String?, val descText: String, val buttonText: String)

    val toasts = mutableListOf<String>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }

    override fun showToast(messageRes: Int) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += DialogCall(titleText, descText, buttonText)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

internal class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
    override fun navigateToBack() = Unit
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

internal object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/shop",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)
