package com.dandi.nyummy.shop.data

import android.util.Log
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
import kotlinx.serialization.json.Json

/**
 * 상점 저장소. 싱글턴이라 상단 바 값([summary])을 상점 화면들이 같이 본다.
 * 장바구니·구매·코인 내역은 다음 단계(`feat/shop-data-cart`)에서 채운다. 그 전에는 부르는 화면이 없다.
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

    override suspend fun getRecommendPage(): ShopRecommendPageVO =
        dataSource.getRecommendPage().toVO(json) { Log.d(TAG, it) }

    override suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO = dataSource.getProducts(tab.name, category?.name, sort.name, cursor, size).toVO()

    override suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): ShopPromotionPageVO =
        dataSource.getPromotionProducts(promotionId, cursor, size).toVO()

    override suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO = TODO(NEXT_STEP)

    override suspend fun getCart(): CartVO = TODO(NEXT_STEP)

    override suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO = TODO(NEXT_STEP)

    override suspend fun updateCartSelection(productIds: List<Long>): CartVO = TODO(NEXT_STEP)

    override suspend fun deleteCartItems(productIds: List<Long>): CartVO = TODO(NEXT_STEP)

    override suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>): PurchaseResultVO =
        TODO(NEXT_STEP)

    override suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO = TODO(NEXT_STEP)

    private companion object {
        const val TAG = "ShopRepository"
        const val NEXT_STEP = "feat/shop-data-cart에서 구현"
    }
}
