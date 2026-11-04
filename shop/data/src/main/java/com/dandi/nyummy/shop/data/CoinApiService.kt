package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.data.dto.CoinGiftDTO
import com.dandi.nyummy.shop.data.dto.CoinGiftRequestDTO
import com.dandi.nyummy.shop.data.dto.CoinHistoriesDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 코인 API
 */
interface CoinApiService {
    /** 코인 내역 조회 */
    @GET("/api/v1/coins/histories")
    suspend fun getCoinHistories(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<CoinHistoriesDTO>

    /** 선물 코인 받기 */
    @POST("/api/v1/coins/gifts")
    suspend fun claimCoinGift(
        @Header(HEADER_COIN_PACKAGE_VARIANT) coinPackageVariant: String,
        @Body body: CoinGiftRequestDTO,
    ): Response<CoinGiftDTO>

    companion object {
        /** 코인 충전 이벤트 가격 그룹(A, B). 없거나 다른 값이면 서버가 400으로 거부한다. */
        const val HEADER_COIN_PACKAGE_VARIANT = "X-Coin-Package-Variant"
    }
}
