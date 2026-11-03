package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.data.dto.CoinHistoriesDTO
import retrofit2.Response
import retrofit2.http.GET
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
}
