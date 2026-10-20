package com.dandi.nyummy.room.data

import com.dandi.nyummy.room.data.dto.OwnedRoomItemsResponseDTO
import com.dandi.nyummy.room.data.dto.RoomResponseDTO
import com.dandi.nyummy.room.data.dto.UpdateEquippedRoomItemsRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * 마이룸 API
 */
interface RoomApiService {
    /** 마이룸 조회(종류별 가진 아이템 첫 묶음, 놓인 아이템) */
    @GET(ROOMS_PATH)
    suspend fun getRoom(@Query("size") size: Int): Response<RoomResponseDTO>

    /** 보유 아이템 목록 조회(한 종류의 다음 묶음) */
    @GET("$ROOMS_PATH/owned-items")
    suspend fun getOwnedItems(
        @Query("category") category: String,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<OwnedRoomItemsResponseDTO>

    /** 꾸민 방 저장 */
    @PUT("$ROOMS_PATH/equipped-items")
    suspend fun updateEquippedItems(@Body request: UpdateEquippedRoomItemsRequestDTO): Response<Unit>

    companion object {
        const val ROOMS_PATH = "/api/v1/rooms"
    }
}
