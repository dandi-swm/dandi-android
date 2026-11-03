package com.dandi.nyummy.room.presentation.tryon

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.room.entity.RoomTryOnItemVO

sealed interface RoomTryOnIntent : MviIntent {
    /** 담은 아이템이 들어왔거나 바뀌었다(장바구니에서 빼거나 수량을 줄였다). 없어진 칸의 아이템은 방에서도 뺀다. */
    data class ItemsChanged(val items: List<RoomTryOnItemVO>) : RoomTryOnIntent

    data object RetryLoad : RoomTryOnIntent

    /** 인테리어 칸. 놓이지 않았으면 ([x], [y]) 바닥 점에 놓고, 놓였으면 방에서 고른다. 자리는 화면이 정한다. */
    data class ClickInteriorCell(val id: Long, val x: Float, val y: Float) : RoomTryOnIntent

    /** 배경 칸. 방 배경을 그 배경으로 바꾸고, 이미 그 배경이면 원래 배경으로 돌린다. */
    data class ClickBackgroundCell(val id: Long) : RoomTryOnIntent

    /** 방을 눌러 놓아 본 아이템을 고르거나(null이면 고르기 해제) 한다. */
    data class SelectItem(val id: Long?) : RoomTryOnIntent

    /** 끌기나 두 손가락 확대, 축소가 끝났을 때 아이템의 새 자리와 크기. 바뀌었으면 그 아이템은 맨 앞으로 온다. */
    data class MoveItem(val id: Long, val x: Float, val y: Float, val scale: Float) : RoomTryOnIntent

    data object BringSelectedToFront : RoomTryOnIntent

    /** 고른 아이템을 방에서 빼 칸으로 되돌린다. */
    data object PutSelectedAway : RoomTryOnIntent

    data object Undo : RoomTryOnIntent

    /** 냐미 동작 하나가 끝났다. 같은 상태의 다른 동작을 이어 간다. */
    data class CatMotionFinished(val playId: Int) : RoomTryOnIntent
}
