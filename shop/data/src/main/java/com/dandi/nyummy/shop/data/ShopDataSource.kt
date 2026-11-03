package com.dandi.nyummy.shop.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.shop.data.dto.AddCartItemRequestDTO
import com.dandi.nyummy.shop.data.dto.AddedCartItemDTO
import com.dandi.nyummy.shop.data.dto.CartDTO
import com.dandi.nyummy.shop.data.dto.CoinGiftDTO
import com.dandi.nyummy.shop.data.dto.CoinGiftRequestDTO
import com.dandi.nyummy.shop.data.dto.CoinHistoriesDTO
import com.dandi.nyummy.shop.data.dto.PromotionProductsDTO
import com.dandi.nyummy.shop.data.dto.PurchaseRequestDTO
import com.dandi.nyummy.shop.data.dto.PurchaseResultDTO
import com.dandi.nyummy.shop.data.dto.ShopItemsDTO
import com.dandi.nyummy.shop.data.dto.ShopMeDTO
import com.dandi.nyummy.shop.data.dto.ShopPageDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartQuantityRequestDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartSelectionRequestDTO

class ShopDataSource(
    private val apiService: ShopApiService,
    private val cartApiService: CartApiService,
    private val purchaseApiService: PurchaseApiService,
    private val coinApiService: CoinApiService,
) : BaseRemoteDataSource() {

    suspend fun getRecommendPage(): ShopPageDTO =
        checkResponse(apiService.getRecommendPage())

    suspend fun getProducts(tab: String, category: String?, sort: String, cursor: Long?, size: Int): ShopItemsDTO =
        checkResponse(apiService.getProducts(tab, category, sort, cursor, size))

    suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): PromotionProductsDTO =
        checkResponse(apiService.getPromotionProducts(promotionId, cursor, size))

    suspend fun getMe(): ShopMeDTO =
        checkResponse(apiService.getMe())

    suspend fun getCart(): CartDTO =
        checkResponse(cartApiService.getCart())

    suspend fun addCartItem(productId: Long, quantity: Int): AddedCartItemDTO =
        checkResponse(cartApiService.addCartItem(AddCartItemRequestDTO(productId, quantity)))

    suspend fun updateCartItemQuantity(productId: Long, quantity: Int): CartDTO =
        checkResponse(cartApiService.updateCartItemQuantity(productId, UpdateCartQuantityRequestDTO(quantity)))

    suspend fun updateCartSelection(productIds: List<Long>): CartDTO =
        checkResponse(cartApiService.updateCartSelection(UpdateCartSelectionRequestDTO(productIds)))

    suspend fun deleteCartItems(productIds: List<Long>): CartDTO =
        checkResponse(cartApiService.deleteCartItems(productIds))

    suspend fun purchase(idempotencyKey: String, body: PurchaseRequestDTO): PurchaseResultDTO =
        checkResponse(purchaseApiService.purchase(idempotencyKey, body))

    suspend fun getCoinHistories(cursor: Long?, size: Int): CoinHistoriesDTO =
        checkResponse(coinApiService.getCoinHistories(cursor, size))

    suspend fun claimCoinGift(coinPackageVariant: String, coinPackageId: Long): CoinGiftDTO =
        checkResponse(coinApiService.claimCoinGift(coinPackageVariant, CoinGiftRequestDTO(coinPackageId)))
}
