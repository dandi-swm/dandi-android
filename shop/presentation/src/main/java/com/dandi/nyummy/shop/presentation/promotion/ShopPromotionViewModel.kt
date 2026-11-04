package com.dandi.nyummy.shop.presentation.promotion

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.GetShopPromotionUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.domain.ShopTracker
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 프로모션 화면 상태.
 *
 * @property banner 맨 위 배너(각진 모양, 누를 수 없음). 상단 바 제목도 이 배너 제목이다
 * @property isEnded 끝났거나 볼 수 없는 프로모션(`promotionNotFound`)
 * @property isLoadFailed 첫 묶음을 읽지 못했다(다시 시도 안내)
 */
data class ShopPromotionUIState(
    val banner: ShopBannerVO? = null,
    val items: ImmutableList<ShopItemVO> = persistentListOf(),
    val nextCursor: Long? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoadFailed: Boolean = false,
    val isEnded: Boolean = false,
    val cartItemCount: Int = 0,
    val detailItem: ShopItemVO? = null,
    val addingProductIds: ImmutableSet<Long> = persistentSetOf(),
) : UiState {
    val hasNext: Boolean get() = nextCursor != null
}

sealed interface ShopPromotionIntent : MviIntent {
    data class Enter(val promotionId: Long) : ShopPromotionIntent
    data object ScreenResumed : ShopPromotionIntent
    data object LoadMore : ShopPromotionIntent
    data object Retry : ShopPromotionIntent
    data object ClickBack : ShopPromotionIntent
    data object ClickCart : ShopPromotionIntent

    /** 끝난 프로모션의 "상점으로 돌아가기". */
    data object ClickGoToShop : ShopPromotionIntent
    data class ClickItem(val item: ShopItemVO) : ShopPromotionIntent
    data object DismissDetail : ShopPromotionIntent
    data class AddToCart(val item: ShopItemVO, val fromSheet: Boolean = false) : ShopPromotionIntent
}

sealed interface ShopPromotionReducerEvent : ReducerEvent {
    data object FirstPageStarted : ShopPromotionReducerEvent
    data object NextPageStarted : ShopPromotionReducerEvent
    data class PageLoaded(val page: ShopPromotionPageVO, val append: Boolean) : ShopPromotionReducerEvent
    data object Ended : ShopPromotionReducerEvent
    data class LoadFailed(val firstPage: Boolean) : ShopPromotionReducerEvent
    data class CartCountChanged(val count: Int) : ShopPromotionReducerEvent
    data class DetailOpened(val item: ShopItemVO) : ShopPromotionReducerEvent
    data object DetailClosed : ShopPromotionReducerEvent
    data class AddStarted(val productId: Long) : ShopPromotionReducerEvent
    data class AddFinished(val productId: Long, val closeDetail: Boolean) : ShopPromotionReducerEvent
}

/**
 * 프로모션 상품 모음. 첫 묶음과 이어 받기, 끝난 프로모션 안내, 줄 "담기"(늘 1개, 같은 상품 연타 막기), 상세 시트,
 * 장바구니 배지(공유 상단 바 상태)를 맡는다.
 */
@HiltViewModel
class ShopPromotionViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getShopPromotion: GetShopPromotionUseCase,
    observeShopSummary: ObserveShopSummaryUseCase,
    private val refreshShopSummary: RefreshShopSummaryUseCase,
    private val addToCart: AddToCartUseCase,
) : MviViewModel<ShopPromotionIntent, ShopPromotionUIState, ShopPromotionReducerEvent>(ShopPromotionUIState()) {

    private var promotionId: Long? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeShopSummary().collect { dispatch(ShopPromotionReducerEvent.CartCountChanged(it.cartItemCount)) }
        }
    }

    override fun onIntent(intent: ShopPromotionIntent) {
        when (intent) {
            is ShopPromotionIntent.Enter -> if (promotionId == null) {
                promotionId = intent.promotionId
                loadFirst()
            }
            ShopPromotionIntent.ScreenResumed -> viewModelScope.launch { refreshShopSummary() }
            ShopPromotionIntent.LoadMore -> loadMore()
            ShopPromotionIntent.Retry -> loadFirst()
            ShopPromotionIntent.ClickBack -> navigationHelper.navigateToBack()
            ShopPromotionIntent.ClickCart -> navigationHelper.navigateTo(ShopCartPage)
            ShopPromotionIntent.ClickGoToShop -> {
                // 끝난 프로모션은 닫고 상점 탭을 연다. 뒤로 가도 이 화면으로 돌아오지 않는다.
                navigationHelper.navigateToBack()
                navigationHelper.navigateTo(ShopPage)
            }
            is ShopPromotionIntent.ClickItem -> dispatch(ShopPromotionReducerEvent.DetailOpened(intent.item))
            ShopPromotionIntent.DismissDetail -> dispatch(ShopPromotionReducerEvent.DetailClosed)
            is ShopPromotionIntent.AddToCart -> add(intent.item, intent.fromSheet)
        }
    }

    private fun loadFirst() {
        val id = promotionId ?: return
        loadJob?.cancel()
        dispatch(ShopPromotionReducerEvent.FirstPageStarted)
        loadJob = viewModelScope.launch { fetch(id, cursor = null) }
    }

    private fun loadMore() {
        val id = promotionId ?: return
        val state = currentState
        if (loadJob?.isActive == true || !state.hasNext || state.isLoading) return
        dispatch(ShopPromotionReducerEvent.NextPageStarted)
        loadJob = viewModelScope.launch { fetch(id, state.nextCursor) }
    }

    private suspend fun fetch(id: Long, cursor: Long?) {
        val firstPage = cursor == null
        getShopPromotion(id, cursor)
            .onSuccess { page ->
                if (page == null) {
                    dispatch(ShopPromotionReducerEvent.Ended)
                } else {
                    dispatch(ShopPromotionReducerEvent.PageLoaded(page, append = !firstPage))
                }
            }
            .onFailure { dispatch(ShopPromotionReducerEvent.LoadFailed(firstPage)) }
    }

    private fun add(item: ShopItemVO, fromSheet: Boolean) {
        val productId = item.productId
        if (productId in currentState.addingProductIds) return
        dispatch(ShopPromotionReducerEvent.AddStarted(productId))
        viewModelScope.launch {
            val added = addToCart(productId, ShopTracker.Source.PROMOTION).isSuccess
            dispatch(ShopPromotionReducerEvent.AddFinished(productId, closeDetail = fromSheet && added))
        }
    }

    override fun reduce(state: ShopPromotionUIState, event: ShopPromotionReducerEvent): ShopPromotionUIState =
        when (event) {
            ShopPromotionReducerEvent.FirstPageStarted -> state.copy(isLoading = true, isLoadFailed = false, isEnded = false)
            ShopPromotionReducerEvent.NextPageStarted -> state.copy(isLoadingMore = true)
            is ShopPromotionReducerEvent.PageLoaded -> state.copy(
                banner = event.page.banner,
                items = (if (event.append) state.items + event.page.items else event.page.items).toImmutableList(),
                nextCursor = event.page.nextCursor,
                isLoading = false,
                isLoadingMore = false,
                isLoadFailed = false,
            )
            ShopPromotionReducerEvent.Ended -> state.copy(
                isEnded = true,
                isLoading = false,
                isLoadingMore = false,
                items = persistentListOf(),
                nextCursor = null,
            )
            is ShopPromotionReducerEvent.LoadFailed ->
                state.copy(isLoading = false, isLoadingMore = false, isLoadFailed = event.firstPage)
            is ShopPromotionReducerEvent.CartCountChanged -> state.copy(cartItemCount = event.count)
            is ShopPromotionReducerEvent.DetailOpened -> state.copy(detailItem = event.item)
            ShopPromotionReducerEvent.DetailClosed -> state.copy(detailItem = null)
            is ShopPromotionReducerEvent.AddStarted ->
                state.copy(addingProductIds = (state.addingProductIds + event.productId).toPersistentSet())
            is ShopPromotionReducerEvent.AddFinished -> state.copy(
                addingProductIds = (state.addingProductIds - event.productId).toPersistentSet(),
                // 늦게 온 다른 상품의 담기 응답이 지금 열린 시트를 닫지 않게, 같은 상품일 때만 닫는다.
                detailItem = if (event.closeDetail && state.detailItem?.productId == event.productId) null else state.detailItem,
            )
        }
}
