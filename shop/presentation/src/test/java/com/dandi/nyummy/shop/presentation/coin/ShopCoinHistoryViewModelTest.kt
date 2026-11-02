package com.dandi.nyummy.shop.presentation.coin

import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.shop.domain.GetCoinHistoriesUseCase
import com.dandi.nyummy.shop.domain.ObserveShopSummaryUseCase
import com.dandi.nyummy.shop.domain.RefreshShopSummaryUseCase
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.CoinHistoryVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCoinHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val navigationHelper = RecordingNavigationHelper()

    private fun history(id: Long, at: LocalDateTime) = CoinHistoryVO(coinHistoryId = id, title = "우편 받기", quantity = 10, createdAt = at)

    private fun headers(entries: List<CoinHistoryEntry>) =
        entries.filterIsInstance<CoinHistoryEntry.DateHeader>().map { it.date }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ShopCoinHistoryViewModel {
        val messageHelper = RecordingMessageHelper()
        return ShopCoinHistoryViewModel(
            navigationHelper = navigationHelper,
            getCoinHistories = GetCoinHistoriesUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            observeShopSummary = ObserveShopSummaryUseCase(repository),
            refreshShopSummary = RefreshShopSummaryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        )
    }

    @Test
    fun `같은 날짜 내역은 머리글 하나 아래에 묶고 자정을 넘기면 새 머리글이다`() {
        val entries = coinHistoryEntries(
            listOf(
                history(1, LocalDateTime.of(2026, 10, 27, 0, 10)),
                history(2, LocalDateTime.of(2026, 10, 26, 23, 50)),
                history(3, LocalDateTime.of(2026, 10, 26, 8, 0)),
            ),
        )

        assertEquals(listOf(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 26)), headers(entries))
        assertEquals(5, entries.size)
        assertTrue(entries[1] is CoinHistoryEntry.Row)
    }

    @Test
    fun `두 묶음에 걸친 같은 날짜는 머리글이 한 번만 나온다`() = runTest(testDispatcher) {
        repository.coinHistoryHandler = { cursor ->
            if (cursor == null) {
                CoinHistoryPageVO(listOf(history(1, LocalDateTime.of(2026, 10, 27, 14, 20)), history(2, LocalDateTime.of(2026, 10, 26, 19, 40))), nextCursor = 2)
            } else {
                CoinHistoryPageVO(listOf(history(3, LocalDateTime.of(2026, 10, 26, 8, 10))), nextCursor = null)
            }
        }
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinHistoryIntent.Enter)
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinHistoryIntent.LoadMore)
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinHistoryIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(null, 2L), repository.coinHistoryRequests)
        assertEquals(listOf(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 26)), headers(viewModel.uiState.value.entries))
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.histories.map { it.coinHistoryId })
    }

    @Test
    fun `내 코인은 공유 상태를 따르고 들어올 때 새로 읽는다`() = runTest(testDispatcher) {
        repository.summaryState.value = ShopSummaryVO(balance = 980, cartItemCount = 0)
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinHistoryIntent.Enter)
        advanceUntilIdle()

        assertEquals(980, viewModel.uiState.value.balance)
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `내역이 없으면 빈 화면이고 식사 기록하기는 식사 기록 화면을 연다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopCoinHistoryIntent.Enter)
        advanceUntilIdle()
        viewModel.onIntent(ShopCoinHistoryIntent.ClickRecordMeal)

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(listOf(MealRecordPage), navigationHelper.pages)
    }
}
