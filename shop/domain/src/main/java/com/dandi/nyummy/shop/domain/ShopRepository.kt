package com.dandi.nyummy.shop.domain

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
import kotlinx.coroutines.flow.StateFlow

interface ShopRepository {

    /** 상단 바 값(잔액·장바구니 종류 수). 상단 바 조회와 담기·장바구니·구매 응답이 올 때마다 바뀐다. */
    val summary: StateFlow<ShopSummaryVO>

    /** 구매가 끝날 때마다 한 번 낸다. 상품 목록 화면이 보유 개수를 다시 읽는 데 쓴다. */
    val purchaseCompleted: Flow<Unit>

    /** 상단 바 값을 서버에서 다시 읽고 [summary]를 바꾼다. */
    suspend fun refreshSummary(): ShopSummaryVO

    /** 추천 탭 SDUI 칸 목록. 모르는 칸과 필수 값이 빠진 칸은 뺀다. */
    suspend fun getRecommendPage(): ShopRecommendPageVO

    /** 인테리어·방 배경 탭 상품 한 묶음. 방 배경 탭은 [category]를 보내지 않는다. */
    suspend fun getProducts(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long?,
        size: Int,
    ): ShopItemPageVO

    /** 프로모션 상품 모음 한 묶음. 끝났거나 볼 수 없는 프로모션이면 `promotionNotFound`(404)를 던진다. */
    suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): ShopPromotionPageVO

    /** 상품을 [quantity]개 더 담는다. 성공하면 [summary]의 장바구니 종류 수가 바뀐다. */
    suspend fun addToCart(productId: Long, quantity: Int): AddedCartItemVO

    suspend fun getCart(): CartVO

    /** 수량을 이 값으로 맞춘다. */
    suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartVO

    /** 선택 상품을 이 목록으로 맞춘다. 전체 해제는 빈 목록이다. */
    suspend fun updateCartSelection(productIds: List<Long>): CartVO

    suspend fun deleteCartItems(productIds: List<Long>): CartVO

    /** 구매한다. 같은 [idempotencyKey]로 다시 보내면 서버가 처음 결과를 돌려준다. 성공하면 [purchaseCompleted]를 낸다. */
    suspend fun purchase(idempotencyKey: String, products: List<PurchaseProductVO>): PurchaseResultVO

    /** 코인 내역 한 묶음(최신순). */
    suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoryPageVO
}
