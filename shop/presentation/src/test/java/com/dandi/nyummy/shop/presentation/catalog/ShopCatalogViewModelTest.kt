package com.dandi.nyummy.shop.presentation.catalog

import com.dandi.nyummy.shop.domain.GetShopProductsUseCase
import com.dandi.nyummy.shop.domain.ObservePurchaseCompletedUseCase
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeShopRepository.ProductRequest
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import com.dandi.nyummy.shop.presentation.ShopSortRequest
import com.dandi.nyummy.shop.presentation.shopItem
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShopCatalogViewModel(
        GetShopProductsUseCase(repository, FakeResourceHelper, RecordingMessageHelper(), RecordingNavigationHelper(), FakeTTIHelper),
        ObservePurchaseCompletedUseCase(repository),
    )

    /** 커서 0부터 [pageCount]묶음, 묶음마다 [size]개. 상품 id는 커서 * 100 + 순번이다. */
    private fun pages(pageCount: Int, size: Int = 3, owned: Boolean = false): suspend (ProductRequest) -> ShopItemPageVO = { request ->
        val index = (request.cursor ?: 0L).toInt()
        ShopItemPageVO(
            items = List(size) { shopItem(index * 100L + it).copy(ownedCount = if (owned) 1 else 0) },
            nextCursor = (index + 1L).takeIf { it < pageCount },
        )
    }

    @Test
    fun `들어오면 신상품순 첫 묶음을 읽고 끝에 닿으면 다음 묶음을 이어 붙인다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 2)
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ProductRequest(ShopTab.INTERIOR, null, ProductSort.NEW, null),
                ProductRequest(ShopTab.INTERIOR, null, ProductSort.NEW, 1L),
            ),
            repository.productRequests,
        )
        assertEquals(listOf(0L, 1L, 2L, 100L, 101L, 102L), viewModel.uiState.value.visibleItems.map { it.productId })
    }

    @Test
    fun `마지막 묶음까지 받으면 더 부르지 않는다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 1)
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(1, repository.productRequests.size)
        assertFalse(viewModel.uiState.value.hasNext)
    }

    @Test
    fun `칩과 정렬을 바꾸면 처음부터 다시 읽는다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 3)
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.SelectCategory(ShopCategory.BED))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.OpenSortSheet)
        viewModel.onIntent(ShopCatalogIntent.SelectSort(ProductSort.PRICE_ASC))
        advanceUntilIdle()

        assertEquals(
            listOf(
                ProductRequest(ShopTab.INTERIOR, ShopCategory.BED, ProductSort.NEW, null),
                ProductRequest(ShopTab.INTERIOR, ShopCategory.BED, ProductSort.PRICE_ASC, null),
            ),
            repository.productRequests.drop(2),
        )
        assertEquals(3, viewModel.uiState.value.visibleItems.size)
        assertFalse(viewModel.uiState.value.isSortSheetOpen)
    }

    @Test
    fun `방 배경 탭은 분류를 보내지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.BACKGROUND))
        viewModel.onIntent(ShopCatalogIntent.SelectCategory(ShopCategory.BED))
        advanceUntilIdle()

        assertEquals(listOf(ProductRequest(ShopTab.BACKGROUND, null, ProductSort.NEW, null)), repository.productRequests)
    }

    @Test
    fun `조건을 바꾸기 전에 보낸 늦은 응답은 버린다`() = runTest(testDispatcher) {
        val slowAll = CompletableDeferred<Unit>()
        repository.productsHandler = { request ->
            if (request.category == null) {
                // 조건을 바꿀 때 조회 작업이 취소돼도 앞 응답이 실제로 늦게 돌아오게 한다(세대 검사가 버리는지 본다).
                withContext(NonCancellable) { slowAll.await() }
                ShopItemPageVO(items = listOf(shopItem(1)))
            } else {
                ShopItemPageVO(items = listOf(shopItem(2)))
            }
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.SelectCategory(ShopCategory.RUG))
        advanceUntilIdle()
        slowAll.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.visibleItems.map { it.productId })
    }

    @Test
    fun `보유 숨기기를 켜면 보유 상품을 빼고 모자라면 3묶음까지만 이어 받는다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 10, owned = true)
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.ToggleHideOwned)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.visibleItems.isEmpty())
        assertEquals(listOf(null, 1L, 2L, 3L), repository.productRequests.map { it.cursor })

        viewModel.onIntent(ShopCatalogIntent.ToggleHideOwned)
        assertEquals(12, viewModel.uiState.value.visibleItems.size)
    }

    @Test
    fun `보유 숨기기 이어 받기는 보이는 상품이 충분해지면 멈춘다`() = runTest(testDispatcher) {
        repository.productsHandler = { request ->
            val index = (request.cursor ?: 0L).toInt()
            ShopItemPageVO(
                // 첫 묶음은 모두 보유, 다음 묶음부터는 12개씩 보유하지 않은 상품이다.
                items = List(if (index == 0) 3 else 12) { shopItem(index * 100L + it).copy(ownedCount = if (index == 0) 1 else 0) },
                nextCursor = index + 1L,
            )
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.ToggleHideOwned)
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        assertEquals(listOf(null, 1L), repository.productRequests.map { it.cursor })
        assertEquals(12, viewModel.uiState.value.visibleItems.size)
    }

    @Test
    fun `보유 숨기기로 이어 받는 동안 대기 표시를 유지한다`() = runTest(testDispatcher) {
        val secondPage = CompletableDeferred<Unit>()
        repository.productsHandler = { request ->
            val index = (request.cursor ?: 0L).toInt()
            if (index == 1) secondPage.await()
            ShopItemPageVO(items = List(3) { shopItem(index * 100L + it).copy(ownedCount = 1) }, nextCursor = index + 1L)
        }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.ToggleHideOwned)
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.visibleItems.isEmpty())
        assertTrue(viewModel.uiState.value.isLoadingMore)

        secondPage.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun `이어 받기가 묶음 한도에서 멈추면 더 보기로 다음 묶음을 받는다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 10, owned = true)
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.ToggleHideOwned)
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasNext)
        assertFalse(viewModel.uiState.value.isLoadingMore)

        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(null, 1L, 2L, 3L, 4L, 5L), repository.productRequests.map { it.cursor })
    }

    @Test
    fun `링크 정렬은 이 탭 것만 한 번 반영한다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 1)))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 1)))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(ShopSortRequest(ShopTab.BACKGROUND, ProductSort.PRICE_DESC, id = 2)))
        advanceUntilIdle()

        assertEquals(ProductSort.POPULAR, viewModel.uiState.value.sort)
        assertEquals(listOf(ProductSort.NEW, ProductSort.POPULAR), repository.productRequests.map { it.sort })
    }

    @Test
    fun `되돌리기 요청은 칩과 정렬을 처음으로 돌려 다시 읽고 이미 처음이면 읽지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        viewModel.onIntent(ShopCatalogIntent.SelectSort(ProductSort.POPULAR))
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.ResetConditions(id = 1))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.ResetConditions(id = 1))
        viewModel.onIntent(ShopCatalogIntent.ResetConditions(id = 2))
        advanceUntilIdle()

        assertEquals(ProductSort.Default, viewModel.uiState.value.sort)
        assertEquals(null, viewModel.uiState.value.category)
        assertEquals(ProductSort.Default, repository.productRequests.last().sort)
        val requestsAfterReset = repository.productRequests.size
        viewModel.onIntent(ShopCatalogIntent.ResetConditions(id = 3))
        advanceUntilIdle()
        assertEquals(requestsAfterReset, repository.productRequests.size)
    }

    @Test
    fun `같은 정렬 링크를 다시 눌러도 처음부터 다시 읽는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 1)))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.ApplySortRequest(ShopSortRequest(ShopTab.INTERIOR, ProductSort.POPULAR, id = 2)))
        advanceUntilIdle()

        assertEquals(3, repository.productRequests.size)
    }

    @Test
    fun `구매가 끝나면 처음부터 다시 읽는다`() = runTest(testDispatcher) {
        repository.productsHandler = pages(pageCount = 3)
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()
        viewModel.onIntent(ShopCatalogIntent.LoadMore)
        advanceUntilIdle()

        repository.purchaseCompletedEvents.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf(null, 1L, null), repository.productRequests.map { it.cursor })
        assertEquals(3, viewModel.uiState.value.visibleItems.size)
    }

    @Test
    fun `첫 묶음을 못 읽으면 실패 안내 뒤 다시 시도로 다시 읽는다`() = runTest(testDispatcher) {
        var fail = true
        repository.productsHandler = { if (fail) throw IOException("offline") else ShopItemPageVO(items = listOf(shopItem(1))) }
        val viewModel = createViewModel()
        viewModel.onIntent(ShopCatalogIntent.Enter(ShopTab.INTERIOR))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoadFailed)
        fail = false
        viewModel.onIntent(ShopCatalogIntent.Retry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(listOf(1L), viewModel.uiState.value.visibleItems.map { it.productId })
    }
}
