package com.dandi.nyummy.room.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.room.entity.RoomItemCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * 마이룸 화면 상태.
 *
 * @property loadState 방과 보관함을 처음 불러오는 상태
 * @property room 지금 꾸미는 중인 방(저장 전)
 * @property savedRoom 마지막으로 저장된 방. [room]과 다르면 저장할 것이 있다.
 * @property undoStack 되돌리기용 이전 방들. 마지막이 바로 전 상태다.
 * @property selectedItemId 방에서 고른 아이템(테두리와 도구 모음을 보여 준다)
 * @property interiorItems 인테리어 탭 보관함(가진 아이템을 한 개씩 따로). 놓인 것은 [InventoryItemUiModel.isPlaced]
 * @property backgrounds 배경 탭 보관함. 깔린 배경은 [InventoryItemUiModel.isPlaced]
 * @property nextCursors 다음 묶음이 남은 종류와 그 cursor
 */
@Immutable
data class MyRoomUIState(
    val loadState: MyRoomLoadState = MyRoomLoadState.LOADING,
    val room: RoomDraft = RoomDraft(),
    val savedRoom: RoomDraft = RoomDraft(),
    val undoStack: ImmutableList<RoomDraft> = persistentListOf(),
    val selectedItemId: Long? = null,
    val selectedTab: MyRoomTab = MyRoomTab.INTERIOR,
    val interiorItems: ImmutableList<InventoryItemUiModel> = persistentListOf(),
    val backgrounds: ImmutableList<InventoryItemUiModel> = persistentListOf(),
    val nextCursors: ImmutableMap<RoomItemCategory, Long> = persistentMapOf(),
    val isLoadingMore: Boolean = false,
    val isSaving: Boolean = false,
) : UiState {
    val hasChanges: Boolean
        get() = room != savedRoom

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canSave: Boolean
        get() = loadState == MyRoomLoadState.LOADED && hasChanges && !isSaving
}

enum class MyRoomLoadState { LOADING, LOADED, FAILED }

/** 보관함 탭. 인테리어는 여러 개 놓고, 배경은 하나만 깐다. */
enum class MyRoomTab { INTERIOR, BACKGROUND }

/**
 * 꾸미는 중인 방 한 장면. 되돌리기와 저장 여부 비교에 그대로 쓴다.
 *
 * @property background 깔린 배경. 없으면 바닥색만 보인다.
 * @property items 놓인 인테리어. 뒤에서 앞 순서(나중 것이 위에 그려진다).
 */
@Immutable
data class RoomDraft(
    val background: RoomBackgroundUiModel? = null,
    val items: ImmutableList<RoomPlacementUiModel> = persistentListOf(),
)

@Immutable
data class RoomBackgroundUiModel(
    val roomItemId: Long,
    val imageUrl: String,
)

/**
 * 방에 놓인 인테리어 하나. 좌표 규칙은 RoomGeometry에 있다.
 *
 * @property x 바닥 가운데 점의 가로 위치(배경 폭 대비 0~1)
 * @property y 바닥 가운데 점의 세로 위치(배경 높이 대비 0~1)
 * @property scale 기본 크기 대비 배율
 */
@Immutable
data class RoomPlacementUiModel(
    val roomItemId: Long,
    val imageUrl: String,
    val x: Float,
    val y: Float,
    val scale: Float,
)

/** 보관함 칸 하나. 같은 아이템을 여러 개 가졌으면 칸도 여러 개다. */
@Immutable
data class InventoryItemUiModel(
    val roomItemId: Long,
    val category: RoomItemCategory,
    val name: String,
    val imageUrl: String,
    val isPlaced: Boolean,
)
