package com.dandi.nyummy.room.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.room.presentation.model.RoomCatMotion

sealed interface MyRoomReducerEvent : ReducerEvent {
    data object LoadStarted : MyRoomReducerEvent

    data class Loaded(val room: RoomVO) : MyRoomReducerEvent

    data object LoadFailed : MyRoomReducerEvent

    data class TabSelected(val category: RoomItemCategory) : MyRoomReducerEvent

    /** 방이 바뀌었다. 바뀌기 전 방은 되돌리기 목록에 쌓인다. */
    data class RoomChanged(val room: RoomDraft, val selectedItemId: Long?) : MyRoomReducerEvent

    data object Undone : MyRoomReducerEvent

    data class ItemSelected(val roomItemId: Long?) : MyRoomReducerEvent

    data class MoreItemsLoadStarted(val category: RoomItemCategory) : MyRoomReducerEvent

    data class MoreItemsLoaded(val page: OwnedRoomItemPageVO) : MyRoomReducerEvent

    data class MoreItemsLoadStopped(val category: RoomItemCategory) : MyRoomReducerEvent

    data object SaveStarted : MyRoomReducerEvent

    /** 저장이 끝났다. 성공하면 지금 방이 저장된 방이 된다. */
    data class SaveFinished(val isSaved: Boolean) : MyRoomReducerEvent

    data class CatMotionChanged(val motion: RoomCatMotion) : MyRoomReducerEvent
}
