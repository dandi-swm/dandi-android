package com.dandi.nyummy.room.domain

import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO

interface RoomRepository {

    /** 종류별 가진 아이템의 첫 묶음([size]개씩)과 지금 방에 놓인 아이템. */
    suspend fun getRoom(size: Int): RoomVO

    /** 한 종류의 가진 아이템 다음 묶음. [cursor]는 직전 묶음의 nextCursor다. */
    suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int): OwnedRoomItemPageVO

    /** 방에 놓인 아이템 전체를 [placements]로 바꾼다. 목록에 없는 아이템은 방에서 빠진다. */
    suspend fun saveRoom(placements: List<RoomPlacementVO>)
}
