package com.dandi.nyummy.shop.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.CoinChargeTracker
import com.dandi.nyummy.shop.domain.GetCoinChargeOfferUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopCoinChargePage
import com.dandi.nyummy.shop.domain.ShopCoinHistoryPage
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.domain.ShopTabLink
import com.dandi.nyummy.shop.domain.ShopTracker
import com.dandi.nyummy.shop.entity.CoinChargeOfferVO
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 상점 화면. 상단 바(공유 상태 구독, 화면이 보일 때 새로 읽기), 선택 탭, 서버 액션 링크, 상세 시트, 담기를 맡는다.
 * 탭 안 목록은 탭 ViewModel이 따로 들고, 상품 누름·담기 누름만 여기로 올린다.
 * 코인 충전 이벤트 대상이면 탭 줄 끝에 "코인 · Event" 탭을 두고, 선물 코인을 받기 전에만 충전 화면으로 보낸다(받은 뒤에는 준비 중 안내).
 * 받았는지는 충전 화면에서 돌아올 때 바뀌므로 화면이 보일 때마다 다시 읽는다.
 */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    observeShopSummary: ObserveShopSummaryUseCase,
    private val refreshShopSummary: RefreshShopSummaryUseCase,
    private val addToCart: AddToCartUseCase,
    private val getCoinChargeOffer: GetCoinChargeOfferUseCase,
    private val coinChargeTracker: CoinChargeTracker,
) : MviViewModel<ShopIntent, ShopUIState, ShopReducerEvent>(ShopUIState.empty) {

    private var entered = false

    /** 지금 기기의 코인 충전 이벤트. 없으면 "코인" 탭이 없다. */
    private var coinChargeOffer: CoinChargeOfferVO? = null

    init {
        viewModelScope.launch {
            observeShopSummary().collect { dispatch(ShopReducerEvent.SummaryChanged(it)) }
        }
    }

    override fun onIntent(intent: ShopIntent) {
        when (intent) {
            is ShopIntent.Enter -> enter(intent.tab, intent.sort)
            ShopIntent.ScreenResumed -> {
                viewModelScope.launch { refreshShopSummary() }
                refreshCoinChargeTab()
            }
            ShopIntent.ClickCoinChargeTab -> clickCoinChargeTab()
            is ShopIntent.SelectTab -> dispatch(ShopReducerEvent.TabSelected(intent.tab))
            ShopIntent.ClickCart -> navigationHelper.navigateTo(ShopCartPage)
            ShopIntent.ClickCoins -> navigationHelper.navigateTo(ShopCoinHistoryPage)
            is ShopIntent.ClickAction -> handleAction(intent.action)
            is ShopIntent.ClickItem -> dispatch(ShopReducerEvent.DetailOpened(intent.item))
            ShopIntent.DismissDetail -> dispatch(ShopReducerEvent.DetailClosed)
            is ShopIntent.AddToCart -> add(intent.item, intent.fromSheet)
        }
    }

    /** 라우트 인자로 처음 탭·정렬을 정한다. 화면을 다시 그려도 사용자가 고른 탭을 덮지 않게 한 번만 한다. */
    private fun enter(tab: String?, sort: String?) {
        if (entered) return
        entered = true
        val args = buildMap {
            tab?.let { put(ShopPage.ARG_TAB, it) }
            sort?.let { put(ShopPage.ARG_SORT, it) }
        }
        ShopTabLink.parse(NavRoute(ShopPage.PATH, args))?.let(::openTab)
    }

    /**
     * 서버 액션. 상점 탭 링크는 새 화면을 쌓지 않고 이 화면의 탭·정렬만 바꾼다(앱 링크로 열면 인자가 다른 상점이
     * 새로 쌓여 추천 탭 상태가 날아간다). 그 밖의 링크는 앱 링크로 연다.
     */
    private fun handleAction(action: ShopActionVO) {
        when (action) {
            is ShopActionVO.Navigate ->
                ShopTabLink.parseLink(action.link)?.let(::openTab) ?: navigationHelper.navigateToAppLink(action.link)
            is ShopActionVO.ShowToast -> messageHelper.showToast(action.message)
        }
    }

    private fun openTab(target: ShopTabLink.Target) {
        dispatch(ShopReducerEvent.TabSelected(target.tab))
        val sort = target.sort
        if (sort != null && target.tab != ShopTab.RECOMMEND) {
            dispatch(ShopReducerEvent.SortRequested(target.tab, sort))
        }
    }

    private fun refreshCoinChargeTab() {
        viewModelScope.launch {
            val offer = getCoinChargeOffer()
            coinChargeOffer = offer
            dispatch(ShopReducerEvent.CoinChargeTabChanged(offer?.let { ShopCoinChargeTab(showsEventChip = !it.isClaimed) }))
        }
    }

    private fun clickCoinChargeTab() {
        val offer = coinChargeOffer ?: return
        coinChargeTracker.tabClick(offer.event, claimed = offer.isClaimed)
        if (offer.isClaimed) {
            messageHelper.showToast(CommonR.string.nyummy_not_ready)
        } else {
            navigationHelper.navigateTo(ShopCoinChargePage.fromShopTab)
        }
    }

    private fun add(item: ShopItemVO, fromSheet: Boolean) {
        val productId = item.productId
        if (productId in currentState.addingProductIds) return
        dispatch(ShopReducerEvent.AddStarted(productId))
        viewModelScope.launch {
            val added = addToCart(productId, ShopTracker.Source.of(currentState.selectedTab)).isSuccess
            dispatch(ShopReducerEvent.AddFinished(productId, closeDetail = fromSheet && added))
        }
    }

    override fun reduce(state: ShopUIState, event: ShopReducerEvent): ShopUIState = when (event) {
        is ShopReducerEvent.SummaryChanged ->
            state.copy(balance = event.summary.balance, cartItemCount = event.summary.cartItemCount)
        is ShopReducerEvent.TabSelected -> state.copy(selectedTab = event.tab)
        is ShopReducerEvent.SortRequested -> state.copy(
            sortRequest = ShopSortRequest(event.tab, event.sort, id = (state.sortRequest?.id ?: 0) + 1),
        )
        is ShopReducerEvent.DetailOpened -> state.copy(detailItem = event.item)
        ShopReducerEvent.DetailClosed -> state.copy(detailItem = null)
        is ShopReducerEvent.AddStarted -> state.copy(addingProductIds = (state.addingProductIds + event.productId).toPersistentSet())
        is ShopReducerEvent.AddFinished -> state.copy(
            addingProductIds = (state.addingProductIds - event.productId).toPersistentSet(),
            // 늦게 온 다른 상품의 담기 응답이 지금 열린 시트를 닫지 않게, 같은 상품일 때만 닫는다.
            detailItem = if (event.closeDetail && state.detailItem?.productId == event.productId) null else state.detailItem,
        )
        is ShopReducerEvent.CoinChargeTabChanged -> state.copy(coinChargeTab = event.tab)
    }
}
