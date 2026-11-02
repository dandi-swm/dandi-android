package com.dandi.nyummy.shop.presentation

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
import com.dandi.nyummy.shop.domain.ShopRepository
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

    override suspend fun getRecommendPage(): ShopRecommendPageVO = ShopRecommendPageVO()

    override suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO = ShopItemPageVO()

    override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int) = ShopPromotionPageVO()

    override suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO {
        addRequests += productId
        addGate?.await()
        error?.let { throw it }
        return AddedCartItemVO(productId = productId, quantity = quantity)
    }

    override suspend fun getCart(): CartVO = CartVO()
    override suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO = CartVO()
    override suspend fun updateCartSelection(productIds: List<Long>): CartVO = CartVO()
    override suspend fun deleteCartItems(productIds: List<Long>): CartVO = CartVO()
    override suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>) = PurchaseResultVO()
    override suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO = CoinHistoryPageVO()
}

internal class RecordingMessageHelper : MessageHelper {
    val toasts = mutableListOf<String>()
    val dialogs = mutableListOf<String>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }

    override fun showToast(messageRes: Int) = Unit
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

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateToAppLink(link: String, asRoot: Boolean) {
        appLinks += link
    }

    override fun navigateToBack() {
        backCount++
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
