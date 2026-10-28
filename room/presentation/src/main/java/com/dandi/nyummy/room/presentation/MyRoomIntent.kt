package com.dandi.nyummy.room.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.room.entity.RoomItemCategory

sealed interface MyRoomIntent : MviIntent {
    /** 상단 뒤로가기나 시스템 뒤로가기. 저장하지 않은 변경이 있으면 저장할지 묻는다. */
    data object ClickBack : MyRoomIntent

    data object ClickSave : MyRoomIntent

    data object RetryLoad : MyRoomIntent

    /** 보관함 탭(아이템 종류)을 바꾼다. */
    data class SelectTab(val category: RoomItemCategory) : MyRoomIntent

    /**
     * 인테리어 칸. 놓이지 않은 아이템이면 ([x], [y]) 바닥 점에 놓고, 놓인 아이템이면 방에서 고른다.
     * 자리는 화면이 정한다(지금 보이는 방 가운데 바닥, 시트에 가리지 않는 곳).
     */
    data class ClickInteriorItem(val roomItemId: Long, val x: Float, val y: Float) : MyRoomIntent

    data class ClickBackground(val roomItemId: Long) : MyRoomIntent

    /** 방을 눌러 아이템을 고르거나(null이면 고르기 해제) 한다. */
    data class SelectItem(val roomItemId: Long?) : MyRoomIntent

    /** 끌기나 두 손가락 확대, 축소가 끝났을 때 아이템의 새 자리와 크기. 바뀌었으면 그 아이템은 맨 앞으로 온다. */
    data class MoveItem(val roomItemId: Long, val x: Float, val y: Float, val scale: Float) : MyRoomIntent

    /** 고른 아이템을 맨 앞으로. */
    data object BringSelectedToFront : MyRoomIntent

    /** 고른 아이템을 방에서 빼 보관함에 넣는다. */
    data object PutSelectedAway : MyRoomIntent

    data object Undo : MyRoomIntent

    /** 보관함 [category] 탭을 끝 가까이까지 넘겼다. 남은 묶음이 있으면 더 받는다. */
    data class ReachInventoryEnd(val category: RoomItemCategory) : MyRoomIntent

    data object ClickShop : MyRoomIntent

    /** 냐미 동작 하나가 끝났다. 같은 상태의 다른 동작을 이어 간다. */
    data class CatMotionFinished(val playId: Int) : MyRoomIntent

    /** 튜토리얼 소개 카드를 눌렀다. 다음 대사로, 마지막이면 마이룸 단계를 마치고 홈으로 돌아간다. */
    data object TutorialAdvance : MyRoomIntent

    /** 튜토리얼이 가리킨 칸을 눌렀다. 놓기는 칸이 하고, 튜토리얼은 저장 안내로 간다. */
    data object TutorialTargetClicked : MyRoomIntent

    /** 화면 오른쪽 위 건너뛰기를 눌렀다. 건너뛰기 확인을 띄운다. */
    data object TutorialSkip : MyRoomIntent

    /** 건너뛰기 확인에서 계속 배우기로 했다(바깥·뒤로 가기 포함). */
    data object TutorialSkipCancelled : MyRoomIntent

    /** 건너뛰기 확인에서 그래도 건너뛰기로 했다. 홈의 냐미 건강 이야기로 돌아간다. */
    data object TutorialSkipConfirmed : MyRoomIntent
}
