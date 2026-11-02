package com.dandi.nyummy.shop.presentation.recommend

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.shop.domain.GetShopRecommendPageUseCase
import com.dandi.nyummy.shop.entity.SduiComponentVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 추천 탭 상태. 서버가 정한 칸을 위에서부터 그린다.
 *
 * @property isLoadFailed 처음 조회가 실패했다. 다시 시도 안내를 보인다
 */
data class ShopRecommendUIState(
    val components: ImmutableList<SduiComponentVO> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
) : UiState

sealed interface ShopRecommendIntent : MviIntent {
    data object Retry : ShopRecommendIntent
}

sealed interface ShopRecommendReducerEvent : ReducerEvent {
    data object LoadStarted : ShopRecommendReducerEvent
    data class Loaded(val components: List<SduiComponentVO>) : ShopRecommendReducerEvent
    data object LoadFailed : ShopRecommendReducerEvent
}

/** 추천 탭. 처음 보일 때 SDUI 칸을 읽고, 실패하면 다시 시도로 다시 읽는다. 상품 누름·액션은 상점 화면이 맡는다. */
@HiltViewModel
class ShopRecommendViewModel @Inject constructor(
    private val getShopRecommendPage: GetShopRecommendPageUseCase,
) : MviViewModel<ShopRecommendIntent, ShopRecommendUIState, ShopRecommendReducerEvent>(ShopRecommendUIState()) {

    private var loadJob: Job? = null

    init {
        load()
    }

    override fun onIntent(intent: ShopRecommendIntent) {
        when (intent) {
            ShopRecommendIntent.Retry -> load()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        dispatch(ShopRecommendReducerEvent.LoadStarted)
        loadJob = viewModelScope.launch {
            getShopRecommendPage()
                .onSuccess { dispatch(ShopRecommendReducerEvent.Loaded(it.components)) }
                .onFailure { dispatch(ShopRecommendReducerEvent.LoadFailed) }
        }
    }

    override fun reduce(state: ShopRecommendUIState, event: ShopRecommendReducerEvent): ShopRecommendUIState =
        when (event) {
            ShopRecommendReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)
            is ShopRecommendReducerEvent.Loaded ->
                state.copy(components = event.components.toImmutableList(), isLoading = false, isLoadFailed = false)
            ShopRecommendReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)
        }
}
