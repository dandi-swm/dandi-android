package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.data.ShopApiService.Companion.SHOP_PATH
import com.dandi.nyummy.shop.data.dto.AddCartItemRequestDTO
import com.dandi.nyummy.shop.data.dto.AddedCartItemDTO
import com.dandi.nyummy.shop.data.dto.CartDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartQuantityRequestDTO
import com.dandi.nyummy.shop.data.dto.UpdateCartSelectionRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 상점 장바구니 API
 */
interface CartApiService {
    /** 장바구니 조회 */
    @GET("$SHOP_PATH/cart")
    suspend fun getCart(): Response<CartDTO>

    /** 장바구니 담기 */
    @POST("$SHOP_PATH/cart/items")
    suspend fun addCartItem(@Body body: AddCartItemRequestDTO): Response<AddedCartItemDTO>

    /** 장바구니 수량 변경 */
    @PUT("$SHOP_PATH/cart/items/{productId}")
    suspend fun updateCartItemQuantity(
        @Path("productId") productId: Long,
        @Body body: UpdateCartQuantityRequestDTO,
    ): Response<CartDTO>

    /** 장바구니 선택 변경 */
    @PUT("$SHOP_PATH/cart/selection")
    suspend fun updateCartSelection(@Body body: UpdateCartSelectionRequestDTO): Response<CartDTO>

    /** 장바구니 빼기 */
    @DELETE("$SHOP_PATH/cart/items")
    suspend fun deleteCartItems(@Query("productIds") productIds: List<Long>): Response<CartDTO>
}
