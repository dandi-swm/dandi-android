package com.dandi.nyummy.room.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.room.data.dto.OwnedRoomItemsResponseDTO
import com.dandi.nyummy.room.data.dto.RoomResponseDTO
import com.dandi.nyummy.room.data.dto.UpdateEquippedRoomItemsRequestDTO

class RoomDataSource(
    private val apiService: RoomApiService,
) : BaseRemoteDataSource() {
    suspend fun getRoom(size: Int): RoomResponseDTO =
        checkResponse(apiService.getRoom(size))

    suspend fun getOwnedItems(category: String, cursor: Long?, size: Int): OwnedRoomItemsResponseDTO =
        checkResponse(apiService.getOwnedItems(category, cursor, size))

    suspend fun updateEquippedItems(request: UpdateEquippedRoomItemsRequestDTO) =
        checkResponse(apiService.updateEquippedItems(request))
}
