package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
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
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.shop.domain.ShopRepository
import com.dandi.nyummy.shop.domain.ShopTracker
import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 상점 저장소 가짜. [error]가 있으면 요청이 그 예외를 던진다. [addGate]가 있으면 담기가 그 값이 끝날 때까지 기다린다.
 */
internal class FakeShopRepository : ShopRepository {
    val summaryState = MutableStateFlow(ShopSummaryVO())
    override val summary: StateFlow<ShopSummaryVO> = summaryState
    val purchaseCompletedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val purchaseCompleted: Flow<Unit> = purchaseCompletedEvents

    var error: Exception? = null
    var addGate: CompletableDeferred<Unit>? = null
    var refreshCount = 0
    val addRequests = mutableListOf<Long>()

    override suspend fun refreshSummary(): ShopSummaryVO {
        refreshCount++
        error?.let { throw it }
        return summaryState.value
    }

    var recommendPage = ShopRecommendPageVO()
    /** 추천 탭 조회만 실패시킨다. 꺼내 쓰면 비워진다(다음 조회는 성공). */
    var recommendError: Exception? = null
    var recommendCount = 0

    /** 추천 탭을 읽을 때 보낸 배치 그룹. */
    val recommendVariants = mutableListOf<String>()

    override suspend fun getRecommendPage(layoutVariant: String): ShopRecommendPageVO {
        recommendCount++
        recommendVariants += layoutVariant
        recommendError?.let { recommendError = null; throw it }
        return recommendPage.copy(layoutVariant = layoutVariant)
    }

    data class ProductRequest(val tab: ShopTab, val category: ShopCategory?, val sort: ProductSort, val cursor: Long?)

    val productRequests = mutableListOf<ProductRequest>()

    /** 상품 목록 요청마다 돌려줄 묶음. 기본은 빈 마지막 묶음이다. */
    var productsHandler: suspend (ProductRequest) -> ShopItemPageVO = { ShopItemPageVO() }

    override suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO {
        val request = ProductRequest(tab, category, sort, cursor)
        productRequests += request
        return productsHandler(request)
    }

    val promotionRequests = mutableListOf<Pair<Long, Long?>>()

    /** 프로모션 요청마다 돌려줄 묶음. 예외를 던지면 그 요청이 실패한다. */
    var promotionHandler: suspend (promotionId: Long, cursor: Long?) -> ShopPromotionPageVO = { _, _ -> ShopPromotionPageVO() }

    override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): ShopPromotionPageVO {
        promotionRequests += promotionId to cursor
        return promotionHandler(promotionId, cursor)
    }

    override suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO {
        addRequests += productId
        addGate?.await()
        error?.let { throw it }
        return AddedCartItemVO(productId = productId, quantity = quantity)
    }

    /** 장바구니 요청 한 번. */
    sealed interface CartCall {
        data object Get : CartCall
        data class Quantity(val productId: Long, val quantity: Int) : CartCall
        data class Selection(val productIds: List<Long>) : CartCall
        data class Delete(val productIds: List<Long>) : CartCall
    }

    val cartCalls = mutableListOf<CartCall>()

    /** 장바구니 요청마다 돌려줄 장바구니. 예외를 던지면 그 요청이 실패한다. */
    var cartHandler: suspend (CartCall) -> CartVO = { CartVO() }

    private suspend fun cart(call: CartCall): CartVO {
        cartCalls += call
        return cartHandler(call)
    }

    override suspend fun getCart(): CartVO = cart(CartCall.Get)
    override suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO = cart(CartCall.Quantity(productId, quantity))
    override suspend fun updateCartSelection(productIds: List<Long>): CartVO = cart(CartCall.Selection(productIds))
    override suspend fun deleteCartItems(productIds: List<Long>): CartVO = cart(CartCall.Delete(productIds))
    val purchaseRequests = mutableListOf<List<PurchaseProductVO>>()

    /** 구매 요청마다 돌려줄 결과. 예외를 던지면 그 요청이 실패한다. */
    var purchaseHandler: suspend (List<PurchaseProductVO>) -> PurchaseResultVO = { PurchaseResultVO() }

    override suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>): PurchaseResultVO {
        purchaseRequests += products
        return purchaseHandler(products)
    }
    val coinHistoryRequests = mutableListOf<Long?>()

    /** 코인 내역 요청마다 돌려줄 묶음. 예외를 던지면 그 요청이 실패한다. */
    var coinHistoryHandler: suspend (cursor: Long?) -> CoinHistoryPageVO = { CoinHistoryPageVO() }

    override suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO {
        coinHistoryRequests += cursor
        return coinHistoryHandler(cursor)
    }
}

internal class RecordingMessageHelper : MessageHelper {
    val toasts = mutableListOf<String>()
    val dialogs = mutableListOf<String>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }

    val toastResIds = mutableListOf<Int>()
    override fun showToast(messageRes: Int) {
        toastResIds += messageRes
    }
    override fun showSnackBar(iconType: IconType, messageText: String, callToActionText: String?, onClickCTA: (() -> Unit)?) = Unit
    override fun showSnackBar(iconType: IconType, messageRes: Int, callToActionText: String?, onClickCTA: (() -> Unit)?) = Unit
    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += descText
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
    val pages = mutableListOf<Page>()
    val appLinks = mutableListOf<String>()
    var backCount = 0

    /** 뒤로 가기와 화면 이동을 부른 순서대로 남긴다. 뒤로 가기는 [Back]이다. */
    val events = mutableListOf<Any>()

    object Back

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
        events += page
    }

    override fun navigateToAppLink(link: String, asRoot: Boolean) {
        appLinks += link
    }

    override fun navigateToBack() {
        backCount++
        events += Back
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
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

internal fun shopItem(productId: Long, category: String = "BED") = ShopItemVO(
    productId = productId,
    itemId = productId + 100,
    name = "상품 $productId",
    category = category,
)

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/shop",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)

/** 남긴 분석 이벤트와 사용자 속성을 모은다. */
internal class RecordingShopAnalytics : AnalyticsHelper {
    val events = mutableListOf<AnalyticsEvent>()
    val userProperties = mutableListOf<Pair<String, String?>>()

    override fun logScreenView(screenName: String) = Unit
    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties += name to value
    }
}

internal fun shopTracker(analytics: AnalyticsHelper = RecordingShopAnalytics()) = ShopTracker(analytics)

/** 원격 구성 값. [values]에 없는 키는 기본값이다. */
internal class FakeRemoteConfig(private val values: Map<RemoteConfigKey<*>, Any> = emptyMap()) : RemoteConfigHelper {
    override suspend fun sync() = Unit

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: RemoteConfigKey<T>): T = (values[key] as T?) ?: key.defaultValue
}
