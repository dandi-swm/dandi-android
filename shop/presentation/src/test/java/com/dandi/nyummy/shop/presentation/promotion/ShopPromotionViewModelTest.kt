package com.dandi.nyummy.shop.presentation.promotion

import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.GetShopPromotionUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.domain.ShopCartPage
import com.dandi.nyummy.shop.domain.ShopErrorType
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import com.dandi.nyummy.shop.presentation.httpException
import com.dandi.nyummy.shop.presentation.shopItem
import com.dandi.nyummy.shop.presentation.shopTracker
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ShopPromotionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private val banner = ShopBannerVO(bannerId = 1, imageUrl = "https://cdn/b.png", title = listOf(RichTextVO("신입 집사\n환영")))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShopPromotionViewModel(
        navigationHelper = navigationHelper,
        getShopPromotion = GetShopPromotionUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        observeShopSummary = ObserveShopSummaryUseCase(repository),
        refreshShopSummary = RefreshShopSummaryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        addToCart = AddToCartUseCase(repository, shopTracker(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
    )

    @Test
    fun `첫 묶음으로 배너와 상품을 보이고 끝에 닿으면 이어 받는다`() = runTest(testDispatcher) {
        repository.promotionHandler = { _, cursor ->
            if (cursor == null) {
                ShopPromotionPageVO(banner, listOf(shopItem(1), shopItem(2)), nextCursor = 2)
            } else {
                ShopPromotionPageVO(banner, listOf(shopItem(3)), nextCursor = null)
            }
        }
        val viewModel = createViewModel()

        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.LoadMore)
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(12L to null, 12L to 2L), repository.promotionRequests)
        assertEquals(banner, viewModel.uiState.value.banner)
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.items.map { it.productId })
        assertFalse(viewModel.uiState.value.hasNext)
    }

    @Test
    fun `이어 받기에 실패해도 더 보기로 같은 커서부터 다시 받는다`() = runTest(testDispatcher) {
        var failNext = true
        repository.promotionHandler = { _, cursor ->
            when {
                cursor == null -> ShopPromotionPageVO(banner, listOf(shopItem(1)), nextCursor = 2)
                failNext -> throw IOException("offline")
                else -> ShopPromotionPageVO(banner, listOf(shopItem(2)), nextCursor = null)
            }
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()

        viewModel.onIntent(ShopPromotionIntent.LoadMore)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertTrue(viewModel.uiState.value.hasNext)

        failNext = false
        viewModel.onIntent(ShopPromotionIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(12L to null, 12L to 2L, 12L to 2L), repository.promotionRequests)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.items.map { it.productId })
    }

    @Test
    fun `시트를 바꾼 뒤 늦게 온 앞 상품의 담기 응답은 지금 시트를 닫지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>().also { repository.addGate = it }

        viewModel.onIntent(ShopPromotionIntent.ClickItem(shopItem(1)))
        viewModel.onIntent(ShopPromotionIntent.AddToCart(shopItem(1), fromSheet = true))
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.DismissDetail)
        viewModel.onIntent(ShopPromotionIntent.ClickItem(shopItem(2)))
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(shopItem(2), viewModel.uiState.value.detailItem)
    }

    @Test
    fun `끝난 프로모션이면 안내 화면을 보이고 상점으로 돌아가기는 이 화면을 닫고 상점을 연다`() = runTest(testDispatcher) {
        repository.promotionHandler = { _, _ -> throw httpException(404, ShopErrorType.PROMOTION_NOT_FOUND.type) }
        val viewModel = createViewModel()

        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.ClickGoToShop)

        assertTrue(viewModel.uiState.value.isEnded)
        assertTrue(messageHelper.dialogs.isEmpty())
        // 상점을 먼저 열고 뒤로 가면 끝난 프로모션 화면이 다시 드러나므로, 닫기가 먼저여야 한다.
        assertEquals(listOf(RecordingNavigationHelper.Back, ShopPage), navigationHelper.events)
    }

    @Test
    fun `읽지 못하면 다시 시도로 다시 읽는다`() = runTest(testDispatcher) {
        var fail = true
        repository.promotionHandler = { _, _ -> if (fail) throw IOException("offline") else ShopPromotionPageVO(banner) }
        val viewModel = createViewModel()

        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        fail = false
        viewModel.onIntent(ShopPromotionIntent.Retry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(banner, viewModel.uiState.value.banner)
    }

    @Test
    fun `담으면 1개를 담고 장바구니 배지는 공유 상태를 따른다`() = runTest(testDispatcher) {
        repository.promotionHandler = { _, _ -> ShopPromotionPageVO(banner, listOf(shopItem(1))) }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()

        viewModel.onIntent(ShopPromotionIntent.AddToCart(shopItem(1)))
        advanceUntilIdle()
        repository.summaryState.value = ShopSummaryVO(balance = 100, cartItemCount = 3)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.addRequests)
        assertEquals(3, viewModel.uiState.value.cartItemCount)
    }

    @Test
    fun `줄을 누르면 시트가 열리고 시트에서 담으면 닫힌다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val item = shopItem(1)

        viewModel.onIntent(ShopPromotionIntent.ClickItem(item))
        assertEquals(item, viewModel.uiState.value.detailItem)
        viewModel.onIntent(ShopPromotionIntent.AddToCart(item, fromSheet = true))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.detailItem)
    }

    @Test
    fun `같은 프로모션으로 다시 들어와도 다시 읽지 않고 장바구니는 장바구니 화면을 연다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.Enter(12))
        advanceUntilIdle()
        viewModel.onIntent(ShopPromotionIntent.ClickCart)

        assertEquals(1, repository.promotionRequests.size)
        assertEquals(listOf(ShopCartPage), navigationHelper.pages)
    }
}
