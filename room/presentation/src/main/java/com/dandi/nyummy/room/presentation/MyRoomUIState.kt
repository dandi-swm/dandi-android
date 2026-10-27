package com.dandi.nyummy.room.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.model.RoomCatMotion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 마이룸 화면 상태.
 *
 * @property loadState 방과 보관함을 처음 불러오는 상태
 * @property room 지금 꾸미는 중인 방(저장 전)
 * @property savedRoom 마지막으로 저장된 방. [room]과 다르면 저장할 것이 있다.
 * @property undoStack 되돌리기용 이전 방들. 마지막이 바로 전 상태다.
 * @property selectedItemId 방에서 고른 아이템(테두리와 도구 모음을 보여 준다)
 * @property selectedCategory 보관함에서 보고 있는 탭(아이템 종류). 방을 불러오면 있는 탭으로 맞춘다.
 * @property inventoryTabs 보관함 탭. 서버가 주는 종류마다 하나씩, 서버가 준 순서대로다. 불러오기 전에는 비어 있다.
 * @property catMotion 방에 앉은 냐미가 지금 하는 동작. 애니메이션을 받지 못했으면 null이고 냐미를 그리지 않는다.
 */
@Immutable
data class MyRoomUIState(
    val loadState: MyRoomLoadState = MyRoomLoadState.LOADING,
    val room: RoomDraft = RoomDraft(),
    val savedRoom: RoomDraft = RoomDraft(),
    val undoStack: ImmutableList<RoomDraft> = persistentListOf(),
    val selectedItemId: Long? = null,
    val selectedCategory: RoomItemCategory = RoomItemCategory.BACKGROUND,
    val inventoryTabs: ImmutableList<InventoryTabUiModel> = persistentListOf(),
    val isSaving: Boolean = false,
    val catMotion: RoomCatMotion? = null,
) : UiState {
    val hasChanges: Boolean
        get() = room != savedRoom

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canSave: Boolean
        get() = loadState == MyRoomLoadState.LOADED && hasChanges && !isSaving

    fun tabOf(category: RoomItemCategory): InventoryTabUiModel? = inventoryTabs.firstOrNull { it.category == category }
}

enum class MyRoomLoadState { LOADING, LOADED, FAILED }

/**
 * 꾸미는 중인 방 한 장면. 되돌리기와 저장 여부 비교에 그대로 쓴다.
 *
 * @property background 깔린 배경. 없으면 기본 빈 방을 깐다.
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
 * 방에 놓인 인테리어 하나. 좌표 규칙은 RoomGeometry에 있다(저장값과 원점, 단위가 다르다).
 *
 * @property x 바닥 가운데 점의 가로 위치(배경 왼쪽 끝 0, 오른쪽 끝 1)
 * @property y 바닥 가운데 점의 세로 위치(배경 위 끝 0, 아래 끝 1)
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

/**
 * 보관함 탭 하나(아이템 종류 하나). 첫 묶음은 방을 불러올 때 같이 받고,
 * 가로로 끝까지 넘기면 이 종류만 다음 묶음을 더 받는다.
 *
 * @property name 서버가 주는 종류 이름(예 "침대"). 비었으면 화면이 정한 이름을 쓴다.
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음까지 받았으면 null.
 * @property isLoadingMore 다음 묶음을 받는 중
 */
@Immutable
data class InventoryTabUiModel(
    val category: RoomItemCategory,
    val name: String = "",
    val items: ImmutableList<InventoryItemUiModel> = persistentListOf(),
    val nextCursor: Long? = null,
    val isLoadingMore: Boolean = false,
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
