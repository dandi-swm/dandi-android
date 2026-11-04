package com.dandi.nyummy.shop.presentation.coin

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.shop.domain.ClaimCoinChargeUseCase
import com.dandi.nyummy.shop.domain.CoinChargeClaimResult
import com.dandi.nyummy.shop.domain.CoinChargeTracker
import com.dandi.nyummy.shop.domain.GetCoinChargeOfferUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopCoinChargePage as ShopCoinChargeRoute
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeOfferVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 코인 충전 화면 상태(피그마 `v2 · 40 Shop`의 `Charge · 10~15`).
 *
 * @property balance 내 코인(공유 상단 바 상태)
 * @property packages 충전 묶음(A/B 그룹마다 가격만 다르다)
 * @property isLoading 이벤트를 아직 읽지 않았다
 * @property isEnded 이벤트가 없다(끝났거나 값이 틀림, `Charge · 15`)
 * @property isClaimed 이번 회차 코인을 이미 받았다. 묶음을 누르면 시트 대신 [showsClaimedDialog]
 * @property sheetPackage 충전 확인 시트에 띄운 묶음(`Charge · 11`). null이면 시트가 없다
 * @property isPaying "결제하기"를 누르고 지급 응답을 기다린다(버튼을 막는다)
 * @property showsClaimedDialog 이미 받은 사람이 묶음을 다시 눌렀을 때 안내(`Charge · 13`)
 * @property grantedCoins 받은 뒤 화면(`Charge · 12`)에 보일 받은 코인 수. null이면 아직 받지 않았다
 */
data class ShopCoinChargeUIState(
    val balance: Int = 0,
    val packages: ImmutableList<CoinChargePackageVO> = persistentListOf(),
    val isLoading: Boolean = true,
    val isEnded: Boolean = false,
    val isClaimed: Boolean = false,
    val sheetPackage: CoinChargePackageVO? = null,
    val isPaying: Boolean = false,
    val showsClaimedDialog: Boolean = false,
    val grantedCoins: Int? = null,
) : UiState

sealed interface ShopCoinChargeIntent : MviIntent {
    /** 화면에 들어왔다(한 번). [source]는 링크의 `source` 인자다. */
    data class Enter(val source: String?) : ShopCoinChargeIntent

    /** 화면이 다시 보였다. 잔액만 다시 읽는다. */
    data object Resume : ShopCoinChargeIntent

    /** [coinPackageId] 묶음(1~3)을 눌렀다. */
    data class ClickPackage(val coinPackageId: Long) : ShopCoinChargeIntent

    /** 충전 확인 시트의 취소, 바깥 누름, 뒤로 가기. */
    data object DismissSheet : ShopCoinChargeIntent

    /** 충전 확인 시트의 "n원 결제하기". */
    data object ClickPay : ShopCoinChargeIntent

    data object DismissClaimedDialog : ShopCoinChargeIntent

    /** 상단 바 뒤로 가기와 시스템 뒤로 가기. 받은 뒤 화면이면 홈으로 간다. */
    data object ClickBack : ShopCoinChargeIntent

    /** 받은 뒤 화면의 "상점 둘러보기"와 이벤트가 끝난 화면의 "상점 가기". */
    data object ClickGoShop : ShopCoinChargeIntent

    /** 받은 뒤 화면의 "홈으로". */
    data object ClickGoHome : ShopCoinChargeIntent
}

sealed interface ShopCoinChargeReducerEvent : ReducerEvent {
    data class BalanceChanged(val balance: Int) : ShopCoinChargeReducerEvent
    data class OfferLoaded(val offer: CoinChargeOfferVO?) : ShopCoinChargeReducerEvent
    data class SheetChanged(val pack: CoinChargePackageVO?) : ShopCoinChargeReducerEvent
    data object PayStarted : ShopCoinChargeReducerEvent
    data class Granted(val coins: Int, val balance: Int?) : ShopCoinChargeReducerEvent
    data object FoundClaimed : ShopCoinChargeReducerEvent
    data object PayFailed : ShopCoinChargeReducerEvent
    data class ClaimedDialogChanged(val shows: Boolean) : ShopCoinChargeReducerEvent
}

/**
 * 코인 충전(가짜 결제 A/B 테스트). 묶음을 누르면 확인 시트, "결제하기"를 누르면 실제 결제 없이 이번 한 번만 이벤트 코인을 받고
 * 받은 뒤 화면을 보인다. 이미 받은 사람은 시트 없이 안내만 본다.
 *
 * 실험 지표라 단계마다 분석 이벤트를 남긴다: 들어옴(view), 묶음 누름, 시트 닫음, 결제하기 누름, 받은 뒤 행동, 떠남(exit).
 * 받음·못 받음은 UseCase가 남긴다. 들어옴은 화면이 만들어질 때 한 번만 남기고, 떠남은 ViewModel이 사라질 때 남긴다.
 */
@HiltViewModel
class ShopCoinChargeViewModel @Inject constructor(
    private val navigationHelper: NavigationHelper,
    private val getCoinChargeOffer: GetCoinChargeOfferUseCase,
    private val claimCoinCharge: ClaimCoinChargeUseCase,
    private val tracker: CoinChargeTracker,
    observeShopSummary: ObserveShopSummaryUseCase,
    private val refreshShopSummary: RefreshShopSummaryUseCase,
) : MviViewModel<ShopCoinChargeIntent, ShopCoinChargeUIState, ShopCoinChargeReducerEvent>(ShopCoinChargeUIState()) {

    /** 읽은 이벤트. 없으면(끝남) 분석 이벤트를 남기지 않는다. */
    private var event: CoinChargeEventVO? = null

    private var hasEntered = false

    init {
        viewModelScope.launch {
            observeShopSummary().collect { dispatch(ShopCoinChargeReducerEvent.BalanceChanged(it.balance)) }
        }
    }

    override fun onIntent(intent: ShopCoinChargeIntent) {
        when (intent) {
            is ShopCoinChargeIntent.Enter -> enter(intent.source)
            ShopCoinChargeIntent.Resume -> viewModelScope.launch { refreshShopSummary() }
            is ShopCoinChargeIntent.ClickPackage -> clickPackage(intent.coinPackageId)
            ShopCoinChargeIntent.DismissSheet -> dismissSheet()
            ShopCoinChargeIntent.ClickPay -> pay()
            ShopCoinChargeIntent.DismissClaimedDialog ->
                dispatch(ShopCoinChargeReducerEvent.ClaimedDialogChanged(shows = false))
            ShopCoinChargeIntent.ClickBack -> if (currentState.grantedCoins != null) {
                leaveResult(CoinChargeTracker.Values.ACTION_BACK)
            } else {
                navigationHelper.navigateToBack()
            }
            ShopCoinChargeIntent.ClickGoShop -> if (currentState.grantedCoins != null) {
                leaveResult(CoinChargeTracker.Values.ACTION_SHOP)
                navigationHelper.navigateTo(ShopPage)
            } else {
                navigationHelper.navigateToBack()
                navigationHelper.navigateTo(ShopPage)
            }
            // 상점 코인 탭으로 들어왔으면 뒤로 가기는 상점이라, 화면을 닫은 뒤 홈 탭을 연다.
            ShopCoinChargeIntent.ClickGoHome -> {
                leaveResult(CoinChargeTracker.Values.ACTION_HOME)
                navigationHelper.navigateTo(HomePage)
            }
        }
    }

    /** 이벤트를 읽고 들어옴을 남긴다. 화면이 다시 만들어져도(회전 등) 한 번만 한다. */
    private fun enter(source: String?) {
        if (hasEntered) return
        hasEntered = true
        viewModelScope.launch {
            val offer = getCoinChargeOffer()
            event = offer?.event
            dispatch(ShopCoinChargeReducerEvent.OfferLoaded(offer))
            if (offer != null) {
                tracker.view(
                    event = offer.event,
                    source = if (source == ShopCoinChargeRoute.SOURCE_SHOP_TAB) source else ShopCoinChargeRoute.SOURCE_LINK,
                    claimed = offer.isClaimed,
                )
            }
        }
    }

    private fun clickPackage(coinPackageId: Long) {
        val event = event ?: return
        val state = currentState
        if (state.isPaying || state.grantedCoins != null) return
        val pack = state.packages.firstOrNull { it.coinPackageId == coinPackageId } ?: return
        if (state.isClaimed) {
            tracker.repeatClick(event, pack)
            dispatch(ShopCoinChargeReducerEvent.ClaimedDialogChanged(shows = true))
            return
        }
        tracker.packageClick(event, pack)
        dispatch(ShopCoinChargeReducerEvent.SheetChanged(pack))
    }

    /** 결제 응답을 기다리는 동안에는 시트를 닫지 않는다. */
    private fun dismissSheet() {
        val event = event ?: return
        val pack = currentState.sheetPackage ?: return
        if (currentState.isPaying) return
        tracker.sheetClose(event, pack)
        dispatch(ShopCoinChargeReducerEvent.SheetChanged(null))
    }

    private fun pay() {
        val event = event ?: return
        val pack = currentState.sheetPackage ?: return
        if (currentState.isPaying) return
        tracker.payClick(event, pack)
        dispatch(ShopCoinChargeReducerEvent.PayStarted)
        viewModelScope.launch {
            when (val result = claimCoinCharge(event, pack)) {
                is CoinChargeClaimResult.Granted ->
                    dispatch(ShopCoinChargeReducerEvent.Granted(result.grant.grantedCoins, result.grant.balance))
                CoinChargeClaimResult.AlreadyClaimed -> dispatch(ShopCoinChargeReducerEvent.FoundClaimed)
                CoinChargeClaimResult.Failed -> dispatch(ShopCoinChargeReducerEvent.PayFailed)
            }
        }
    }

    /** 받은 뒤 화면을 닫고(들어온 곳으로 돌아감), 고른 행동을 남긴다. */
    private fun leaveResult(action: String) {
        event?.let { tracker.resultAction(it, action) }
        navigationHelper.navigateToBack()
    }

    override fun onCleared() {
        val event = event
        if (event != null) {
            val state = currentState
            val stage = when {
                state.grantedCoins != null -> CoinChargeTracker.Values.STAGE_RESULT
                state.sheetPackage != null -> CoinChargeTracker.Values.STAGE_SHEET
                else -> CoinChargeTracker.Values.STAGE_LIST
            }
            tracker.exit(event, stage)
        }
        super.onCleared()
    }

    override fun reduce(state: ShopCoinChargeUIState, event: ShopCoinChargeReducerEvent): ShopCoinChargeUIState =
        when (event) {
            is ShopCoinChargeReducerEvent.BalanceChanged -> state.copy(balance = event.balance)
            is ShopCoinChargeReducerEvent.OfferLoaded -> state.copy(
                packages = event.offer?.event?.packages.orEmpty().toImmutableList(),
                isLoading = false,
                isEnded = event.offer == null,
                isClaimed = event.offer?.isClaimed == true,
            )
            is ShopCoinChargeReducerEvent.SheetChanged -> state.copy(sheetPackage = event.pack)
            ShopCoinChargeReducerEvent.PayStarted -> state.copy(isPaying = true)
            // 지급 응답에 잔액이 있으면 바로 보이고, 없으면 다시 읽은 상단 바 잔액을 기다린다.
            is ShopCoinChargeReducerEvent.Granted -> state.copy(
                balance = event.balance ?: state.balance,
                isPaying = false,
                sheetPackage = null,
                isClaimed = true,
                grantedCoins = event.coins,
            )
            ShopCoinChargeReducerEvent.FoundClaimed -> state.copy(
                isPaying = false,
                sheetPackage = null,
                isClaimed = true,
                showsClaimedDialog = true,
            )
            ShopCoinChargeReducerEvent.PayFailed -> state.copy(isPaying = false)
            is ShopCoinChargeReducerEvent.ClaimedDialogChanged -> state.copy(showsClaimedDialog = event.shows)
        }
}
