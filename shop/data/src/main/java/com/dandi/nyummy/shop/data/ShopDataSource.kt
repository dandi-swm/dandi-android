package com.dandi.nyummy.shop.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.shop.data.dto.PromotionProductsDTO
import com.dandi.nyummy.shop.data.dto.ShopItemsDTO
import com.dandi.nyummy.shop.data.dto.ShopMeDTO
import com.dandi.nyummy.shop.data.dto.ShopPageDTO

class ShopDataSource(
    private val apiService: ShopApiService,
) : BaseRemoteDataSource() {

    suspend fun getRecommendPage(): ShopPageDTO =
        checkResponse(apiService.getRecommendPage())

    suspend fun getProducts(tab: String, category: String?, sort: String, cursor: Long?, size: Int): ShopItemsDTO =
        checkResponse(apiService.getProducts(tab, category, sort, cursor, size))

    suspend fun getPromotionProducts(promotionId: Long, cursor: Long?, size: Int): PromotionProductsDTO =
        checkResponse(apiService.getPromotionProducts(promotionId, cursor, size))

    suspend fun getMe(): ShopMeDTO =
        checkResponse(apiService.getMe())
}
