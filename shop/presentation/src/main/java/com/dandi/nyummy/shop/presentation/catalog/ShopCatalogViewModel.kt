package com.dandi.nyummy.shop.presentation.catalog

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.shop.domain.GetShopProductsUseCase
import com.dandi.nyummy.shop.domain.ObservePurchaseCompletedUseCase
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.ShopSortRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 인테리어·방 배경 탭 목록 상태.
 *
 * @property items 받은 상품 전체(서버 순서)
 * @property visibleItems 화면에 보이는 상품. 보유 숨기기가 켜져 있으면 보유 상품을 뺀다
 * @property nextCursor 다음 묶음 커서. null이면 마지막 묶음까지 받았다
 * @property isLoading 첫 묶음을 읽는 중
 * @property isLoadingMore 다음 묶음을 읽는 중
 * @property isLoadFailed 첫 묶음을 읽지 못했다(다시 시도 안내)
 */
data class ShopCatalogUIState(
    val tab: ShopTab = ShopTab.INTERIOR,
    val category: ShopCategory? = null,
    val sort: ProductSort = ProductSort.Default,
    val hideOwned: Boolean = false,
    val items: ImmutableList<ShopItemVO> = persistentListOf(),
    val visibleItems: ImmutableList<ShopItemVO> = persistentListOf(),
    val nextCursor: Long? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoadFailed: Boolean = false,
    val isSortSheetOpen: Boolean = false,
) : UiState {
    val hasNext: Boolean get() = nextCursor != null
}

sealed interface ShopCatalogIntent : MviIntent {
    /** 탭이 처음 보였다. 한 번만 반영한다. */
    data class Enter(val tab: ShopTab) : ShopCatalogIntent

    /** 칩. null은 "전체"다(인테리어만). */
    data class SelectCategory(val category: ShopCategory?) : ShopCatalogIntent
    data object OpenSortSheet : ShopCatalogIntent
    data object DismissSortSheet : ShopCatalogIntent
    data class SelectSort(val sort: ProductSort) : ShopCatalogIntent

    /** 링크로 받은 정렬(`/shop/interior?sort=POPULAR`). 같은 요청은 한 번만 반영한다. */
    data class ApplySortRequest(val request: ShopSortRequest) : ShopCatalogIntent

    /** 칩·정렬을 처음으로 되돌린다(바깥에서 상점에 다시 들어옴). 같은 [id]는 한 번만 반영한다. */
    data class ResetConditions(val id: Int) : ShopCatalogIntent
    data object ToggleHideOwned : ShopCatalogIntent
    data object LoadMore : ShopCatalogIntent
    data object Retry : ShopCatalogIntent
}

sealed interface ShopCatalogReducerEvent : ReducerEvent {
    data class Entered(val tab: ShopTab) : ShopCatalogReducerEvent
    data class ConditionChanged(val category: ShopCategory?, val sort: ProductSort) : ShopCatalogReducerEvent
    data object FirstPageStarted : ShopCatalogReducerEvent
    data object NextPageStarted : ShopCatalogReducerEvent
    data class PageLoaded(val page: ShopItemPageVO, val append: Boolean) : ShopCatalogReducerEvent

    /** [firstPage]면 다시 시도 안내를 보이고, 다음 묶음이면 읽기만 멈춘다(끝에 닿으면 다시 부른다). */
    data class LoadFailed(val firstPage: Boolean) : ShopCatalogReducerEvent
    data class SortSheetChanged(val isOpen: Boolean) : ShopCatalogReducerEvent
    data class HideOwnedChanged(val hideOwned: Boolean) : ShopCatalogReducerEvent
}

/**
 * 인테리어·방 배경 탭 목록. 탭마다 한 인스턴스다.
 *
 * - 칩·정렬이 바뀌면 진행 중인 조회를 취소하고 처음부터 다시 읽는다. 요청 세대 번호로 늦게 온 이전 조건 응답을 버린다.
 * - 보유 숨기기는 받은 상품을 화면에서 거른다. 한 번의 트리거(첫 조회, 끝에 닿음, 숨기기 켬)로 새로 보이게 된 상품이
 *   [MIN_VISIBLE_PER_TRIGGER]개보다 적고 다음 묶음이 있으면 [MAX_PAGES_PER_TRIGGER]묶음까지 이어 받는다.
 * - 구매가 끝나면 산 상품이 "n개 보유"로 보이게 처음부터 다시 읽는다.
 */
@HiltViewModel
class ShopCatalogViewModel @Inject constructor(
    private val getShopProducts: GetShopProductsUseCase,
    observePurchaseCompleted: ObservePurchaseCompletedUseCase,
) : MviViewModel<ShopCatalogIntent, ShopCatalogUIState, ShopCatalogReducerEvent>(ShopCatalogUIState()) {

    private var entered = false
    private var generation = 0
    private var loadJob: Job? = null
    private var lastSortRequestId = 0
    private var lastConditionResetId = 0

    init {
        viewModelScope.launch {
            observePurchaseCompleted().collect { if (entered) reload() }
        }
    }

    override fun onIntent(intent: ShopCatalogIntent) {
        when (intent) {
            is ShopCatalogIntent.Enter -> enter(intent.tab)
            is ShopCatalogIntent.SelectCategory -> changeCondition(intent.category, currentState.sort)
            ShopCatalogIntent.OpenSortSheet -> dispatch(ShopCatalogReducerEvent.SortSheetChanged(true))
            ShopCatalogIntent.DismissSortSheet -> dispatch(ShopCatalogReducerEvent.SortSheetChanged(false))
            is ShopCatalogIntent.SelectSort -> {
                dispatch(ShopCatalogReducerEvent.SortSheetChanged(false))
                changeCondition(currentState.category, intent.sort)
            }
            is ShopCatalogIntent.ApplySortRequest -> applySortRequest(intent.request)
            is ShopCatalogIntent.ResetConditions -> resetConditions(intent.id)
            ShopCatalogIntent.ToggleHideOwned -> toggleHideOwned()
            ShopCatalogIntent.LoadMore -> loadMore()
            ShopCatalogIntent.Retry -> reload()
        }
    }

    private fun enter(tab: ShopTab) {
        if (entered) return
        entered = true
        dispatch(ShopCatalogReducerEvent.Entered(tab))
        reload()
    }

    private fun applySortRequest(request: ShopSortRequest) {
        if (request.id == lastSortRequestId) return
        lastSortRequestId = request.id
        if (request.tab != currentState.tab) return
        changeCondition(currentState.category, request.sort, force = true)
    }

    /** 칩·정렬이 이미 처음 그대로면 받아 둔 목록을 둔다. */
    private fun resetConditions(id: Int) {
        if (id == lastConditionResetId) return
        lastConditionResetId = id
        changeCondition(category = null, sort = ProductSort.Default)
    }

    private fun changeCondition(category: ShopCategory?, sort: ProductSort, force: Boolean = false) {
        val state = currentState
        val safeCategory = if (state.tab == ShopTab.INTERIOR) category else null
        if (!force && safeCategory == state.category && sort == state.sort) return
        dispatch(ShopCatalogReducerEvent.ConditionChanged(safeCategory, sort))
        if (entered) reload()
    }

    private fun toggleHideOwned() {
        val hideOwned = !currentState.hideOwned
        dispatch(ShopCatalogReducerEvent.HideOwnedChanged(hideOwned))
        if (hideOwned && loadJob?.isActive != true) fillVisible()
    }

    /** 지금 조건으로 처음부터 다시 읽는다. 진행 중인 조회는 취소하고, 늦게 와도 버린다. */
    private fun reload() {
        loadJob?.cancel()
        val token = ++generation
        dispatch(ShopCatalogReducerEvent.FirstPageStarted)
        loadJob = viewModelScope.launch { fetch(token, cursor = null) }
    }

    private fun loadMore() {
        val state = currentState
        if (loadJob?.isActive == true || !state.hasNext || state.isLoading) return
        dispatch(ShopCatalogReducerEvent.NextPageStarted)
        val token = generation
        loadJob = viewModelScope.launch { fetch(token, state.nextCursor) }
    }

    /** 보유 숨기기를 켰을 때 화면이 거의 비면 다음 묶음을 이어 받는다. */
    private fun fillVisible() {
        val state = currentState
        if (state.visibleItems.size >= MIN_VISIBLE_PER_TRIGGER || !state.hasNext || state.isLoading) return
        dispatch(ShopCatalogReducerEvent.NextPageStarted)
        val token = generation
        loadJob = viewModelScope.launch { fetch(token, state.nextCursor, alreadyVisible = state.visibleItems.size) }
    }

    /**
     * [cursor]부터 한 묶음을 읽고, 보유 숨기기로 이번 트리거에 새로 보인 상품이 모자라면 정해진 묶음 수까지 더 읽는다.
     * [token]이 지금 세대와 다르면(조건이 바뀌었으면) 받은 것을 버린다.
     */
    private suspend fun fetch(token: Int, cursor: Long?, alreadyVisible: Int = 0) {
        val firstPage = cursor == null
        val visibleBefore = if (firstPage) 0 else currentState.visibleItems.size - alreadyVisible
        var next = cursor
        repeat(MAX_PAGES_PER_TRIGGER) { fetched ->
            val state = currentState
            val result = getShopProducts(state.tab, state.category, state.sort, next)
            if (token != generation) return
            val page = result.getOrElse {
                dispatch(ShopCatalogReducerEvent.LoadFailed(firstPage = firstPage && fetched == 0))
                return
            }
            dispatch(ShopCatalogReducerEvent.PageLoaded(page, append = !(firstPage && fetched == 0)))
            val now = currentState
            val newlyVisible = now.visibleItems.size - visibleBefore
            if (!now.hideOwned || newlyVisible >= MIN_VISIBLE_PER_TRIGGER || !now.hasNext) return
            if (fetched == MAX_PAGES_PER_TRIGGER - 1) return
            next = now.nextCursor
            // 이어 받는 동안 대기 표시를 유지한다. 보이는 상품이 아직 없을 때 헤더 아래가 비지 않게 한다.
            dispatch(ShopCatalogReducerEvent.NextPageStarted)
        }
    }

    override fun reduce(state: ShopCatalogUIState, event: ShopCatalogReducerEvent): ShopCatalogUIState = when (event) {
        is ShopCatalogReducerEvent.Entered -> state.copy(tab = event.tab)
        is ShopCatalogReducerEvent.ConditionChanged -> state.copy(category = event.category, sort = event.sort)
        ShopCatalogReducerEvent.FirstPageStarted -> state.copy(
            items = persistentListOf(),
            visibleItems = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            isLoadFailed = false,
        )
        ShopCatalogReducerEvent.NextPageStarted -> state.copy(isLoadingMore = true)
        is ShopCatalogReducerEvent.PageLoaded -> {
            val items = (if (event.append) state.items + event.page.items else event.page.items).toImmutableList()
            state.copy(
                items = items,
                visibleItems = items.visibleFor(state.hideOwned),
                nextCursor = event.page.nextCursor,
                isLoading = false,
                isLoadingMore = false,
                isLoadFailed = false,
            )
        }
        is ShopCatalogReducerEvent.LoadFailed -> state.copy(
            isLoading = false,
            isLoadingMore = false,
            isLoadFailed = event.firstPage,
        )
        is ShopCatalogReducerEvent.SortSheetChanged -> state.copy(isSortSheetOpen = event.isOpen)
        is ShopCatalogReducerEvent.HideOwnedChanged -> state.copy(
            hideOwned = event.hideOwned,
            visibleItems = state.items.visibleFor(event.hideOwned),
        )
    }

    private fun List<ShopItemVO>.visibleFor(hideOwned: Boolean): ImmutableList<ShopItemVO> =
        (if (hideOwned) filterNot { it.isOwned } else this).toImmutableList()

    companion object {
        /** 보유 숨기기로 한 번의 트리거에 이만큼은 새로 보이게 이어 받는다(3열 4줄). */
        const val MIN_VISIBLE_PER_TRIGGER = 12

        /** 한 번의 트리거에 받는 최대 묶음 수. 보유가 많아도 요청이 끝없이 늘지 않게 한다. */
        const val MAX_PAGES_PER_TRIGGER = 3
    }
}
