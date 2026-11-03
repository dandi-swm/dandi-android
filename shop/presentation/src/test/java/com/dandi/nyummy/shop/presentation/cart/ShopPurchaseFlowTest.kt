package com.dandi.nyummy.shop.presentation.cart

import com.dandi.nyummy.mailbox.domain.MailboxPage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.shop.domain.AddToCartUseCase
import com.dandi.nyummy.shop.domain.DeleteCartItemsUseCase
import com.dandi.nyummy.shop.domain.GetCartUseCase
import com.dandi.nyummy.shop.domain.GetShopProductsUseCase
import com.dandi.nyummy.shop.domain.PurchaseUseCase
import com.dandi.nyummy.shop.domain.ShopErrorType
import com.dandi.nyummy.shop.domain.ShopPage
import com.dandi.nyummy.shop.domain.UpdateCartQuantityUseCase
import com.dandi.nyummy.shop.domain.UpdateCartSelectionUseCase
import com.dandi.nyummy.shop.entity.CartItemVO
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeShopRepository.CartCall
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopPurchaseFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    /** 1번(2개, 150), 2번(1개, 320)을 골랐고, 3번은 골랐지만 살 수 없고, 4번은 살 수 있지만 고르지 않았다. */
    private fun cart(balanceAfter: Int = 860) = CartVO(
        items = listOf(
            CartItemVO(shopItem(1), quantity = 2, maxQuantity = 10, isSelected = true, canPurchase = true, purchasePrice = 150),
            CartItemVO(shopItem(2), quantity = 1, maxQuantity = 10, isSelected = true, canPurchase = true, purchasePrice = 320),
            CartItemVO(shopItem(3), quantity = 1, maxQuantity = 0, isSelected = true, canPurchase = false, purchasePrice = 400),
            CartItemVO(shopItem(4), quantity = 1, maxQuantity = 10, isSelected = false, canPurchase = true, purchasePrice = 200),
        ),
        selectedItemCount = 2,
        summary = CartSummaryVO(quantity = 3, purchasePrice = 620, balance = 620 + balanceAfter, balanceAfter = balanceAfter),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShopCartViewModel(
        navigationHelper = navigationHelper,
        getCart = GetCartUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        updateCartQuantity = UpdateCartQuantityUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        updateCartSelection = UpdateCartSelectionUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        deleteCartItems = DeleteCartItemsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        getShopProducts = GetShopProductsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        addToCart = AddToCartUseCase(repository, shopTracker(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        purchase = PurchaseUseCase(repository, shopTracker(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
    )

    private suspend fun enteredViewModel(cart: CartVO = cart()): ShopCartViewModel {
        repository.cartHandler = { cart }
        return createViewModel().also { it.onIntent(ShopCartIntent.Enter) }
    }

    @Test
    fun `코인이 모자라면 구매 요청 없이 부족 시트를 연다`() = runTest(testDispatcher) {
        val viewModel = enteredViewModel(cart(balanceAfter = -380))
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        advanceUntilIdle()

        assertEquals(ShopCartSheet.InsufficientBalance, viewModel.uiState.value.sheet)
        assertEquals(380, viewModel.uiState.value.shortage)
        assertTrue(repository.purchaseRequests.isEmpty())

        viewModel.onIntent(ShopCartIntent.ClickRecordMeal)
        assertNull(viewModel.uiState.value.sheet)
        assertEquals(listOf(MealRecordPage), navigationHelper.pages)
    }

    @Test
    fun `구매하면 고른 살 수 있는 항목만 보내고 완료 화면을 보인다`() = runTest(testDispatcher) {
        repository.purchaseHandler = { PurchaseResultVO(itemPurchaseId = 9, quantity = 3, balance = 860) }
        val viewModel = enteredViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        assertEquals(ShopCartSheet.Confirm, viewModel.uiState.value.sheet)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()

        assertEquals(
            listOf(listOf(PurchaseProductVO(1, 2, 150), PurchaseProductVO(2, 1, 320))),
            repository.purchaseRequests,
        )
        val completion = viewModel.uiState.value.completion!!
        assertEquals(listOf(1L, 2L), completion.items.map { it.productId })
        assertEquals(3, completion.quantity)
        assertEquals(860, completion.balance)
        assertNull(viewModel.uiState.value.sheet)
    }

    @Test
    fun `완료 화면에서 뒤로 가거나 우편함으로 가면 장바구니를 닫는다`() = runTest(testDispatcher) {
        repository.purchaseHandler = { PurchaseResultVO(quantity = 3, balance = 860) }
        val viewModel = enteredViewModel()
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickBack)
        viewModel.onIntent(ShopCartIntent.ClickGoToMailbox)

        assertEquals(2, navigationHelper.backCount)
        assertEquals(listOf(ShopPage, MailboxPage), navigationHelper.pages)
    }

    @Test
    fun `구매 요청 중에는 다시 보내지 않는다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.purchaseHandler = {
            gate.await()
            PurchaseResultVO(quantity = 3)
        }
        val viewModel = enteredViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPurchasing)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.purchaseRequests.size)
    }

    @Test
    fun `막히면 다이얼로그 뒤 장바구니를 다시 읽는다`() = runTest(testDispatcher) {
        repository.purchaseHandler = { throw httpException(409, ShopErrorType.PRICE_CHANGED.type) }
        val viewModel = enteredViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()

        assertEquals(listOf("장바구니를 다시 불러와 바뀐 가격을 보여 드릴게요."), messageHelper.dialogs)
        assertEquals(listOf(CartCall.Get, CartCall.Get), repository.cartCalls)
        assertNull(viewModel.uiState.value.sheet)
        assertNull(viewModel.uiState.value.completion)
    }

    @Test
    fun `서버가 코인 부족으로 거절하면 장바구니를 다시 받은 뒤 최신 금액으로 부족 시트를 연다`() = runTest(testDispatcher) {
        repository.purchaseHandler = { throw httpException(409, ShopErrorType.INSUFFICIENT_BALANCE.type) }
        val viewModel = enteredViewModel()
        advanceUntilIdle()
        val reloaded = CompletableDeferred<Unit>()
        repository.cartHandler = {
            reloaded.await()
            cart(balanceAfter = -100)
        }

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()
        // 다시 받는 동안에는 잔액이 넉넉했던 옛 금액으로 시트를 열지 않는다.
        assertNull(viewModel.uiState.value.sheet)
        assertTrue(viewModel.uiState.value.isSyncing)

        reloaded.complete(Unit)
        advanceUntilIdle()

        assertEquals(ShopCartSheet.InsufficientBalance, viewModel.uiState.value.sheet)
        assertEquals(100, viewModel.uiState.value.shortage)
        assertEquals(listOf(CartCall.Get, CartCall.Get), repository.cartCalls)
    }

    @Test
    fun `장바구니 요청의 응답을 받기 전에는 구매 시트를 열지 않는다`() = runTest(testDispatcher) {
        val viewModel = enteredViewModel()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.cartHandler = { call ->
            if (call is CartCall.Quantity) gate.await()
            cart()
        }

        viewModel.onIntent(ShopCartIntent.ChangeQuantity(1, 3))
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        assertNull(viewModel.uiState.value.sheet)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.onIntent(ShopCartIntent.ClickPurchase)

        assertEquals(ShopCartSheet.Confirm, viewModel.uiState.value.sheet)
    }

    @Test
    fun `처리 중이면 안내 없이 장바구니를 다시 읽는다`() = runTest(testDispatcher) {
        repository.purchaseHandler = { throw httpException(409, ShopErrorType.PURCHASE_IN_PROGRESS.type) }
        val viewModel = enteredViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopCartIntent.ClickPurchase)
        viewModel.onIntent(ShopCartIntent.ConfirmPurchase)
        advanceUntilIdle()

        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(listOf(CartCall.Get, CartCall.Get), repository.cartCalls)
        assertNull(viewModel.uiState.value.sheet)
        assertEquals(1, repository.purchaseRequests.size)
    }
}
