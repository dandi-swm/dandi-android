package com.dandi.nyummy.shop.presentation.recommend

import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.shop.domain.GetShopRecommendPageUseCase
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import com.dandi.nyummy.shop.presentation.httpException
import com.dandi.nyummy.shop.presentation.shopItem
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
class ShopRecommendViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()

    private val components = listOf(
        SduiComponentVO.SectionHeader(title = listOf(RichTextVO("지금 인기 있는 가구"))),
        SduiComponentVO.Ranking(listOf(shopItem(1), shopItem(2))),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShopRecommendViewModel(
        GetShopRecommendPageUseCase(repository, FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper),
    )

    @Test
    fun `처음 보일 때 서버 칸을 순서대로 읽는다`() = runTest(testDispatcher) {
        repository.recommendPage = ShopRecommendPageVO(components)

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(components, viewModel.uiState.value.components)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isLoadFailed)
        assertEquals(1, repository.recommendCount)
    }

    @Test
    fun `읽지 못하면 실패 안내를 보이고 다시 시도하면 다시 읽는다`() = runTest(testDispatcher) {
        repository.recommendPage = ShopRecommendPageVO(components)
        repository.recommendError = IOException("offline")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoadFailed)
        assertTrue(viewModel.uiState.value.components.isEmpty())

        viewModel.onIntent(ShopRecommendIntent.Retry)
        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isLoadFailed)
        advanceUntilIdle()

        assertEquals(components, viewModel.uiState.value.components)
        assertEquals(2, repository.recommendCount)
    }

    @Test
    fun `서버 오류는 공통 안내를 띄우고 실패 화면을 보인다`() = runTest(testDispatcher) {
        repository.recommendError = httpException(500)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoadFailed)
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `읽는 중에 다시 시도를 눌러도 요청은 한 번이다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(ShopRecommendIntent.Retry)
        viewModel.onIntent(ShopRecommendIntent.Retry)
        advanceUntilIdle()

        assertEquals(1, repository.recommendCount)
    }
}
