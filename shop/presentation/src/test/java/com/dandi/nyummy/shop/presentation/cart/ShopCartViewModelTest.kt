package com.dandi.nyummy.shop.presentation.cart

import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.DeleteCartItemsUseCase
import com.dandi.nyummy.shop.domain.GetCartUseCase
import com.dandi.nyummy.shop.domain.GetShopProductsUseCase
import com.dandi.nyummy.shop.domain.ShopErrorType
import com.dandi.nyummy.shop.domain.ShopMessages
import com.dandi.nyummy.shop.domain.UpdateCartQuantityUseCase
import com.dandi.nyummy.shop.domain.UpdateCartSelectionUseCase
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeShopRepository.CartCall
import com.dandi.nyummy.shop.presentation.FakeShopRepository.ProductRequest
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import com.dandi.nyummy.shop.presentation.httpException
import com.dandi.nyummy.shop.presentation.shopItem
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private fun row(productId: Long, quantity: Int = 1, max: Int = 10, selected: Boolean = true, canPurchase: Boolean = true) =
        CartItemVO(shopItem(productId), quantity = quantity, maxQuantity = max, isSelected = selected, canPurchase = canPurchase)

    private fun cartOf(vararg rows: CartItemVO) = CartVO(items = rows.toList(), selectedItemCount = rows.count { it.isSelected })

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ShopCartViewModel =
        ShopCartViewModel(
            navigationHelper = navigationHelper,
            getCart = GetCartUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            updateCartQuantity = UpdateCartQuantityUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            updateCartSelection = UpdateCartSelectionUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            deleteCartItems = DeleteCartItemsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            getShopProducts = GetShopProductsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            addToCart = AddToCartUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        )

    @Test
    fun `응답 순서가 뒤바뀌어도 마지막으로 보낸 수량이 남는다`() = runTest(testDispatcher) {
        val slow = CompletableDeferred<Unit>()
        repository.cartHandler = { call ->
            when (call) {
                CartCall.Get -> cartOf(row(1, quantity = 1))
                is CartCall.Quantity -> {
                    if (call.quantity == 2) slow.await()
                    cartOf(row(1, quantity = call.quantity))
                }
                else -> cartOf(row(1))
            }
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 2))
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 3))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.items.single().quantity)

        slow.complete(Unit)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.items.single().quantity)
        assertEquals(listOf(CartCall.Get, CartCall.Quantity(1, 2), CartCall.Quantity(1, 3)), repository.cartCalls)
    }

    @Test
    fun `수량은 1과 최대 수량 사이로 자르고 같으면 보내지 않는다`() = runTest(testDispatcher) {
        repository.cartHandler = { call ->
            if (call is CartCall.Quantity) cartOf(row(1, quantity = call.quantity, max = 3)) else cartOf(row(1, quantity = 1, max = 3))
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 0))
        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 9))
        advanceUntilIdle()

        assertEquals(listOf(CartCall.Get, CartCall.Quantity(1, 3)), repository.cartCalls)
        assertEquals(3, viewModel.uiState.value.items.single().quantity)
    }

    @Test
    fun `수량 변경이 거절되면 더 담을 수 없다고 알리고 장바구니를 다시 읽는다`() = runTest(testDispatcher) {
        repository.cartHandler = { call ->
            if (call is CartCall.Quantity) throw httpException(409, ShopErrorType.CART_QUANTITY_EXCEEDED.type)
            cartOf(row(1, quantity = 1))
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 2))
        assertEquals(2, viewModel.uiState.value.items.single().quantity)
        advanceUntilIdle()

        assertEquals(listOf(ShopMessages.CANNOT_ADD_MORE), messageHelper.toasts)
        assertEquals(listOf(CartCall.Get, CartCall.Quantity(1, 2), CartCall.Get), repository.cartCalls)
        assertEquals(1, viewModel.uiState.value.items.single().quantity)
    }

    @Test
    fun `선택은 먼저 바꿔 보이고 서버 응답으로 맞춘다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.cartHandler = { call ->
            when (call) {
                is CartCall.Selection -> {
                    gate.await()
                    // 서버는 2번만 골랐다고 돌려준다.
                    cartOf(row(1, selected = false), row(2, selected = true))
                }
                else -> cartOf(row(1, selected = true), row(2, selected = true))
            }
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ToggleSelect(1))
        advanceUntilIdle()
        assertEquals(listOf(false, true), viewModel.uiState.value.items.map { it.isSelected })
        assertEquals(1, viewModel.uiState.value.selectedItemCount)
        assertEquals(CartCall.Selection(listOf(2L)), repository.cartCalls.last())

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(false, true), viewModel.uiState.value.items.map { it.isSelected })
    }

    @Test
    fun `전체 선택은 살 수 있는 항목만 보내고 모두 골랐으면 모두 해제한다`() = runTest(testDispatcher) {
        var server = cartOf(row(1, selected = false), row(2, selected = false), row(3, selected = false, canPurchase = false))
        repository.cartHandler = { call ->
            if (call is CartCall.Selection) {
                server = cartOf(row(1, selected = 1L in call.productIds), row(2, selected = 2L in call.productIds), row(3, selected = false, canPurchase = false))
            }
            server
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ToggleSelectAll)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAllSelected)
        viewModel.onIntent(ShopCartIntent.ToggleSelectAll)
        advanceUntilIdle()

        assertEquals(
            listOf(CartCall.Selection(listOf(1L, 2L)), CartCall.Selection(emptyList())),
            repository.cartCalls.filterIsInstance<CartCall.Selection>(),
        )
        assertFalse(viewModel.uiState.value.isAllSelected)
    }

    @Test
    fun `살 수 없는 항목은 고르거나 수량을 바꿀 수 없다`() = runTest(testDispatcher) {
        repository.cartHandler = { cartOf(row(1, selected = false, canPurchase = false)) }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ToggleSelect(1))
        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 2))
        advanceUntilIdle()

        assertEquals(listOf(CartCall.Get), repository.cartCalls)
    }

    @Test
    fun `선택 삭제는 고른 항목만 빼고 고른 게 없으면 보내지 않는다`() = runTest(testDispatcher) {
        repository.cartHandler = { call ->
            if (call is CartCall.Delete) cartOf(row(2, selected = false)) else cartOf(row(1, selected = true), row(2, selected = false))
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.DeleteSelected)
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.DeleteSelected)
        advanceUntilIdle()

        assertEquals(listOf(CartCall.Get, CartCall.Delete(listOf(1L))), repository.cartCalls)
        assertEquals(listOf(2L), viewModel.uiState.value.items.map { it.shopItem.productId })
    }

    @Test
    fun `빈 장바구니는 인기 인테리어 6개를 추천하고 못 불러오면 추천 칸을 숨긴다`() = runTest(testDispatcher) {
        repository.productsHandler = { ShopItemPageVO(items = List(8) { shopItem(it.toLong()) }) }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(6, viewModel.uiState.value.recommendations.size)
        assertEquals(ProductRequest(ShopTab.INTERIOR, null, ProductSort.POPULAR, null), repository.productRequests.single())

        repository.productsHandler = { throw IOException("offline") }
        val failing = createViewModel()
        failing.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        assertTrue(failing.uiState.value.isEmpty)
        assertTrue(failing.uiState.value.recommendations.isEmpty())
    }

    @Test
    fun `추천 상품을 담으면 장바구니를 다시 읽는다`() = runTest(testDispatcher) {
        var added = false
        repository.cartHandler = { if (added) cartOf(row(7)) else CartVO() }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()

        added = true
        viewModel.onIntent(ShopCartIntent.ClickRecommendation(shopItem(7)))
        viewModel.onIntent(ShopCartIntent.AddRecommendation(shopItem(7)))
        advanceUntilIdle()

        assertEquals(listOf(7L), repository.addRequests)
        assertEquals(listOf(7L), viewModel.uiState.value.items.map { it.shopItem.productId })
        assertEquals(null, viewModel.uiState.value.detailItem)
        assertTrue(viewModel.uiState.value.recommendations.isEmpty())
    }

    @Test
    fun `처음 읽기에 실패하면 다시 시도 안내를 보인다`() = runTest(testDispatcher) {
        var fail = true
        repository.cartHandler = { if (fail) throw IOException("offline") else cartOf(row(1)) }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCartIntent.Enter)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadFailed)

        fail = false
        viewModel.onIntent(ShopCartIntent.Retry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(1, viewModel.uiState.value.items.size)
    }
}
