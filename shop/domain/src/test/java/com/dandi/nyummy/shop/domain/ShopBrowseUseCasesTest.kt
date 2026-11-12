package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ShopBrowseUseCasesTest {

    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private val addToCart = AddToCartUseCase(repository, shopTracker(), FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private val getPromotion = GetShopPromotionUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private val getProducts = GetShopProductsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)

    @Test
    fun `담기는 늘 1개를 담고 성공을 알린다`() = runBlocking {
        repository.added = AddedCartItemVO(productId = 7L, quantity = 2, cartItemCount = 3)

        val result = addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertEquals(listOf(7L to 1), repository.addRequests)
        assertEquals(AddedCartItemVO(productId = 7L, quantity = 2, cartItemCount = 3), result.getOrNull())
        assertEquals(listOf(StringResource.SHOP_ADDED_TO_CART.name), messageHelper.toasts)
    }

    @Test
    fun `장바구니가 가득 차서 거절되면 가득 찼다고 알린다`() = runBlocking {
        repository.error = httpException(409, ShopErrorType.CART_FULL.type)

        val result = addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertTrue(result.isFailure)
        assertEquals(listOf(StringResource.SHOP_CART_FULL.name), messageHelper.toasts)
    }

    @Test
    fun `그 밖의 거절은 모두 더 이상 담을 수 없다고 알린다`() {
        val rejections = listOf(
            409 to ShopErrorType.CART_QUANTITY_EXCEEDED,
            409 to ShopErrorType.PURCHASE_LIMIT_EXCEEDED,
            409 to ShopErrorType.PRODUCT_NOT_FOR_SALE,
            409 to ShopErrorType.ALREADY_OWNED,
            404 to ShopErrorType.PRODUCT_NOT_FOUND,
        )
        for ((status, error) in rejections) {
            val messages = RecordingMessageHelper()
            val useCase = AddToCartUseCase(
                FakeShopRepository(error = httpException(status, error.type)),
                shopTracker(),
                FakeResourceHelper,
                messages,
                RecordingNavigationHelper(),
                FakeTTIHelper,
            )

            runBlocking { useCase(7L, ShopTracker.Source.of(ShopTab.RECOMMEND)) }

            assertEquals("$error", listOf(StringResource.SHOP_CANNOT_ADD_MORE.name), messages.toasts)
            assertTrue("$error", messages.dialogs.isEmpty())
        }
    }

    @Test
    fun `모르는 4xx도 더 이상 담을 수 없다고 알린다`() = runBlocking {
        repository.error = httpException(400, "api.common.invalidInputValue")

        addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertEquals(listOf(StringResource.SHOP_CANNOT_ADD_MORE.name), messageHelper.toasts)
    }

    @Test
    fun `코드 없는 404도 준비 중 안내 대신 더 이상 담을 수 없다고 알린다`() = runBlocking {
        repository.error = httpException(404)

        addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertEquals(listOf(StringResource.SHOP_CANNOT_ADD_MORE.name), messageHelper.toasts)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `응답 전에 끊기면 다시 담아 달라고 알린다`() = runBlocking {
        repository.error = IOException("offline")

        val result = addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertTrue(result.isFailure)
        assertEquals(listOf(StringResource.SHOP_ADD_FAILED.name), messageHelper.toasts)
    }

    @Test
    fun `서버 오류는 토스트 대신 공통 오류로 안내한다`() = runBlocking {
        repository.error = httpException(500)

        addToCart(7L, ShopTracker.Source.of(ShopTab.RECOMMEND))

        assertTrue(messageHelper.toasts.isEmpty())
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `끝난 프로모션은 실패가 아니라 null이다`() = runBlocking {
        repository.error = httpException(404, ShopErrorType.PROMOTION_NOT_FOUND.type)

        val result = getPromotion(12L)

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `진행 중인 프로모션은 묶음을 돌려준다`() = runBlocking {
        repository.promotionPage = ShopPromotionPageVO(nextCursor = 30L)

        assertEquals(ShopPromotionPageVO(nextCursor = 30L), getPromotion(12L).getOrNull())
    }

    @Test
    fun `방 배경 탭은 분류 칩을 보내지 않고 크기는 서버 최대치로 자른다`() = runBlocking {
        getProducts(ShopTab.BACKGROUND, ShopCategory.BED, ProductSort.POPULAR, cursor = 9L, size = 100)
        getProducts(ShopTab.INTERIOR, ShopCategory.BED, ProductSort.NEW)

        assertEquals(
            listOf(
                listOf(ShopTab.BACKGROUND, null, ProductSort.POPULAR, 9L, GetShopProductsUseCase.MAX_PAGE_SIZE),
                listOf(ShopTab.INTERIOR, ShopCategory.BED, ProductSort.NEW, null, GetShopProductsUseCase.PAGE_SIZE),
            ),
            repository.productRequests,
        )
    }
}
