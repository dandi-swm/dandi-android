package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.entity.ShopTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopTrackerTest {

    private val analytics = RecordingShopAnalytics()
    private val messageHelper = RecordingMessageHelper()

    private fun addToCart(repository: FakeShopRepository) =
        AddToCartUseCase(repository, shopTracker(analytics), FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper)

    private fun purchase(repository: FakeShopRepository) =
        PurchaseUseCase(repository, shopTracker(analytics), FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper)

    @Test
    fun `담으면 담은 곳과 상품을 남긴다`() = runBlocking {
        addToCart(FakeShopRepository())(7L, ShopTracker.Source.of(ShopTab.INTERIOR))

        val event = analytics.events.single()
        assertEquals("shop_add_to_cart", event.name)
        assertEquals(mapOf("source" to "interior"), event.params)
        assertEquals(mapOf("product_id" to 7L), event.numberParams)
    }

    @Test
    fun `담지 못하면 남기지 않는다`() = runBlocking {
        addToCart(FakeShopRepository(error = httpException(409, ShopErrorType.CART_FULL.type)))(7L, ShopTracker.Source.CART)

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `구매를 마치면 산 개수와 차감한 코인을 남긴다`() = runBlocking {
        val repository = FakeShopRepository(purchaseResult = PurchaseResultVO(quantity = 3, purchasePrice = 1620))

        purchase(repository)(listOf(PurchaseProductVO(productId = 1L, quantity = 3, purchasePrice = 540)))

        val event = analytics.events.single()
        assertEquals("shop_purchase", event.name)
        assertEquals(mapOf("item_count" to 3L, "coins" to 1620L), event.numberParams)
    }

    @Test
    fun `구매가 막히면 남기지 않는다`() = runBlocking {
        val repository = FakeShopRepository().apply { purchaseErrors += httpException(409, ShopErrorType.INSUFFICIENT_BALANCE.type) }

        purchase(repository)(listOf(PurchaseProductVO(productId = 1L, quantity = 1, purchasePrice = 540)))

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `배치 그룹은 B만 B이고 그 밖은 A다`() {
        assertEquals(listOf("A", "B", "B", "A", "A"), listOf("A", "B", " b ", "", "C").map(ShopLayoutVariant::of))
    }
}
