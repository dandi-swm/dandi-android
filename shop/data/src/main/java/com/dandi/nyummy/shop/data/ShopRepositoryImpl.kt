package com.dandi.nyummy.shop.data

import android.util.Log
import com.dandi.nyummy.shop.data.dto.CartDTO
import com.dandi.nyummy.shop.data.dto.PurchaseRequestDTO
import com.dandi.nyummy.shop.data.dto.toDTO
import com.dandi.nyummy.shop.domain.ShopRepository
import com.dandi.nyummy.shop.entity.AddedCartItemVO
import com.dandi.nyummy.shop.entity.CartVO
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.PurchaseProductVO
import com.dandi.nyummy.shop.entity.PurchaseResultVO
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * 상점 저장소. 싱글턴이라 상단 바 값([summary])을 상점 화면들이 같이 본다.
 * 상단 바 조회, 담기, 장바구니(항목 수·잔액), 구매(잔액·종류 수) 응답이 올 때마다 [summary]를 바꾼다.
 */
class ShopRepositoryImpl(
    private val dataSource: ShopDataSource,
    private val json: Json,
) : ShopRepository {

    private val summaryState = MutableStateFlow(ShopSummaryVO())
    override val summary: StateFlow<ShopSummaryVO> = summaryState.asStateFlow()

    private val purchaseCompletedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val purchaseCompleted: Flow<Unit> = purchaseCompletedEvents.asSharedFlow()

    override suspend fun refreshSummary(): ShopSummaryVO =
        dataSource.getMe().toVO().also { summaryState.value = it }

    override suspend fun getRecommendPage(layoutVariant: String): ShopRecommendPageVO =
        dataSource.getRecommendPage(layoutVariant).toVO(json) { Log.d(TAG, it) }.copy(layoutVariant = layoutVariant)

    override suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO = dataSource.getProducts(tab.name, category?.name, sort.name, cursor, size).toVO()

    override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): ShopPromotionPageVO =
        dataSource.getPromotionProducts(promotionId, cursor, size).toVO()

    override suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO =
        dataSource.addCartItem(productId, quantity).toVO().also { added ->
            summaryState.update { it.copy(cartItemCount = added.cartItemCount) }
        }

    override suspend fun getCart(): CartVO = dataSource.getCart().applyToSummary()

    override suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO =
        dataSource.updateCartItemQuantity(productId, quantity).applyToSummary()

    override suspend fun updateCartSelection(productIds: List<Long>): CartVO =
        dataSource.updateCartSelection(productIds).applyToSummary()

    override suspend fun deleteCartItems(productIds: List<Long>): CartVO =
        dataSource.deleteCartItems(productIds).applyToSummary()

    override suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>): PurchaseResultVO {
        val result = dataSource.purchase(idempotencyKey, PurchaseRequestDTO(products.map { it.toDTO() })).toVO()
        summaryState.value = ShopSummaryVO(balance = result.balance, cartItemCount = result.cartItemCount)
        purchaseCompletedEvents.tryEmit(Unit)
        return result
    }

    override suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO =
        dataSource.getCoinHistories(cursor, size).toVO()

    /** 장바구니 응답의 항목 수(서버 기준)와 잔액으로 상단 바를 맞춘다. */
    private fun CartDTO.applyToSummary(): CartVO = toVO().also { cart ->
        summaryState.value = ShopSummaryVO(balance = cart.summary.balance, cartItemCount = cartItems.orEmpty().size)
    }

    private companion object {
        const val TAG = "ShopRepository"
    }
}
