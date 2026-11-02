package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.data.ShopApiService.Companion.SHOP_PATH
import com.dandi.nyummy.shop.data.dto.PurchaseRequestDTO
import com.dandi.nyummy.shop.data.dto.PurchaseResultDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 상점 구매 API
 */
interface PurchaseApiService {
    /** 아이템 구매 */
    @POST("$SHOP_PATH/purchases")
    suspend fun purchase(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PurchaseRequestDTO,
    ): Response<PurchaseResultDTO>
}
