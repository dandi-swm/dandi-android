package com.dandi.nyummy.shop.presentation.recommend

import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.shop.domain.GetShopRecommendPageUseCase
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.presentation.FakeRemoteConfig
import com.dandi.nyummy.shop.presentation.FakeResourceHelper
import com.dandi.nyummy.shop.presentation.FakeShopRepository
import com.dandi.nyummy.shop.presentation.FakeTTIHelper
import com.dandi.nyummy.shop.presentation.RecordingMessageHelper
import com.dandi.nyummy.shop.presentation.RecordingNavigationHelper
import com.dandi.nyummy.shop.presentation.RecordingShopAnalytics
import com.dandi.nyummy.shop.presentation.httpException
import com.dandi.nyummy.shop.presentation.shopItem
import com.dandi.nyummy.shop.presentation.shopTracker
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

    private val analytics = RecordingShopAnalytics()
    private var layoutVariant = ""

    private fun createViewModel(): ShopRecommendViewModel {
        val tracker = shopTracker(analytics)
        val remoteConfig = FakeRemoteConfig(mapOf(RemoteConfigKey.ShopLayoutVariant to layoutVariant))
        return ShopRecommendViewModel(
            GetShopRecommendPageUseCase(repository, remoteConfig, tracker, FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper),
            tracker,
        )
    }

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

    // region 배치 A/B

    private val banner = ShopBannerVO(bannerId = 31, action = ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/promotions/3"))
    private val layoutB = listOf(
        SduiComponentVO.HeroBanner(listOf(ShopBannerVO(bannerId = 30), banner)),
        SduiComponentVO.SectionHeader(title = listOf(RichTextVO("지금 인기 있는 가구"))),
        SduiComponentVO.Ranking(listOf(shopItem(1), shopItem(2))),
        SduiComponentVO.SectionHeader(title = listOf(RichTextVO("오늘의 할인"))),
        SduiComponentVO.ItemList(listOf(shopItem(5), shopItem(6), shopItem(7))),
    )

    @Test
    fun `원격 구성의 배치 그룹을 보내고, 받은 칸 순서와 그룹을 노출로 남긴다`() = runTest(testDispatcher) {
        layoutVariant = "b"
        repository.recommendPage = ShopRecommendPageVO(layoutB)

        createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("B"), repository.recommendVariants)
        assertEquals(listOf("shop_layout_group" to "B"), analytics.userProperties)
        val view = analytics.events.single()
        assertEquals("shop_recommend_view", view.name)
        assertEquals(mapOf("layout_group" to "B", "sections" to "hero_banner,ranking,item_list"), view.params)
    }

    @Test
    fun `배치 그룹이 없거나 모르는 값이면 A로 보낸다`() = runTest(testDispatcher) {
        for (raw in listOf("", "C")) {
            layoutVariant = raw
            createViewModel()
            advanceUntilIdle()
        }

        assertEquals(listOf("A", "A"), repository.recommendVariants)
    }

    @Test
    fun `상품을 누르면 칸 종류, 제목 줄을 뺀 칸 순서, 칸 안 순서를 남긴다`() = runTest(testDispatcher) {
        layoutVariant = "B"
        repository.recommendPage = ShopRecommendPageVO(layoutB)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopRecommendIntent.ItemClicked(componentIndex = 4, position = 1, productId = 6))

        val click = analytics.events.last()
        assertEquals("shop_recommend_item_click", click.name)
        assertEquals(mapOf("layout_group" to "B", "section" to "item_list"), click.params)
        assertEquals(mapOf("section_order" to 3L, "position" to 2L, "product_id" to 6L), click.numberParams)
    }

    @Test
    fun `배너를 누르면 몇 번째 배너인지와 배너 id를 남긴다`() = runTest(testDispatcher) {
        repository.recommendPage = ShopRecommendPageVO(layoutB)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopRecommendIntent.BannerClicked(componentIndex = 0, action = banner.action!!))

        val click = analytics.events.last()
        assertEquals("hero_banner", click.params["section"])
        assertEquals(mapOf("section_order" to 1L, "position" to 2L, "banner_id" to 31L), click.numberParams)
    }

    @Test
    fun `칸을 받기 전에 누른 것은 남기지 않는다`() = runTest(testDispatcher) {
        repository.recommendError = IllegalStateException("offline")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ShopRecommendIntent.ItemClicked(componentIndex = 0, position = 0, productId = 1))

        assertTrue(analytics.events.isEmpty())
    }

    // endregion
}
