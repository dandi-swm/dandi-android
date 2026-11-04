package com.dandi.nyummy.shop.data

import com.dandi.nyummy.common.data.richText.RichTextDTO
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.shop.data.dto.AddCartItemRequestDTO
import com.dandi.nyummy.shop.data.dto.AddedCartItemDTO
import com.dandi.nyummy.shop.data.dto.CartDTO
import com.dandi.nyummy.shop.data.dto.CartItemDTO
import com.dandi.nyummy.shop.data.dto.CartSummaryDTO
import com.dandi.nyummy.shop.data.dto.CoinGiftDTO
import com.dandi.nyummy.shop.data.dto.CoinGiftRequestDTO
import com.dandi.nyummy.shop.data.dto.CoinHistoriesDTO
import com.dandi.nyummy.shop.data.dto.CoinHistoryDTO
import com.dandi.nyummy.shop.data.dto.PromotionProductsDTO
import com.dandi.nyummy.shop.data.dto.PurchaseProductDTO
import com.dandi.nyummy.shop.data.dto.PurchaseRequestDTO
import com.dandi.nyummy.shop.data.dto.PurchaseResultDTO
import com.dandi.nyummy.shop.data.dto.ShopItemDTO
import com.dandi.nyummy.shop.data.dto.ShopItemsDTO
import com.dandi.nyummy.shop.data.dto.ShopMeDTO
import com.dandi.nyummy.shop.data.dto.ShopPageDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartQuantityRequestDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartSelectionRequestDTO
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.time.LocalDateTime
import java.time.ZoneId

class ShopRepositoryImplTest {

    private val shopApi = FakeShopApi()
    private val cartApi = FakeCartApi()
    private val purchaseApi = FakePurchaseApi()
    private val coinApi = FakeCoinApi()
    private val repository = ShopRepositoryImpl(
        ShopDataSource(shopApi, cartApi, purchaseApi, coinApi),
        Json { ignoreUnknownKeys = true },
    )

    @Test
    fun `선물 코인 받기는 가격 그룹을 헤더로, 묶음 번호를 본문으로 보낸다`() = runBlocking {
        val gift = ShopDataSource(shopApi, cartApi, purchaseApi, coinApi).claimCoinGift(coinPackageVariant = "B", coinPackageId = 2)

        assertEquals(listOf("B" to CoinGiftRequestDTO(coinPackageId = 2)), coinApi.giftRequests)
        assertEquals(CoinGiftDTO(coinQuantity = 3000, balance = 3860), gift)
    }

    @Test
    fun `추천 탭은 배치 그룹을 헤더로 보내고 받은 칸에 그 그룹을 적는다`() = runBlocking {
        val page = repository.getRecommendPage("B")

        assertEquals(listOf("B"), shopApi.recommendVariants)
        assertEquals("B", page.layoutVariant)
    }

    @Test
    fun `상단 바 조회는 잔액과 장바구니 종류 수를 공유 상태로 둔다`() = runBlocking {
        shopApi.me = ShopMeDTO(balance = 1240, cartItemCount = 3)

        repository.refreshSummary()

        assertEquals(ShopSummaryVO(balance = 1240, cartItemCount = 3), repository.summary.value)
    }

    @Test
    fun `담으면 잔액은 두고 장바구니 종류 수만 바꾼다`() = runBlocking {
        shopApi.me = ShopMeDTO(balance = 1240, cartItemCount = 3)
        repository.refreshSummary()
        cartApi.added = AddedCartItemDTO(productId = 7, quantity = 1, cartItemCount = 4)

        repository.addToCart(7, 1)

        assertEquals(listOf(AddCartItemRequestDTO(7, 1)), cartApi.addRequests)
        assertEquals(ShopSummaryVO(balance = 1240, cartItemCount = 4), repository.summary.value)
    }

    @Test
    fun `장바구니 응답은 서버 항목 수와 잔액으로 상단 바를 맞추고 그릴 수 없는 항목은 뺀다`() = runBlocking {
        cartApi.cart = CartDTO(
            cartItems = listOf(cartItem(1), CartItemDTO(shopItem = ShopItemDTO(productId = 2))),
            selectedItemCount = 1,
            summary = CartSummaryDTO(purchasePrice = 1620, balance = 1240, balanceAfter = -380),
        )

        val cart = repository.getCart()

        assertEquals(listOf(1L), cart.items.map { it.shopItem.productId })
        assertEquals("", cart.items.single().unavailableReason)
        assertEquals(CartSummaryVO(purchasePrice = 1620, balance = 1240, balanceAfter = -380), cart.summary)
        assertEquals(ShopSummaryVO(balance = 1240, cartItemCount = 2), repository.summary.value)
    }

    @Test
    fun `구매가 되면 잔액과 종류 수를 바꾸고 구매 완료를 한 번 낸다`() = runBlocking {
        purchaseApi.result = PurchaseResultDTO(itemPurchaseId = 9, quantity = 3, balance = 860, cartItemCount = 1)
        val completed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { repository.purchaseCompleted.first() }
        }

        val result = repository.purchase("key-1", listOf(PurchaseProductVO(productId = 1, quantity = 2, purchasePrice = 210)))

        completed.await()
        assertEquals(9L, result.itemPurchaseId)
        assertEquals(ShopSummaryVO(balance = 860, cartItemCount = 1), repository.summary.value)
        assertEquals(
            listOf("key-1" to PurchaseRequestDTO(listOf(PurchaseProductDTO(productId = 1, quantity = 2, purchasePrice = 210)))),
            purchaseApi.requests,
        )
    }

    @Test
    fun `구매가 거절되면 서버 코드를 담아 던지고 상단 바와 완료 신호는 그대로다`() = runBlocking {
        shopApi.me = ShopMeDTO(balance = 1240, cartItemCount = 3)
        repository.refreshSummary()
        purchaseApi.error = """{"code":"api.shop.priceChanged","message":"price changed"}"""
        val completed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(200) { repository.purchaseCompleted.first() }
        }

        try {
            repository.purchase("key-1", emptyList())
            fail("purchase should throw")
        } catch (e: HttpResponseException) {
            assertEquals(409, e.rawCode)
            assertEquals("api.shop.priceChanged", e.cause?.message)
        }

        assertNull(completed.await())
        assertEquals(ShopSummaryVO(balance = 1240, cartItemCount = 3), repository.summary.value)
    }

    @Test
    fun `코인 내역 시각은 기기 타임존 날짜로 바꾼다(자정 경계)`() {
        val dto = CoinHistoriesDTO(
            coinHistories = listOf(
                CoinHistoryDTO(coinHistoryId = 1, category = "ROOM_ITEM_PURCHASE", title = "발바닥 방석 외 2개 구매", quantity = -1620, createdAt = "2026-11-01T15:20:00Z"),
                CoinHistoryDTO(coinHistoryId = 2, createdAt = "not a time"),
            ),
            nextCursor = 1,
            hasNext = false,
        )

        val seoul = dto.toVO(ZoneId.of("Asia/Seoul"))
        val utc = dto.toVO(ZoneId.of("UTC"))

        assertEquals(listOf(LocalDateTime.of(2026, 11, 2, 0, 20)), seoul.items.map { it.createdAt })
        assertEquals(listOf(LocalDateTime.of(2026, 11, 1, 15, 20)), utc.items.map { it.createdAt })
        assertEquals(-1620, seoul.items.single().quantity)
        assertNull(seoul.nextCursor)
    }

    @Test
    fun `빼기는 상품 id 목록을 그대로 보낸다`() = runBlocking {
        repository.deleteCartItems(listOf(1L, 3L))

        assertEquals(listOf(listOf(1L, 3L)), cartApi.deleteRequests)
        assertTrue(repository.getCart().items.isEmpty())
    }

    private fun cartItem(productId: Long) = CartItemDTO(
        shopItem = ShopItemDTO(
            productId = productId,
            itemId = productId + 100,
            name = "발바닥 방석",
            price = listOf(RichTextDTO(text = "210")),
        ),
        quantity = 1,
        maxQuantity = 10,
        isSelected = true,
        canPurchase = true,
        purchasePrice = 210,
    )

    private class FakeShopApi : ShopApiService {
        var me = ShopMeDTO()
        val recommendVariants = mutableListOf<String>()
        override suspend fun getRecommendPage(layoutVariant: String): Response<ShopPageDTO> {
            recommendVariants += layoutVariant
            return Response.success(ShopPageDTO())
        }
        override suspend fun getProducts(tab: String, category: String?, sort: String, cursor: Long?, size: Int) =
            Response.success(ShopItemsDTO())
        override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int) =
            Response.success(PromotionProductsDTO())
        override suspend fun getMe(): Response<ShopMeDTO> = Response.success(me)
    }

    private class FakeCartApi : CartApiService {
        var cart = CartDTO()
        var added = AddedCartItemDTO()
        val addRequests = mutableListOf<AddCartItemRequestDTO>()
        val deleteRequests = mutableListOf<List<Long>>()
        override suspend fun getCart(): Response<CartDTO> = Response.success(cart)
        override suspend fun addCartItem(body: AddCartItemRequestDTO): Response<AddedCartItemDTO> {
            addRequests += body
            return Response.success(added)
        }
        override suspend fun updateCartItemQuantity(productId: Long, body: UpdateCartQuantityRequestDTO) =
            Response.success(cart)
        override suspend fun updateCartSelection(body: UpdateCartSelectionRequestDTO) = Response.success(cart)
        override suspend fun deleteCartItems(productIds: List<Long>): Response<CartDTO> {
            deleteRequests += productIds
            return Response.success(cart)
        }
    }

    private class FakePurchaseApi : PurchaseApiService {
        var result = PurchaseResultDTO()
        var error: String? = null
        val requests = mutableListOf<Pair<String, PurchaseRequestDTO>>()
        override suspend fun purchase(idempotencyKey: String, body: PurchaseRequestDTO): Response<PurchaseResultDTO> {
            requests += idempotencyKey to body
            return error?.let { Response.error(409, it.toResponseBody("application/json".toMediaType())) }
                ?: Response.success(result)
        }
    }

    private class FakeCoinApi : CoinApiService {
        val giftRequests = mutableListOf<Pair<String, CoinGiftRequestDTO>>()
        override suspend fun getCoinHistories(cursor: Long?, size: Int) = Response.success(CoinHistoriesDTO())
        override suspend fun claimCoinGift(coinPackageVariant: String, body: CoinGiftRequestDTO): Response<CoinGiftDTO> {
            giftRequests += coinPackageVariant to body
            return Response.success(CoinGiftDTO(coinQuantity = 3000, balance = 3860))
        }
    }
}
