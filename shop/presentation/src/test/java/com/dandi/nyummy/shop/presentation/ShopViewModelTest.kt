package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.CoinChargePackages
import com.dandi.nyummy.shop.domain.CoinChargeRepository
import com.dandi.nyummy.shop.domain.CoinChargeTracker
import com.dandi.nyummy.shop.domain.GetCoinChargeOfferUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopCoinChargePage
import com.dandi.nyummy.shop.domain.ShopCoinHistoryPage
import com.dandi.nyummy.shop.domain.ShopErrorType
import com.dandi.nyummy.shop.domain.ShopMessages
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val analytics = RecordingShopAnalytics()
    private val coinChargeRepository = FakeCoinChargeTabRepository()

    private fun createViewModel(): ShopViewModel {
        val coinChargeTracker = CoinChargeTracker(analytics)
        return ShopViewModel(
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            observeShopSummary = ObserveShopSummaryUseCase(repository),
            refreshShopSummary = RefreshShopSummaryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            addToCart = AddToCartUseCase(repository, shopTracker(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            getCoinChargeOffer = GetCoinChargeOfferUseCase(coinChargeRepository, coinChargeTracker),
            coinChargeTracker = coinChargeTracker,
        )
    }

    @Test
    fun `인자가 없으면 추천 탭으로 열린다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.Enter(tab = null, sort = null))

        assertEquals(ShopTab.RECOMMEND, viewModel.uiState.value.selectedTab)
        assertNull(viewModel.uiState.value.sortRequest)
    }

    @Test
    fun `링크 인자로 시작 탭과 정렬을 정하고 다시 들어와도 고른 탭을 덮지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.Enter(tab = "interior", sort = "POPULAR"))
        viewModel.onIntent(ShopIntent.SelectTab(ShopTab.BACKGROUND))
        viewModel.onIntent(ShopIntent.Enter(tab = "interior", sort = "POPULAR"))

        assertEquals(ShopTab.BACKGROUND, viewModel.uiState.value.selectedTab)
        assertEquals(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 1), viewModel.uiState.value.sortRequest)
    }

    @Test
    fun `상점 탭 링크 액션은 화면을 쌓지 않고 탭과 정렬만 바꾸며 같은 링크도 다시 적용한다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val link = ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/interior?sort=POPULAR")

        viewModel.onIntent(ShopIntent.ClickAction(link))
        viewModel.onIntent(ShopIntent.SelectTab(ShopTab.RECOMMEND))
        viewModel.onIntent(ShopIntent.ClickAction(link))

        assertEquals(ShopTab.INTERIOR, viewModel.uiState.value.selectedTab)
        assertEquals(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 2), viewModel.uiState.value.sortRequest)
        assertTrue(navigationHelper.appLinks.isEmpty())
        assertTrue(navigationHelper.pages.isEmpty())
    }

    @Test
    fun `정렬 없는 방 배경 링크는 탭만 바꾼다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.ClickAction(ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/background")))

        assertEquals(ShopTab.BACKGROUND, viewModel.uiState.value.selectedTab)
        assertNull(viewModel.uiState.value.sortRequest)
    }

    @Test
    fun `다른 링크는 앱 링크로 열고 토스트 액션은 글자를 띄운다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.ClickAction(ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/promotions/7")))
        viewModel.onIntent(ShopIntent.ClickAction(ShopActionVO.ShowToast("곧 열려요")))

        assertEquals(listOf("https://link.nyummy.co.kr/shop/promotions/7"), navigationHelper.appLinks)
        assertEquals(listOf("곧 열려요"), messageHelper.toasts)
        assertEquals(ShopTab.RECOMMEND, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `장바구니와 코인 칩은 각 화면을 연다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.ClickCart)
        viewModel.onIntent(ShopIntent.ClickCoins)

        assertEquals(listOf(ShopCartPage, ShopCoinHistoryPage), navigationHelper.pages)
    }

    @Test
    fun `상단 바는 공유 상태를 따르고 화면이 보일 때마다 새로 읽는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.summaryState.value = ShopSummaryVO(balance = 1240, cartItemCount = 2)
        viewModel.onIntent(ShopIntent.ScreenResumed)
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(1240, viewModel.uiState.value.balance)
        assertEquals(2, viewModel.uiState.value.cartItemCount)
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun `응답 전에 같은 상품을 다시 담으면 요청은 한 번만 간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>().also { repository.addGate = it }
        val item = shopItem(7)

        viewModel.onIntent(ShopIntent.AddToCart(item))
        advanceUntilIdle()
        viewModel.onIntent(ShopIntent.AddToCart(item))
        viewModel.onIntent(ShopIntent.AddToCart(item, fromSheet = true))
        advanceUntilIdle()

        assertEquals(listOf(7L), repository.addRequests)
        assertTrue(7L in viewModel.uiState.value.addingProductIds)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.onIntent(ShopIntent.AddToCart(item))
        advanceUntilIdle()

        assertEquals(listOf(7L, 7L), repository.addRequests)
        assertTrue(viewModel.uiState.value.addingProductIds.isEmpty())
        assertEquals(listOf(ShopMessages.ADDED_TO_CART, ShopMessages.ADDED_TO_CART), messageHelper.toasts)
    }

    @Test
    fun `다른 상품은 함께 담을 수 있다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        repository.addGate = CompletableDeferred()

        viewModel.onIntent(ShopIntent.AddToCart(shopItem(1)))
        viewModel.onIntent(ShopIntent.AddToCart(shopItem(2)))
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), repository.addRequests)
    }

    @Test
    fun `시트에서 담으면 담긴 뒤 시트를 닫는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val item = shopItem(7)

        viewModel.onIntent(ShopIntent.ClickItem(item))
        assertEquals(item, viewModel.uiState.value.detailItem)
        viewModel.onIntent(ShopIntent.AddToCart(item, fromSheet = true))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.detailItem)
    }

    @Test
    fun `시트에서 담기가 거절되면 시트를 그대로 두고 알린다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val item = shopItem(7)
        repository.error = httpException(409, ShopErrorType.CART_QUANTITY_EXCEEDED.type)

        viewModel.onIntent(ShopIntent.ClickItem(item))
        viewModel.onIntent(ShopIntent.AddToCart(item, fromSheet = true))
        advanceUntilIdle()

        assertEquals(item, viewModel.uiState.value.detailItem)
        assertEquals(listOf(ShopMessages.CANNOT_ADD_MORE), messageHelper.toasts)
    }

    @Test
    fun `카드에서 담아도 열린 시트는 닫지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val opened = shopItem(1)

        viewModel.onIntent(ShopIntent.ClickItem(opened))
        viewModel.onIntent(ShopIntent.AddToCart(shopItem(2)))
        advanceUntilIdle()

        assertEquals(opened, viewModel.uiState.value.detailItem)
        viewModel.onIntent(ShopIntent.DismissDetail)
        assertNull(viewModel.uiState.value.detailItem)
    }

    @Test
    fun `시트를 바꾼 뒤 늦게 온 앞 상품의 담기 응답은 지금 시트를 닫지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>().also { repository.addGate = it }
        val first = shopItem(1)
        val second = shopItem(2)

        viewModel.onIntent(ShopIntent.ClickItem(first))
        viewModel.onIntent(ShopIntent.AddToCart(first, fromSheet = true))
        advanceUntilIdle()
        viewModel.onIntent(ShopIntent.DismissDetail)
        viewModel.onIntent(ShopIntent.ClickItem(second))
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(second, viewModel.uiState.value.detailItem)
    }

    // region 코인 충전 이벤트 탭

    private val coinChargeEvent = CoinChargeEventVO(eventId = "2026-11-coin-charge", group = "A", packages = CoinChargePackages.of("A"))

    @Test
    fun `코인 충전 이벤트가 없으면 코인 탭이 없다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.coinChargeTab)
    }

    @Test
    fun `받기 전에는 Event 칩이 붙고, 누르면 충전 화면으로 가며 탭 누름을 남긴다`() = runTest(testDispatcher) {
        coinChargeRepository.current = coinChargeEvent
        val viewModel = createViewModel()
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(ShopCoinChargeTab(showsEventChip = true), viewModel.uiState.value.coinChargeTab)
        viewModel.onIntent(ShopIntent.ClickCoinChargeTab)

        val route = navigationHelper.pages.single().toRoute()
        assertEquals(ShopCoinChargePage.PATH, route.path)
        assertEquals("shop_tab", route.args[ShopCoinChargePage.ARG_SOURCE])
        val click = analytics.events.single()
        assertEquals("coin_charge_tab_click", click.name)
        assertEquals("false", click.params["claimed"])
    }

    @Test
    fun `받은 뒤에는 칩이 없고, 누르면 준비 중 안내만 띄우고 충전 화면으로 가지 않는다`() = runTest(testDispatcher) {
        coinChargeRepository.current = coinChargeEvent
        coinChargeRepository.claimed += coinChargeEvent.eventId
        val viewModel = createViewModel()
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(ShopCoinChargeTab(showsEventChip = false), viewModel.uiState.value.coinChargeTab)
        viewModel.onIntent(ShopIntent.ClickCoinChargeTab)

        assertTrue(navigationHelper.pages.isEmpty())
        assertEquals(listOf(CommonR.string.nyummy_not_ready), messageHelper.toastResIds)
        assertEquals("true", analytics.events.single().params["claimed"])
    }

    @Test
    fun `충전 화면에서 받고 돌아오면 다시 읽어 칩을 뗀다`() = runTest(testDispatcher) {
        coinChargeRepository.current = coinChargeEvent
        val viewModel = createViewModel()
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        coinChargeRepository.claimed += coinChargeEvent.eventId
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(ShopCoinChargeTab(showsEventChip = false), viewModel.uiState.value.coinChargeTab)
    }

    @Test
    fun `추천 탭을 당겨 새로 고치면 코인 탭도 다시 읽는다`() = runTest(testDispatcher) {
        coinChargeRepository.current = coinChargeEvent
        val viewModel = createViewModel()
        viewModel.onIntent(ShopIntent.ScreenResumed)
        advanceUntilIdle()

        coinChargeRepository.claimed += coinChargeEvent.eventId
        viewModel.onIntent(ShopIntent.PulledToRefresh)
        advanceUntilIdle()

        assertEquals(ShopCoinChargeTab(showsEventChip = false), viewModel.uiState.value.coinChargeTab)
    }

    // endregion
}

private class FakeCoinChargeTabRepository : CoinChargeRepository {
    var current: CoinChargeEventVO? = null
    val claimed = mutableSetOf<String>()

    override fun getEvent(): CoinChargeEventVO? = current
    override suspend fun isClaimed(eventId: String): Boolean = eventId in claimed
    override suspend fun markClaimed(eventId: String) {
        claimed += eventId
    }

    override suspend fun claim(event: CoinChargeEventVO, coinPackageId: Long): CoinChargeGrantVO = error("상점 화면은 받지 않는다")
}
