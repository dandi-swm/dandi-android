package com.dandi.nyummy.shop.presentation.recommend

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.shop.domain.GetShopRecommendPageUseCase
import com.dandi.nyummy.shop.domain.ShopTracker
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.presentation.R
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
 * @property isRefreshing 당겨서 새로 고치는 중이다. 보던 칸은 그대로 두고 위에 인디케이터만 돈다
 */
data class ShopRecommendUIState(
    val components: ImmutableList<SduiComponentVO> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val isRefreshing: Boolean = false,
) : UiState

sealed interface ShopRecommendIntent : MviIntent {
    data object Retry : ShopRecommendIntent

    /** 목록을 아래로 당겼다. 칸을 다시 읽는다. */
    data object Refresh : ShopRecommendIntent

    /** [componentIndex]번째 칸의 [position]번째(0부터) 상품을 눌렀다. 분석 이벤트만 남긴다(상세 시트는 상점 화면이 연다). */
    data class ItemClicked(val componentIndex: Int, val position: Int, val productId: Long) : ShopRecommendIntent

    /** [componentIndex]번째 배너 칸의 배너를 눌렀다. 분석 이벤트만 남긴다(이동은 상점 화면이 한다). */
    data class BannerClicked(val componentIndex: Int, val action: ShopActionVO) : ShopRecommendIntent
}

sealed interface ShopRecommendReducerEvent : ReducerEvent {
    data object LoadStarted : ShopRecommendReducerEvent
    data class Loaded(val components: List<SduiComponentVO>) : ShopRecommendReducerEvent
    data object LoadFailed : ShopRecommendReducerEvent
    data object RefreshStarted : ShopRecommendReducerEvent

    /** 새로 고침이 실패했다. 보던 칸은 그대로 둔다. */
    data object RefreshFailed : ShopRecommendReducerEvent
}

/**
 * 추천 탭. 처음 보일 때 SDUI 칸을 읽고, 실패하면 다시 시도로 다시 읽는다. 목록을 당기면 보던 칸을 둔 채 다시 읽는다.
 * 상품 누름·액션은 상점 화면이 맡는다.
 * 배치 A/B 측정을 위해 칸을 받아 그린 것과 칸별 누름(칸 종류, 몇 번째 칸, 칸 안 몇 번째)을 분석 이벤트로 남긴다.
 */
@HiltViewModel
class ShopRecommendViewModel @Inject constructor(
    private val getShopRecommendPage: GetShopRecommendPageUseCase,
    private val tracker: ShopTracker,
    private val messageHelper: MessageHelper,
) : MviViewModel<ShopRecommendIntent, ShopRecommendUIState, ShopRecommendReducerEvent>(ShopRecommendUIState()) {

    private var loadJob: Job? = null

    /** 지금 그린 추천 탭. 누름 이벤트에 배치 그룹과 칸 정보를 붙이는 데 쓴다. */
    private var page: ShopRecommendPageVO? = null

    init {
        load()
    }

    override fun onIntent(intent: ShopRecommendIntent) {
        when (intent) {
            ShopRecommendIntent.Retry -> load()
            ShopRecommendIntent.Refresh -> refresh()
            is ShopRecommendIntent.ItemClicked -> page?.let {
                tracker.recommendItemClick(it, intent.componentIndex, intent.position, productId = intent.productId, bannerId = null)
            }
            is ShopRecommendIntent.BannerClicked -> page?.let { page ->
                val banners = (page.components.getOrNull(intent.componentIndex) as? SduiComponentVO.HeroBanner)?.banners.orEmpty()
                val position = banners.indexOfFirst { it.action == intent.action }
                if (position >= 0) {
                    tracker.recommendItemClick(page, intent.componentIndex, position, productId = null, bannerId = banners[position].bannerId)
                }
            }
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        dispatch(ShopRecommendReducerEvent.LoadStarted)
        loadJob = viewModelScope.launch {
            getShopRecommendPage()
                .onSuccess {
                    page = it
                    dispatch(ShopRecommendReducerEvent.Loaded(it.components))
                    tracker.recommendView(it)
                }
                .onFailure { dispatch(ShopRecommendReducerEvent.LoadFailed) }
        }
    }

    /**
     * 당겨서 새로 고침. 보던 칸이 없으면(처음 조회 중·실패) 처음 조회와 같다. 노출 이벤트는 탭에 들어올 때만 남기고
     * 새로 고침으로는 다시 남기지 않는다. 실패하면 보던 칸을 두고, 공통 오류 안내(UseCase)가 없었을 때만 토스트로 알린다.
     */
    private fun refresh() {
        if (loadJob?.isActive == true) return
        if (currentState.components.isEmpty()) return load()
        dispatch(ShopRecommendReducerEvent.RefreshStarted)
        loadJob = viewModelScope.launch {
            getShopRecommendPage()
                .onSuccess {
                    page = it
                    dispatch(ShopRecommendReducerEvent.Loaded(it.components))
                }
                .onFailure { e ->
                    dispatch(ShopRecommendReducerEvent.RefreshFailed)
                    if ((e as? HttpResponseException)?.isCommonErrorHandling() != true) {
                        messageHelper.showToast(R.string.shop_refresh_failed)
                    }
                }
        }
    }

    override fun reduce(state: ShopRecommendUIState, event: ShopRecommendReducerEvent): ShopRecommendUIState =
        when (event) {
            ShopRecommendReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)
            is ShopRecommendReducerEvent.Loaded -> state.copy(
                components = event.components.toImmutableList(),
                isLoading = false,
                isLoadFailed = false,
                isRefreshing = false,
            )
            ShopRecommendReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true, isRefreshing = false)
            ShopRecommendReducerEvent.RefreshStarted -> state.copy(isRefreshing = true)
            ShopRecommendReducerEvent.RefreshFailed -> state.copy(isRefreshing = false)
        }
}
