package com.dandi.nyummy.room.presentation.tryon

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.model.RoomCatMotion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 놓아보기(사기 전에 내 방에 놓아 보기) 화면 상태.
 *
 * @property loadState 마이룸 방을 처음 불러오는 상태
 * @property room 지금 놓아 보는 방. 마이룸 배경 위에 놓아 본 아이템만 있다(가진 인테리어는 보이지 않는다).
 * @property originalBackground 마이룸에 저장된 배경. 놓아 본 배경을 다시 누르면 이 배경으로 돌아간다.
 * @property undoStack 되돌리기용 이전 방들. 마지막이 바로 전 상태다.
 * @property selectedItemId 방에서 고른 놓아 본 아이템(테두리와 도구 모음을 보여 준다)
 * @property cells 담은 아이템 칸. 배경이 먼저, 그다음 받은 순서다.
 * @property catMotion 방에 앉은 냐미가 지금 하는 동작. 애니메이션을 받지 못했으면 null이고 냐미를 그리지 않는다.
 */
@Immutable
data class RoomTryOnUIState(
    val loadState: MyRoomLoadState = MyRoomLoadState.LOADING,
    val room: RoomDraft = RoomDraft(),
    val originalBackground: RoomBackgroundUiModel? = null,
    val undoStack: ImmutableList<RoomDraft> = persistentListOf(),
    val selectedItemId: Long? = null,
    val cells: ImmutableList<RoomTryOnCellUiModel> = persistentListOf(),
    val catMotion: RoomCatMotion? = null,
) : UiState {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()
}

/**
 * 담은 아이템 칸 하나. 수량이 n이면 칸도 n개다.
 *
 * @property id 방 무대에서 이 칸의 아이템을 가리키는 값. 서버 아이템 id(양수)와 겹치지 않게 음수다.
 * @property price 1개 결제가(코인)
 * @property isPurchasable 지금 살 수 있다. 살 수 없어도 놓아 볼 수 있다.
 * @property isPlaced 방에 놓였다(배경이면 깔렸다).
 */
@Immutable
data class RoomTryOnCellUiModel(
    val key: String,
    val id: Long,
    val name: String,
    val imageUrl: String,
    val isBackground: Boolean,
    val price: Int,
    val isPurchasable: Boolean,
    val isPlaced: Boolean = false,
)
