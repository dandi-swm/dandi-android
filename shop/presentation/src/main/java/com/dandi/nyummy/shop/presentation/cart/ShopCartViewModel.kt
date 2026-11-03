package com.dandi.nyummy.shop.presentation.cart

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.DeleteCartItemsUseCase
import com.dandi.nyummy.shop.domain.GetCartUseCase
import com.dandi.nyummy.shop.domain.GetShopProductsUseCase
import com.dandi.nyummy.shop.domain.PurchaseOutcome
import com.dandi.nyummy.shop.domain.PurchaseUseCase
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.domain.UpdateCartQuantityUseCase
import com.dandi.nyummy.shop.domain.UpdateCartSelectionUseCase
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

/**
 * 장바구니 화면 상태. [items]·[selectedItemCount]·[summary]는 서버 장바구니이고, 수량·선택은 누르자마자 먼저 바꿔
 * 보인 뒤 마지막 요청의 응답으로 맞춘다.
 *
 * @property isSyncing 보낸 요청의 응답을 아직 다 받지 못했다. 화면 값이 서버와 다를 수 있다
 * @property recommendations 빈 장바구니의 인기 인테리어 추천. 불러오지 못하면 비어 있고 칸을 숨긴다
 */
data class ShopCartUIState(
    val items: ImmutableList<CartItemVO> = persistentListOf(),
    val selectedItemCount: Int = 0,
    val summary: CartSummaryVO = CartSummaryVO(),
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val isSyncing: Boolean = false,
    val recommendations: ImmutableList<ShopItemVO> = persistentListOf(),
    val detailItem: ShopItemVO? = null,
    val addingProductIds: ImmutableSet<Long> = persistentSetOf(),
    val sheet: ShopCartSheet? = null,
    val isPurchasing: Boolean = false,
    val completion: ShopPurchaseCompletion? = null,
) : UiState {
    val isEmpty: Boolean get() = !isLoading && !isLoadFailed && items.isEmpty()

    /** 이번에 살 항목: 골랐고 살 수 있다. */
    val purchasingItems: List<CartItemVO> get() = items.filter { it.isSelected && it.canPurchase }

    /** 모자란 코인. 결제할 코인이 내 코인보다 크면 양수다. */
    val shortage: Int get() = (summary.purchasePrice - summary.balance).coerceAtLeast(0)

    /** 전체 선택 체크: 살 수 있는 항목이 있고 모두 골랐다. */
    val isAllSelected: Boolean get() = items.any { it.canPurchase } && items.filter { it.canPurchase }.all { it.isSelected }
}

/** 장바구니 위에 뜨는 구매 시트. */
enum class ShopCartSheet {
    /** "아이템 n개를 살까요?"(피그마 `Shop · 21`). */
    Confirm,

    /** "코인이 n개 모자라요"(피그마 `Shop · 21b`). */
    InsufficientBalance,
}

/**
 * 구매 완료 화면(피그마 `Shop · 22`) 값.
 *
 * @property items 산 상품(그림). 구매 요청에 보낸 항목이다
 * @property quantity 산 개수 합
 * @property balance 구매 뒤 남은 코인
 */
data class ShopPurchaseCompletion(
    val items: ImmutableList<ShopItemVO>,
    val quantity: Int,
    val balance: Int,
)

sealed interface ShopCartIntent : MviIntent {
    data object Enter : ShopCartIntent
    data object Retry : ShopCartIntent
    data object ClickBack : ShopCartIntent

    /** −/+. [quantity]는 바꾼 뒤 수량이다(1 ~ 최대 수량으로 자른다). */
    data class ChangeQuantity(val productId: Long, val quantity: Int) : ShopCartIntent
    data class ToggleSelect(val productId: Long) : ShopCartIntent
    data object ToggleSelectAll : ShopCartIntent

    /** 줄의 ✕. */
    data class DeleteItem(val productId: Long) : ShopCartIntent
    data object DeleteSelected : ShopCartIntent

    /** 빈 장바구니의 "상점 구경하기". */
    data object ClickBrowseShop : ShopCartIntent

    /** 빈 장바구니 추천의 "전체보기"(인테리어 인기순). */
    data object ClickSeeAllRecommendations : ShopCartIntent
    data class ClickRecommendation(val item: ShopItemVO) : ShopCartIntent
    data object DismissDetail : ShopCartIntent
    data class AddRecommendation(val item: ShopItemVO) : ShopCartIntent

    /** 하단 "n개 구매하기". 코인이 모자라면 부족 시트, 아니면 확인 시트를 연다. */
    data object ClickPurchase : ShopCartIntent
    data object DismissSheet : ShopCartIntent

    /** 확인 시트의 "구매하기". */
    data object ConfirmPurchase : ShopCartIntent

    /** 코인 부족 시트의 "식사 기록하기". */
    data object ClickRecordMeal : ShopCartIntent

    /** 완료 화면의 "우편함 가기". */
    data object ClickGoToMailbox : ShopCartIntent

    /** 완료 화면의 "상점 더 둘러보기". */
    data object ClickMoreShopping : ShopCartIntent
}

sealed interface ShopCartReducerEvent : ReducerEvent {
    data object LoadStarted : ShopCartReducerEvent
    data class CartLoaded(val cart: CartVO) : ShopCartReducerEvent
    data object LoadFailed : ShopCartReducerEvent
    data class SyncChanged(val isSyncing: Boolean) : ShopCartReducerEvent
    data class QuantityShown(val productId: Long, val quantity: Int) : ShopCartReducerEvent
    data class SelectionShown(val productIds: Set<Long>) : ShopCartReducerEvent
    data class RecommendationsLoaded(val items: List<ShopItemVO>) : ShopCartReducerEvent
    data class DetailOpened(val item: ShopItemVO) : ShopCartReducerEvent
    data object DetailClosed : ShopCartReducerEvent
    data class AddStarted(val productId: Long) : ShopCartReducerEvent
    data class AddFinished(val productId: Long, val added: Boolean) : ShopCartReducerEvent
    data class SheetChanged(val sheet: ShopCartSheet?) : ShopCartReducerEvent
    data object PurchaseStarted : ShopCartReducerEvent

    /** 구매 요청이 끝났다. [completion]이 있으면 완료 화면을, [sheet]가 있으면 그 시트를 보인다. */
    data class PurchaseFinished(val completion: ShopPurchaseCompletion?, val sheet: ShopCartSheet?) : ShopCartReducerEvent
}

/**
 * 장바구니. 조회·수량·선택·빼기 요청을 누른 순서대로 하나씩 보내고(서버도 그 순서로 처리한다), 가장 최근 요청의 응답만
 * 화면에 반영한다. 그래서 마지막 입력이 서버와 화면 모두에 남는다. 바꾸는 요청이 실패하면 장바구니를 다시 읽고, 그것도
 * 실패하면 화면 값이 서버와 다를 수 있으니 다시 시도 안내를 보인다(수량 거절 토스트는 UseCase가 띄운다).
 * 장바구니가 비면 인기 인테리어 6개를 추천한다. 구매(확인·코인 부족 시트, 완료 화면)도 이 화면이 맡는다.
 */
@HiltViewModel
class ShopCartViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getCart: GetCartUseCase,
    private val updateCartQuantity: UpdateCartQuantityUseCase,
    private val updateCartSelection: UpdateCartSelectionUseCase,
    private val deleteCartItems: DeleteCartItemsUseCase,
    private val getShopProducts: GetShopProductsUseCase,
    private val addToCart: AddToCartUseCase,
    private val purchase: PurchaseUseCase,
) : MviViewModel<ShopCartIntent, ShopCartUIState, ShopCartReducerEvent>(ShopCartUIState()) {

    private var entered = false
    private var latestRequestId = 0
    private var pendingRequests = 0
    private val requestMutex = Mutex()

    /** 서버가 코인 부족이라고 했다. 장바구니를 다시 받은 뒤 최신 금액으로 부족 시트를 연다. */
    private var openShortageAfterReload = false

    override fun onIntent(intent: ShopCartIntent) {
        when (intent) {
            ShopCartIntent.Enter -> if (!entered) {
                entered = true
                reload()
            }
            ShopCartIntent.Retry -> reload()
            // 완료 화면에서 뒤로 가면 장바구니가 아니라 상점으로 간다.
            ShopCartIntent.ClickBack -> if (currentState.completion != null) leaveTo(ShopPage) else navigationHelper.navigateToBack()
            is ShopCartIntent.ChangeQuantity -> changeQuantity(intent.productId, intent.quantity)
            is ShopCartIntent.ToggleSelect -> toggleSelect(intent.productId)
            ShopCartIntent.ToggleSelectAll -> toggleSelectAll()
            is ShopCartIntent.DeleteItem -> mutate { deleteCartItems(listOf(intent.productId)) }
            ShopCartIntent.DeleteSelected -> deleteSelected()
            ShopCartIntent.ClickBrowseShop -> leaveTo(ShopPage)
            ShopCartIntent.ClickSeeAllRecommendations -> leaveTo(ShopPage.of(ShopTab.INTERIOR, ProductSort.POPULAR).asNewEntry())
            is ShopCartIntent.ClickRecommendation -> dispatch(ShopCartReducerEvent.DetailOpened(intent.item))
            ShopCartIntent.DismissDetail -> dispatch(ShopCartReducerEvent.DetailClosed)
            is ShopCartIntent.AddRecommendation -> addRecommendation(intent.item)
            ShopCartIntent.ClickPurchase -> openPurchaseSheet()
            ShopCartIntent.DismissSheet -> dispatch(ShopCartReducerEvent.SheetChanged(null))
            ShopCartIntent.ConfirmPurchase -> confirmPurchase()
            ShopCartIntent.ClickRecordMeal -> {
                dispatch(ShopCartReducerEvent.SheetChanged(null))
                navigationHelper.navigateTo(MealRecordPage)
            }
            ShopCartIntent.ClickGoToMailbox -> leaveTo(MailboxPage)
            ShopCartIntent.ClickMoreShopping -> leaveTo(ShopPage)
        }
    }

    private fun reload() {
        if (currentState.items.isEmpty() || currentState.isLoadFailed) dispatch(ShopCartReducerEvent.LoadStarted)
        send(request = { getCart() }, onFailure = { dispatch(ShopCartReducerEvent.LoadFailed) })
    }

    /** 장바구니를 바꾸는 요청. 실패하면 서버 값으로 되돌리려고 다시 읽는다. */
    private fun mutate(request: suspend () -> Result<CartVO>) {
        send(request = request, onFailure = { reload() })
    }

    private fun send(request: suspend () -> Result<CartVO>, onFailure: () -> Unit) {
        val requestId = ++latestRequestId
        if (pendingRequests++ == 0) dispatch(ShopCartReducerEvent.SyncChanged(true))
        viewModelScope.launch {
            // 앞 요청이 끝난 뒤 보낸다. 병렬로 보내면 서버가 다른 순서로 처리해 마지막 응답이 마지막 입력과 어긋날 수 있다.
            val result = requestMutex.withLock { request() }
            if (--pendingRequests == 0) dispatch(ShopCartReducerEvent.SyncChanged(false))
            if (requestId != latestRequestId) return@launch
            result
                .onSuccess { cart ->
                    dispatch(ShopCartReducerEvent.CartLoaded(cart))
                    if (cart.items.isEmpty()) loadRecommendations()
                    if (openShortageAfterReload) {
                        openShortageAfterReload = false
                        if (currentState.summary.balanceAfter < 0) dispatch(ShopCartReducerEvent.SheetChanged(ShopCartSheet.InsufficientBalance))
                    }
                }
                .onFailure { onFailure() }
        }
    }

    private fun changeQuantity(productId: Long, quantity: Int) {
        val item = currentState.items.firstOrNull { it.shopItem.productId == productId } ?: return
        if (!item.canPurchase) return
        val clamped = quantity.coerceIn(1, item.maxQuantity.coerceAtLeast(1))
        if (clamped == item.quantity) return
        dispatch(ShopCartReducerEvent.QuantityShown(productId, clamped))
        mutate { updateCartQuantity(productId, clamped) }
    }

    private fun toggleSelect(productId: Long) {
        val items = currentState.items
        val item = items.firstOrNull { it.shopItem.productId == productId } ?: return
        if (!item.canPurchase) return
        val selected = items.filter { it.isSelected }.map { it.shopItem.productId }.toMutableSet()
        if (!selected.remove(productId)) selected += productId
        sendSelection(selected)
    }

    private fun toggleSelectAll() {
        val state = currentState
        val purchasable = state.items.filter { it.canPurchase }.map { it.shopItem.productId }
        if (purchasable.isEmpty()) return
        sendSelection(if (state.isAllSelected) emptySet() else purchasable.toSet())
    }

    private fun sendSelection(productIds: Set<Long>) {
        dispatch(ShopCartReducerEvent.SelectionShown(productIds))
        // 서버에는 장바구니 순서대로 보낸다.
        val ordered = currentState.items.map { it.shopItem.productId }.filter { it in productIds }
        mutate { updateCartSelection(ordered) }
    }

    private fun deleteSelected() {
        val selected = currentState.items.filter { it.isSelected }.map { it.shopItem.productId }
        if (selected.isEmpty()) return
        mutate { deleteCartItems(selected) }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            val items = getShopProducts(ShopTab.INTERIOR, category = null, sort = ProductSort.POPULAR, size = RECOMMENDATION_SIZE)
                .getOrNull()?.items.orEmpty()
            dispatch(ShopCartReducerEvent.RecommendationsLoaded(items.take(RECOMMENDATION_SIZE)))
        }
    }

    private fun addRecommendation(item: ShopItemVO) {
        val productId = item.productId
        if (productId in currentState.addingProductIds) return
        dispatch(ShopCartReducerEvent.AddStarted(productId))
        viewModelScope.launch {
            val added = addToCart(productId).isSuccess
            dispatch(ShopCartReducerEvent.AddFinished(productId, added))
            if (added) reload()
        }
    }

    /** 수량·선택 요청의 응답을 다 받기 전에는 열지 않는다. 화면의 항목과 서버 요약이 어긋난 채 금액을 확인하지 않게 한다. */
    private fun openPurchaseSheet() {
        val state = currentState
        if (state.purchasingItems.isEmpty() || state.isPurchasing || state.isSyncing) return
        val sheet = if (state.summary.balanceAfter < 0) ShopCartSheet.InsufficientBalance else ShopCartSheet.Confirm
        dispatch(ShopCartReducerEvent.SheetChanged(sheet))
    }

    /**
     * 확인 시트의 "구매하기". 요청 중에는 다시 보내지 않는다. 막힘(다이얼로그는 UseCase가 띄움)·결과 모름이면 장바구니를
     * 다시 읽고, 코인이 모자라면 장바구니를 다시 읽은 뒤 최신 금액으로 부족 시트를 보인다.
     */
    private fun confirmPurchase() {
        val state = currentState
        if (state.isPurchasing || state.isSyncing) return
        val buying = state.purchasingItems
        if (buying.isEmpty()) return
        dispatch(ShopCartReducerEvent.PurchaseStarted)
        viewModelScope.launch {
            val products = buying.map {
                PurchaseProductVO(productId = it.shopItem.productId, quantity = it.quantity, purchasePrice = it.purchasePrice)
            }
            when (val outcome = purchase(products)) {
                is PurchaseOutcome.Completed -> dispatch(
                    ShopCartReducerEvent.PurchaseFinished(
                        completion = ShopPurchaseCompletion(
                            items = buying.map { it.shopItem }.toImmutableList(),
                            quantity = outcome.result.quantity,
                            balance = outcome.result.balance,
                        ),
                        sheet = null,
                    ),
                )
                PurchaseOutcome.InsufficientBalance -> {
                    // 지금 요약은 잔액이 넉넉했던 옛 값이다. 다시 받은 뒤 최신 금액으로 부족 시트를 연다(받는 동안은 대기 표시).
                    dispatch(ShopCartReducerEvent.PurchaseFinished(completion = null, sheet = null))
                    openShortageAfterReload = true
                    reload()
                }
                PurchaseOutcome.Blocked, PurchaseOutcome.Unknown -> {
                    dispatch(ShopCartReducerEvent.PurchaseFinished(completion = null, sheet = null))
                    reload()
                }
            }
        }
    }

    /** 장바구니를 닫고 상점 탭을 연다. 상점에서 뒤로 가도 이 화면으로 돌아오지 않는다. */
    private fun leaveTo(page: Page) {
        navigationHelper.navigateToBack()
        navigationHelper.navigateTo(page)
    }

    /**
     * 같은 인자의 상점 탭 엔트리가 이미 있으면 키가 같아 그 화면이 그대로 앞으로 온다(사용자가 바꾼 탭에 머문다).
     * 요청마다 다른 인자를 붙여 새 엔트리로 열어, 요청한 탭과 정렬이 늘 적용되게 한다(알림 링크의 식별자와 같은 방식).
     */
    private fun Page.asNewEntry(): Page {
        val route = toRoute().let { it.copy(args = it.args + (ARG_REQUEST_KEY to UUID.randomUUID().toString())) }
        return object : Page {
            override fun toRoute(): NavRoute = route
        }
    }

    override fun reduce(state: ShopCartUIState, event: ShopCartReducerEvent): ShopCartUIState = when (event) {
        ShopCartReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)
        is ShopCartReducerEvent.CartLoaded -> state.copy(
            items = event.cart.items.toImmutableList(),
            selectedItemCount = event.cart.selectedItemCount,
            summary = event.cart.summary,
            isLoading = false,
            isLoadFailed = false,
            recommendations = if (event.cart.items.isEmpty()) state.recommendations else persistentListOf(),
        )
        // 장바구니가 있어도 서버 값을 다시 받지 못했으면 화면 값을 믿을 수 없으니 다시 시도 안내를 보인다.
        ShopCartReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)
        is ShopCartReducerEvent.SyncChanged -> state.copy(isSyncing = event.isSyncing)
        is ShopCartReducerEvent.QuantityShown -> state.copy(
            items = state.items.map {
                if (it.shopItem.productId == event.productId) it.copy(quantity = event.quantity) else it
            }.toImmutableList(),
        )
        is ShopCartReducerEvent.SelectionShown -> state.copy(
            items = state.items.map { it.copy(isSelected = it.shopItem.productId in event.productIds) }.toImmutableList(),
            selectedItemCount = event.productIds.size,
        )
        is ShopCartReducerEvent.RecommendationsLoaded -> state.copy(recommendations = event.items.toImmutableList())
        is ShopCartReducerEvent.DetailOpened -> state.copy(detailItem = event.item)
        ShopCartReducerEvent.DetailClosed -> state.copy(detailItem = null)
        is ShopCartReducerEvent.AddStarted ->
            state.copy(addingProductIds = (state.addingProductIds + event.productId).toPersistentSet())
        is ShopCartReducerEvent.AddFinished -> state.copy(
            addingProductIds = (state.addingProductIds - event.productId).toPersistentSet(),
            detailItem = if (event.added) null else state.detailItem,
        )
        is ShopCartReducerEvent.SheetChanged -> state.copy(sheet = event.sheet)
        ShopCartReducerEvent.PurchaseStarted -> state.copy(isPurchasing = true)
        is ShopCartReducerEvent.PurchaseFinished -> state.copy(
            isPurchasing = false,
            sheet = event.sheet,
            completion = event.completion,
        )
    }

    companion object {
        /** 빈 장바구니 추천 개수(3열 2줄). */
        const val RECOMMENDATION_SIZE = 6

        /** 상점 탭을 새 엔트리로 열려고 붙이는 인자. 화면은 읽지 않는다. */
        private const val ARG_REQUEST_KEY = "request"
    }
}
