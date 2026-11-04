package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.data.dto.PromotionProductsDTO
import com.dandi.nyummy.shop.data.dto.ShopItemsDTO
import com.dandi.nyummy.shop.data.dto.ShopMeDTO
import com.dandi.nyummy.shop.data.dto.ShopPageDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 상점 API
 */
interface ShopApiService {
    /** 추천 탭 SDUI 조회 */
    @GET("$SHOP_PATH/pages/recommend")
    suspend fun getRecommendPage(@Header(HEADER_LAYOUT_VARIANT) layoutVariant: String): Response<ShopPageDTO>

    /** 인테리어·방 배경 탭 상품 목록 조회 */
    @GET("$SHOP_PATH/products")
    suspend fun getProducts(
        @Query("tab") tab: String,
        @Query("category") category: String?,
        @Query("sort") sort: String,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<ShopItemsDTO>

    /** 프로모션 상품 모음 조회 */
    @GET("$SHOP_PATH/promotions/{promotionId}/products")
    suspend fun getPromotionProducts(
        @Path("promotionId") promotionId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<PromotionProductsDTO>

    /** 상단 바 조회 */
    @GET("$SHOP_PATH/me")
    suspend fun getMe(): Response<ShopMeDTO>

    companion object {
        const val SHOP_PATH = "/api/v1/shop"

        /** 추천 탭 배치 A/B 그룹(A, B). 없거나 모르는 값이면 서버가 A로 준다. */
        const val HEADER_LAYOUT_VARIANT = "X-Shop-Layout-Variant"
    }
}
