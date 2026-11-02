package com.dandi.nyummy.shop.presentation.coin

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.shop.domain.GetCoinHistoriesUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.CoinHistoryVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 코인 내역 목록의 한 칸: 날짜 머리글이거나 내역 한 줄. */
sealed interface CoinHistoryEntry {
    data class DateHeader(val date: LocalDate) : CoinHistoryEntry
    data class Row(val history: CoinHistoryVO) : CoinHistoryEntry
}

/**
 * 받은 내역 전체를 기기 타임존 날짜로 묶어 머리글을 끼운다. 묶음 경계에 같은 날짜가 걸쳐도 머리글은 한 번만 나온다
 * (받은 줄 전체로 매번 다시 계산하므로). 서버가 최신순으로 주므로 순서는 그대로 둔다.
 */
fun coinHistoryEntries(histories: List<CoinHistoryVO>): List<CoinHistoryEntry> = buildList {
    var lastDate: LocalDate? = null
    histories.forEach { history ->
        val date = history.createdAt.toLocalDate()
        if (date != lastDate) {
            add(CoinHistoryEntry.DateHeader(date))
            lastDate = date
        }
        add(CoinHistoryEntry.Row(history))
    }
}

/**
 * 코인 내역 화면 상태.
 *
 * @property balance 내 코인(공유 상단 바 상태)
 * @property entries 날짜 머리글이 끼워진 내역
 */
data class ShopCoinHistoryUIState(
    val balance: Int = 0,
    val histories: ImmutableList<CoinHistoryVO> = persistentListOf(),
    val entries: ImmutableList<CoinHistoryEntry> = persistentListOf(),
    val nextCursor: Long? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoadFailed: Boolean = false,
) : UiState {
    val hasNext: Boolean get() = nextCursor != null
    val isEmpty: Boolean get() = !isLoading && !isLoadFailed && histories.isEmpty()
}

sealed interface ShopCoinHistoryIntent : MviIntent {
    data object Enter : ShopCoinHistoryIntent
    data object LoadMore : ShopCoinHistoryIntent
    data object Retry : ShopCoinHistoryIntent
    data object ClickBack : ShopCoinHistoryIntent

    /** 빈 화면의 "식사 기록하기". */
    data object ClickRecordMeal : ShopCoinHistoryIntent
}

sealed interface ShopCoinHistoryReducerEvent : ReducerEvent {
    data class BalanceChanged(val balance: Int) : ShopCoinHistoryReducerEvent
    data object FirstPageStarted : ShopCoinHistoryReducerEvent
    data object NextPageStarted : ShopCoinHistoryReducerEvent
    data class PageLoaded(val page: CoinHistoryPageVO, val append: Boolean) : ShopCoinHistoryReducerEvent
    data class LoadFailed(val firstPage: Boolean) : ShopCoinHistoryReducerEvent
}

/** 코인 내역. 내 코인은 공유 상단 바 상태를 따르고 들어올 때 새로 읽는다. 내역은 끝에 닿으면 이어 받는다. */
@HiltViewModel
class ShopCoinHistoryViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getCoinHistories: GetCoinHistoriesUseCase,
    observeShopSummary: ObserveShopSummaryUseCase,
    private val refreshShopSummary: RefreshShopSummaryUseCase,
) : MviViewModel<ShopCoinHistoryIntent, ShopCoinHistoryUIState, ShopCoinHistoryReducerEvent>(ShopCoinHistoryUIState()) {

    private var entered = false
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeShopSummary().collect { dispatch(ShopCoinHistoryReducerEvent.BalanceChanged(it.balance)) }
        }
    }

    override fun onIntent(intent: ShopCoinHistoryIntent) {
        when (intent) {
            ShopCoinHistoryIntent.Enter -> if (!entered) {
                entered = true
                viewModelScope.launch { refreshShopSummary() }
                loadFirst()
            }
            ShopCoinHistoryIntent.LoadMore -> loadMore()
            ShopCoinHistoryIntent.Retry -> loadFirst()
            ShopCoinHistoryIntent.ClickBack -> navigationHelper.navigateToBack()
            ShopCoinHistoryIntent.ClickRecordMeal -> navigationHelper.navigateTo(MealRecordPage)
        }
    }

    private fun loadFirst() {
        loadJob?.cancel()
        dispatch(ShopCoinHistoryReducerEvent.FirstPageStarted)
        loadJob = viewModelScope.launch { fetch(cursor = null) }
    }

    private fun loadMore() {
        val state = currentState
        if (loadJob?.isActive == true || !state.hasNext || state.isLoading) return
        dispatch(ShopCoinHistoryReducerEvent.NextPageStarted)
        loadJob = viewModelScope.launch { fetch(state.nextCursor) }
    }

    private suspend fun fetch(cursor: Long?) {
        getCoinHistories(cursor)
            .onSuccess { dispatch(ShopCoinHistoryReducerEvent.PageLoaded(it, append = cursor != null)) }
            .onFailure { dispatch(ShopCoinHistoryReducerEvent.LoadFailed(firstPage = cursor == null)) }
    }

    override fun reduce(state: ShopCoinHistoryUIState, event: ShopCoinHistoryReducerEvent): ShopCoinHistoryUIState =
        when (event) {
            is ShopCoinHistoryReducerEvent.BalanceChanged -> state.copy(balance = event.balance)
            ShopCoinHistoryReducerEvent.FirstPageStarted -> state.copy(isLoading = true, isLoadFailed = false)
            ShopCoinHistoryReducerEvent.NextPageStarted -> state.copy(isLoadingMore = true)
            is ShopCoinHistoryReducerEvent.PageLoaded -> {
                val histories = if (event.append) state.histories + event.page.items else event.page.items
                state.copy(
                    histories = histories.toImmutableList(),
                    entries = coinHistoryEntries(histories).toImmutableList(),
                    nextCursor = event.page.nextCursor,
                    isLoading = false,
                    isLoadingMore = false,
                    isLoadFailed = false,
                )
            }
            is ShopCoinHistoryReducerEvent.LoadFailed ->
                state.copy(isLoading = false, isLoadingMore = false, isLoadFailed = event.firstPage)
        }
}
