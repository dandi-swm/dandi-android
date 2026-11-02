package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class PurchaseUseCaseTest {

    private val repository = FakeShopRepository()
    private val messageHelper = RecordingMessageHelper()
    private val purchase = PurchaseUseCase(
        repository,
        FakeResourceHelper,
        messageHelper,
        RecordingNavigationHelper(),
        FakeTTIHelper,
    )
    private val products = listOf(
        PurchaseProductVO(productId = 1L, quantity = 2, purchasePrice = 210),
        PurchaseProductVO(productId = 3L, quantity = 1, purchasePrice = 1200),
    )

    @Test
    fun `장바구니에서 고른 상품을 보내고 완료 결과를 돌려준다`() = runBlocking {
        repository.purchaseResult = PurchaseResultVO(itemPurchaseId = 9L, quantity = 3, balance = 860)

        val outcome = purchase(products)

        assertEquals(PurchaseOutcome.Completed(PurchaseResultVO(itemPurchaseId = 9L, quantity = 3, balance = 860)), outcome)
        assertEquals(products, repository.purchaseRequests.single().second)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `구매하기를 누를 때마다 새 구매 키를 만든다`() = runBlocking {
        purchase(products)
        purchase(products)

        val keys = repository.purchaseRequests.map { it.first }
        assertEquals(2, keys.size)
        assertNotEquals(keys[0], keys[1])
        assertTrue(keys.all { it.isNotBlank() })
    }

    @Test
    fun `응답을 못 받으면 같은 키로 한 번 더 보내고 처음 결과로 완료한다`() = runBlocking {
        repository.purchaseErrors += SocketTimeoutException("timeout")
        repository.purchaseResult = PurchaseResultVO(itemPurchaseId = 9L)

        val outcome = purchase(products)

        assertEquals(PurchaseOutcome.Completed(PurchaseResultVO(itemPurchaseId = 9L)), outcome)
        val keys = repository.purchaseRequests.map { it.first }
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
    }

    @Test
    fun `다시 보내도 응답이 없으면 더 보내지 않고 결과 모름으로 조용히 끝낸다`() = runBlocking {
        repository.purchaseErrors += IOException("offline")
        repository.purchaseErrors += IOException("offline")

        val outcome = purchase(products)

        assertEquals(PurchaseOutcome.Unknown, outcome)
        assertEquals(2, repository.purchaseRequests.size)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `서버가 거절하면 다시 보내지 않는다`() = runBlocking {
        repository.purchaseErrors += httpException(409, ShopErrorType.PRICE_CHANGED.type)

        purchase(products)

        assertEquals(1, repository.purchaseRequests.size)
    }

    @Test
    fun `막힘 이유별로 피그마 다이얼로그를 띄우고 막힘 결과를 돌려준다`() {
        val cases = listOf(
            ShopErrorType.PRICE_CHANGED to Triple("가격이 바뀌었어요", "장바구니를 다시 불러와 바뀐 가격을 보여 드릴게요.", "장바구니 다시 보기"),
            ShopErrorType.CART_MISMATCH to Triple("가격이 바뀌었어요", "장바구니를 다시 불러와 바뀐 가격을 보여 드릴게요.", "장바구니 다시 보기"),
            ShopErrorType.PRODUCT_NOT_FOR_SALE to Triple("판매가 끝난 상품이 있어요", "장바구니를 다시 불러와 살 수 없는 상품을 표시해 드릴게요.", "확인"),
            ShopErrorType.PRODUCT_NOT_FOUND to Triple("판매가 끝난 상품이 있어요", "장바구니를 다시 불러와 살 수 없는 상품을 표시해 드릴게요.", "확인"),
            ShopErrorType.ALREADY_OWNED to Triple(
                "이미 가진 방 배경이에요",
                "방 배경은 하나만 가질 수 있어요. 장바구니를 다시 불러와 표시해 드릴게요.",
                "확인",
            ),
            ShopErrorType.PURCHASE_LIMIT_EXCEEDED to Triple(
                "구매 한도를 넘은 상품이 있어요",
                "한 사람이 살 수 있는 개수가 정해진 상품이에요. 장바구니를 다시 불러와 보여 드릴게요.",
                "장바구니 다시 보기",
            ),
        )
        for ((error, dialog) in cases) {
            val messages = RecordingMessageHelper()
            val status = if (error == ShopErrorType.PRODUCT_NOT_FOUND) 404 else 409
            val useCase = PurchaseUseCase(
                FakeShopRepository().apply { purchaseErrors += httpException(status, error.type) },
                FakeResourceHelper,
                messages,
                RecordingNavigationHelper(),
                FakeTTIHelper,
            )

            val outcome = runBlocking { useCase(products) }

            assertEquals("$error", PurchaseOutcome.Blocked, outcome)
            assertEquals(
                "$error",
                listOf(RecordingMessageHelper.DialogCall(dialog.first, dialog.second, dialog.third)),
                messages.dialogs,
            )
        }
    }

    @Test
    fun `코인이 모자라면 다이얼로그 없이 코인 부족 결과를 돌려준다`() = runBlocking {
        repository.purchaseErrors += httpException(409, ShopErrorType.INSUFFICIENT_BALANCE.type)

        assertEquals(PurchaseOutcome.InsufficientBalance, purchase(products))
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `처리 중이거나 키가 엉키면 안내 없이 결과 모름이고 새 키로 다시 보내지 않는다`() {
        for (error in listOf(ShopErrorType.PURCHASE_IN_PROGRESS, ShopErrorType.IDEMPOTENCY_KEY_REUSED)) {
            val repository = FakeShopRepository().apply { purchaseErrors += httpException(409, error.type) }
            val messages = RecordingMessageHelper()
            val useCase = PurchaseUseCase(repository, FakeResourceHelper, messages, RecordingNavigationHelper(), FakeTTIHelper)

            val outcome = runBlocking { useCase(products) }

            assertEquals("$error", PurchaseOutcome.Unknown, outcome)
            assertEquals("$error", 1, repository.purchaseRequests.size)
            assertTrue("$error", messages.dialogs.isEmpty())
        }
    }

    @Test
    fun `서버 오류와 모르는 4xx는 일시 오류를 알리고 결과 모름으로 다시 읽게 한다`() {
        for (error in listOf(httpException(500), httpException(400, "api.common.invalidInputValue"))) {
            val messages = RecordingMessageHelper()
            val useCase = PurchaseUseCase(
                FakeShopRepository().apply { purchaseErrors += error },
                FakeResourceHelper,
                messages,
                RecordingNavigationHelper(),
                FakeTTIHelper,
            )

            assertEquals(PurchaseOutcome.Unknown, runBlocking { useCase(products) })
            assertEquals("잠시 문제가 생겼어요", messages.dialogs.single().titleText)
        }
    }

    @Test
    fun `수량 변경이 거절되면 더 이상 담을 수 없다고 알리고 실패를 돌려준다`() = runBlocking {
        repository.error = httpException(409, ShopErrorType.CART_QUANTITY_EXCEEDED.type)
        val updateQuantity = UpdateCartQuantityUseCase(
            repository,
            FakeResourceHelper,
            messageHelper,
            RecordingNavigationHelper(),
            FakeTTIHelper,
        )

        val result = updateQuantity(1L, 11)

        assertTrue(result.isFailure)
        assertEquals(listOf(ShopMessages.CANNOT_ADD_MORE), messageHelper.toasts)
    }

    @Test
    fun `수량 변경이 되면 서버 장바구니를 그대로 돌려준다`() = runBlocking {
        repository.cart = CartVO(selectedItemCount = 2)
        val updateQuantity = UpdateCartQuantityUseCase(
            repository,
            FakeResourceHelper,
            messageHelper,
            RecordingNavigationHelper(),
            FakeTTIHelper,
        )

        assertEquals(CartVO(selectedItemCount = 2), updateQuantity(1L, 2).getOrNull())
        assertTrue(messageHelper.toasts.isEmpty())
    }
}
