package com.dandi.nyummy.room.presentation.tryon

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.model.RoomCatMotion

sealed interface RoomTryOnReducerEvent : ReducerEvent {
    data object LoadStarted : RoomTryOnReducerEvent

    /** 마이룸 배경을 불러왔다([room]에는 배경만 있다). 되돌리기와 고르기는 비운다. */
    data class Loaded(val room: RoomDraft) : RoomTryOnReducerEvent

    data object LoadFailed : RoomTryOnReducerEvent

    /**
     * 담은 아이템 칸이 바뀌었다. [room]은 없어진 칸의 아이템을 뺀 방이고, [isRoomTrimmed]면 되돌리기를 비우고
     * 고른 아이템이 빠졌으면 고르기를 푼다(되돌리기로 없는 칸이 되살아나지 않게).
     */
    data class CellsChanged(
        val cells: List<RoomTryOnCellUiModel>,
        val room: RoomDraft,
        val isRoomTrimmed: Boolean,
    ) : RoomTryOnReducerEvent

    /** 방이 바뀌었다. 바뀌기 전 방은 되돌리기 목록에 쌓인다. */
    data class RoomChanged(val room: RoomDraft, val selectedItemId: Long?) : RoomTryOnReducerEvent

    data object Undone : RoomTryOnReducerEvent

    data class ItemSelected(val id: Long?) : RoomTryOnReducerEvent

    data class CatMotionChanged(val motion: RoomCatMotion) : RoomTryOnReducerEvent
}
