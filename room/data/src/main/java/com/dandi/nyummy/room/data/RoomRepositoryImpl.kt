package com.dandi.nyummy.room.data

import com.dandi.nyummy.room.data.dto.EquippedRoomItemRequestDTO
import com.dandi.nyummy.room.data.dto.UpdateEquippedRoomItemsRequestDTO
import com.dandi.nyummy.room.domain.RoomRepository
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO

class RoomRepositoryImpl(
    private val dataSource: RoomDataSource,
) : RoomRepository {

    override suspend fun getRoom(size: Int) =
        dataSource.getRoom(size).toVO()

    override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int) =
        dataSource.getOwnedItems(category.name, cursor, size).toVO(category)

    override suspend fun saveRoom(placements: List<RoomPlacementVO>) =
        dataSource.updateEquippedItems(
            UpdateEquippedRoomItemsRequestDTO(equippedRoomItems = placements.map(EquippedRoomItemRequestDTO::from)),
        )
}
