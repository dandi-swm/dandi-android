package com.dandi.nyummy.shop.presentation.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.shop.domain.ClaimCoinChargeUseCase
import com.dandi.nyummy.shop.domain.CoinChargeRepository
import com.dandi.nyummy.shop.domain.CoinChargeTracker
import com.dandi.nyummy.shop.domain.GetCoinChargeOfferUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCoinChargeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val packs = listOf(
        CoinChargePackageVO(coinPackageId = 1, coins = 300, price = 1500, imageId = 1),
        CoinChargePackageVO(coinPackageId = 2, coins = 1500, price = 7500, imageId = 2),
    )
    private val event = CoinChargeEventVO(eventId = "2026-11-coin-charge", group = "B", packages = packs)

    private val shopRepository = FakeShopRepository()
    private val coinChargeRepository = FakeCoinChargeRepository(event)
    private val navigationHelper = RecordingNavigationHelper()
    private val analytics = RecordingAnalytics()

    private val names get() = analytics.events.map { it.name }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ShopCoinChargeViewModel {
        val messageHelper = RecordingMessageHelper()
        val tracker = CoinChargeTracker(analytics)
        return ShopCoinChargeViewModel(
            navigationHelper = navigationHelper,
            getCoinChargeOffer = GetCoinChargeOfferUseCase(coinChargeRepository, tracker),
            claimCoinCharge = ClaimCoinChargeUseCase(
                coinChargeRepository, shopRepository, tracker, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper,
            ),
            tracker = tracker,
            observeShopSummary = ObserveShopSummaryUseCase(shopRepository),
            refreshShopSummary = RefreshShopSummaryUseCase(shopRepository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        )
    }

    @Test
    fun `들어오면 그룹의 묶음을 보이고 들어온 곳과 함께 한 번만 남긴다`() = runTest(testDispatcher) {
        shopRepository.summaryState.value = ShopSummaryVO(balance = 1240)
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(packs, state.packages)
        assertEquals(1240, state.balance)
        assertFalse(state.isLoading)
        val view = analytics.events.single()
        assertEquals("coin_charge_view", view.name)
        assertEquals(
            mapOf("event_id" to event.eventId, "ab_group" to "B", "source" to "shop_tab", "claimed" to "false"),
            view.params,
        )
        assertEquals(listOf(AnalyticsHelper.UserProperty.COIN_CHARGE_GROUP to "B"), analytics.userProperties)
    }

    @Test
    fun `알 수 없는 들어온 곳은 link로 남긴다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = null))
        advanceUntilIdle()

        assertEquals("link", analytics.events.single().params["source"])
    }

    @Test
    fun `이벤트가 없으면 끝났다고 보이고 아무것도 남기지 않는다`() = runTest(testDispatcher) {
        coinChargeRepository.current = null
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "link"))
        advanceUntilIdle()
        clear(viewModel)

        assertTrue(viewModel.uiState.value.isEnded)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `묶음을 누르면 시트가 뜨고, 취소하면 닫았다고 남긴다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()

        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(2L))
        assertEquals(packs[1], viewModel.uiState.value.sheetPackage)

        viewModel.onIntent(ShopCoinChargeIntent.DismissSheet)
        assertNull(viewModel.uiState.value.sheetPackage)

        assertEquals(listOf("coin_charge_view", "coin_charge_package_click", "coin_charge_sheet_close"), names)
        val click = analytics.events[1]
        assertEquals("2", click.params["package_id"])
        assertEquals(mapOf("price" to 7500L, "coins" to 1500L), click.numberParams)
    }

    @Test
    fun `결제하기를 누르면 누름을 남기고, 받으면 이벤트 코인으로 받은 뒤 화면을 보인다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))

        val gate = CompletableDeferred<Unit>()
        coinChargeRepository.claimGate = gate
        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPaying)
        viewModel.onIntent(ShopCoinChargeIntent.DismissSheet)
        assertEquals(packs[0], viewModel.uiState.value.sheetPackage)

        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3000, state.grantedCoins)
        assertNull(state.sheetPackage)
        assertTrue(state.isClaimed)
        assertEquals(
            listOf("coin_charge_view", "coin_charge_package_click", "coin_charge_pay_click", "coin_charge_granted"),
            names,
        )
        assertEquals(listOf(1L), coinChargeRepository.claimRequests)
    }

    @Test
    fun `지급 응답에 잔액이 있으면 받은 뒤 화면에 바로 보인다`() = runTest(testDispatcher) {
        shopRepository.summaryState.value = ShopSummaryVO(balance = 100)
        coinChargeRepository.grantBalance = 3100
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))

        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        advanceUntilIdle()

        assertEquals(3100, viewModel.uiState.value.balance)
    }

    @Test
    fun `받은 뒤 상점 둘러보기는 화면을 닫고 상점을 열고, 행동과 떠남을 남긴다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))
        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        advanceUntilIdle()

        viewModel.onIntent(ShopCoinChargeIntent.ClickGoShop)
        clear(viewModel)

        assertEquals(listOf(RecordingNavigationHelper.Back, ShopPage), navigationHelper.events)
        assertEquals("shop", analytics.events.first { it.name == "coin_charge_result_action" }.params["action"])
        assertEquals("result", analytics.events.last().params["stage"])
    }

    @Test
    fun `받은 뒤 홈으로는 화면을 닫고 홈 탭을 연다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))
        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        advanceUntilIdle()

        viewModel.onIntent(ShopCoinChargeIntent.ClickGoHome)

        assertEquals(listOf(RecordingNavigationHelper.Back, HomePage), navigationHelper.events)
        assertEquals("home", analytics.events.first { it.name == "coin_charge_result_action" }.params["action"])
    }

    @Test
    fun `이미 받은 사람이 묶음을 누르면 시트 없이 안내하고 다시 누름을 남긴다`() = runTest(testDispatcher) {
        coinChargeRepository.claimed += event.eventId
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "link"))
        advanceUntilIdle()

        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))

        val state = viewModel.uiState.value
        assertTrue(state.showsClaimedDialog)
        assertNull(state.sheetPackage)
        assertEquals("true", analytics.events.first().params["claimed"])
        assertEquals("coin_charge_repeat_click", analytics.events.last().name)
    }

    @Test
    fun `결제하기를 눌렀는데 이미 받은 것으로 확인되면 시트를 닫고 안내한다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))
        coinChargeRepository.claimed += event.eventId

        viewModel.onIntent(ShopCoinChargeIntent.ClickPay)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showsClaimedDialog)
        assertNull(state.sheetPackage)
        assertNull(state.grantedCoins)
    }

    @Test
    fun `시트를 띄운 채 떠나면 떠난 단계를 sheet로 남긴다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCoinChargeIntent.Enter(source = "shop_tab"))
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinChargeIntent.ClickPackage(1L))

        clear(viewModel)

        val exit = analytics.events.last()
        assertEquals("coin_charge_exit", exit.name)
        assertEquals("sheet", exit.params["stage"])
    }

    /** 화면을 떠날 때처럼 ViewModel을 정리한다. */
    private fun clear(viewModel: ShopCoinChargeViewModel) {
        val store = ViewModelStore()
        ViewModelProvider.create(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = viewModel as T
            },
        )[ShopCoinChargeViewModel::class]
        store.clear()
    }
}

private class FakeCoinChargeRepository(var current: CoinChargeEventVO?) : CoinChargeRepository {
    val claimed = mutableSetOf<String>()
    val claimRequests = mutableListOf<Long>()
    var claimGate: CompletableDeferred<Unit>? = null
    var grantBalance: Int? = null

    override fun getEvent(): CoinChargeEventVO? = current
    override suspend fun isClaimed(eventId: String): Boolean = eventId in claimed
    override suspend fun markClaimed(eventId: String) {
        claimed += eventId
    }

    override suspend fun claim(event: CoinChargeEventVO, coinPackageId: Long): CoinChargeGrantVO {
        claimRequests += coinPackageId
        claimGate?.await()
        return CoinChargeGrantVO(grantedCoins = 3000, balance = grantBalance)
    }
}

private class RecordingAnalytics : AnalyticsHelper {
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
