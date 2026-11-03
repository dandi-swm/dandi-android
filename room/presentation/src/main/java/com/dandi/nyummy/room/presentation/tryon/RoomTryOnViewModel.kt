package com.dandi.nyummy.room.presentation.tryon

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.cat.domain.CatMotionPicker
import com.dandi.nyummy.cat.domain.GetCatAnimationsUseCase
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.room.domain.GetRoomUseCase
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomTryOnItemVO
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.model.toItemImageUrl
import com.dandi.nyummy.room.presentation.model.toRoomCatMotion
import com.dandi.nyummy.room.presentation.model.toRoomDraft
import com.dandi.nyummy.room.presentation.model.withBackground
import com.dandi.nyummy.room.presentation.model.withItemInFront
import com.dandi.nyummy.room.presentation.model.withItemMoved
import com.dandi.nyummy.room.presentation.model.withItemPlaced
import com.dandi.nyummy.room.presentation.model.withItemRemoved
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 놓아보기. 마이룸에 깔아 둔 배경만 가져온 빈 방(가진 아이템은 보이지 않는다)에 담은 아이템을 놓아 본다. 서버에 저장하지 않는다.
 *
 * 이 ViewModel은 놓아보기를 띄운 화면(장바구니) 엔트리에 묶여서, 놓아보기를 닫았다 다시 열면 놓아 둔 모습이 그대로 남고
 * 그 화면을 떠나면 함께 지워진다. 그 사이 담은 아이템이 바뀌면 없어진 칸의 아이템만 방에서 뺀다.
 */
@HiltViewModel
class RoomTryOnViewModel @Inject constructor(
    private val getRoom: GetRoomUseCase,
    private val getCatAnimations: GetCatAnimationsUseCase,
    private val catMotionPicker: CatMotionPicker,
) : MviViewModel<RoomTryOnIntent, RoomTryOnUIState, RoomTryOnReducerEvent>(RoomTryOnUIState()) {

    /** 칸 키마다 한 번 정한 방 무대 id. 같은 칸이면 다시 받아도 같은 id라 놓아 둔 아이템이 이어진다. */
    private val idByKey = mutableMapOf<String, Long>()

    /** 방에서 쓰는 냐미 애니메이션(마이룸 상태). 받지 못했으면 null이고 냐미를 그리지 않는다. */
    private var catAnimation: CatAnimationVO? = null

    init {
        load()
        loadCat()
    }

    override fun onIntent(intent: RoomTryOnIntent) {
        when (intent) {
            is RoomTryOnIntent.ItemsChanged -> changeItems(intent.items)
            RoomTryOnIntent.RetryLoad -> load()
            is RoomTryOnIntent.ClickInteriorCell -> clickInteriorCell(intent)
            is RoomTryOnIntent.ClickBackgroundCell -> clickBackgroundCell(intent.id)
            is RoomTryOnIntent.SelectItem -> selectItem(intent.id)
            is RoomTryOnIntent.MoveItem -> moveItem(intent)
            RoomTryOnIntent.BringSelectedToFront -> bringSelectedToFront()
            RoomTryOnIntent.PutSelectedAway -> putSelectedAway()
            RoomTryOnIntent.Undo -> if (currentState.canUndo) dispatch(RoomTryOnReducerEvent.Undone)
            is RoomTryOnIntent.CatMotionFinished -> if (intent.playId == currentState.catMotion?.playId) startCatMotion()
        }
    }

    override fun reduce(state: RoomTryOnUIState, event: RoomTryOnReducerEvent): RoomTryOnUIState =
        when (event) {
            RoomTryOnReducerEvent.LoadStarted -> state.copy(loadState = MyRoomLoadState.LOADING)
            is RoomTryOnReducerEvent.Loaded -> state.copy(
                loadState = MyRoomLoadState.LOADED,
                room = event.room,
                originalBackground = event.room.background,
                undoStack = persistentListOf(),
                selectedItemId = null,
            ).withPlacedMarks()
            RoomTryOnReducerEvent.LoadFailed -> state.copy(loadState = MyRoomLoadState.FAILED)
            is RoomTryOnReducerEvent.CellsChanged -> state.copy(
                cells = event.cells.toImmutableList(),
                room = event.room,
                undoStack = if (event.isRoomTrimmed) persistentListOf() else state.undoStack,
                selectedItemId = state.selectedItemId?.takeIf { id -> event.room.items.any { it.roomItemId == id } },
            ).withPlacedMarks()
            is RoomTryOnReducerEvent.RoomChanged -> state.copy(
                room = event.room,
                undoStack = (state.undoStack + state.room).takeLast(MAX_UNDO).toImmutableList(),
                selectedItemId = event.selectedItemId,
            ).withPlacedMarks()
            RoomTryOnReducerEvent.Undone -> {
                val previous = state.undoStack.last()
                state.copy(
                    room = previous,
                    undoStack = state.undoStack.dropLast(1).toImmutableList(),
                    selectedItemId = state.selectedItemId?.takeIf { id -> previous.items.any { it.roomItemId == id } },
                ).withPlacedMarks()
            }
            is RoomTryOnReducerEvent.ItemSelected -> state.copy(selectedItemId = event.id)
            is RoomTryOnReducerEvent.CatMotionChanged -> state.copy(catMotion = event.motion)
        }

    /**
     * 마이룸에 깔아 둔 배경을 읽는다. 가진 인테리어는 놓아보기에 보이지 않으므로 배경만 쓰고, 가진 아이템 목록은 한 개씩만 받는다.
     * 배경을 깔지 않았으면 기본 빈 방이다.
     */
    private fun load() {
        dispatch(RoomTryOnReducerEvent.LoadStarted)
        viewModelScope.launch {
            getRoom(size = 1)
                .onSuccess { room ->
                    // 불러오는 사이 들어온 칸은 그대로 두고, 방만 마이룸 배경으로 채운다.
                    dispatch(RoomTryOnReducerEvent.Loaded(RoomDraft(background = room.placements.toRoomDraft().background)))
                }
                .onFailure { dispatch(RoomTryOnReducerEvent.LoadFailed) }
        }
    }

    /** 냐미 애니메이션을 읽는다. 마이룸과 같은 상황(ROOM_IDLE)을 쓰고, 실패하면 냐미 없이 놓아 본다. */
    private fun loadCat() {
        viewModelScope.launch {
            getCatAnimations().onSuccess { set ->
                catAnimation = set.animations[CatState.ROOM_IDLE]
                startCatMotion()
            }
        }
    }

    private fun startCatMotion() {
        val animation = catAnimation ?: return
        val previous = currentState.catMotion
        val group = catMotionPicker.nextGroup(animation.motions.size, previous?.group)
        val playId = (previous?.playId ?: 0) + 1
        dispatch(RoomTryOnReducerEvent.CatMotionChanged(animation.toRoomCatMotion(group, playId, catMotionPicker.restMillis())))
    }

    /**
     * 칸을 새로 만든다. 배경이 먼저, 그다음 받은 순서다. 없어진 칸의 아이템은 방에서 빼고, 깔려 있던 놓아 본 배경이
     * 없어졌으면 원래 배경으로 돌린다.
     */
    private fun changeItems(items: List<RoomTryOnItemVO>) {
        val cells = items
            .sortedBy { it.category != RoomItemCategory.BACKGROUND }
            .map { item ->
                RoomTryOnCellUiModel(
                    key = item.key,
                    id = idByKey.getOrPut(item.key) { -(idByKey.size + 1L) },
                    name = item.name,
                    imageUrl = item.itemId.toItemImageUrl(),
                    isBackground = item.category == RoomItemCategory.BACKGROUND,
                    price = item.price,
                    isPurchasable = item.isPurchasable,
                )
            }
        val state = currentState
        val liveIds = cells.mapTo(HashSet()) { it.id }
        val keptItems = state.room.items.filter { it.roomItemId in liveIds }
        val background = state.room.background
        val keptBackground = if (background != null && background.isTryOn() && background.roomItemId !in liveIds) {
            state.originalBackground
        } else {
            background
        }
        val trimmed = keptItems.size != state.room.items.size || keptBackground != background
        val room = if (trimmed) RoomDraft(background = keptBackground, items = keptItems.toImmutableList()) else state.room
        dispatch(RoomTryOnReducerEvent.CellsChanged(cells = cells, room = room, isRoomTrimmed = trimmed))
    }

    private fun clickInteriorCell(intent: RoomTryOnIntent.ClickInteriorCell) {
        val state = currentState
        if (state.loadState != MyRoomLoadState.LOADED) return
        if (state.room.items.any { it.roomItemId == intent.id }) {
            dispatch(RoomTryOnReducerEvent.ItemSelected(intent.id))
            return
        }
        val cell = state.cells.firstOrNull { it.id == intent.id && !it.isBackground } ?: return
        val room = state.room.withItemPlaced(cell.id, cell.imageUrl, intent.x, intent.y) ?: return
        dispatch(RoomTryOnReducerEvent.RoomChanged(room, selectedItemId = cell.id))
    }

    /** 다른 배경이면 그 배경으로 바꾸고, 이미 깔린 배경이면 마이룸 원래 배경으로 돌린다. */
    private fun clickBackgroundCell(id: Long) {
        val state = currentState
        if (state.loadState != MyRoomLoadState.LOADED) return
        val cell = state.cells.firstOrNull { it.id == id && it.isBackground } ?: return
        val target = if (state.room.background?.roomItemId == id) state.originalBackground else RoomBackgroundUiModel(cell.id, cell.imageUrl)
        val room = state.room.withBackground(target) ?: return
        dispatch(RoomTryOnReducerEvent.RoomChanged(room, selectedItemId = state.selectedItemId))
    }

    private fun selectItem(id: Long?) {
        if (id != null && currentState.room.items.none { it.roomItemId == id }) return
        dispatch(RoomTryOnReducerEvent.ItemSelected(id))
    }

    private fun moveItem(intent: RoomTryOnIntent.MoveItem) {
        val state = currentState
        val room = state.room.withItemMoved(intent.id, intent.x, intent.y, intent.scale) ?: return
        if (room == state.room) {
            dispatch(RoomTryOnReducerEvent.ItemSelected(intent.id))
            return
        }
        dispatch(RoomTryOnReducerEvent.RoomChanged(room, selectedItemId = intent.id))
    }

    private fun bringSelectedToFront() {
        val state = currentState
        val selectedId = state.selectedItemId ?: return
        val room = state.room.withItemInFront(selectedId) ?: return
        dispatch(RoomTryOnReducerEvent.RoomChanged(room, selectedItemId = selectedId))
    }

    private fun putSelectedAway() {
        val state = currentState
        val selectedId = state.selectedItemId ?: return
        val room = state.room.withItemRemoved(selectedId) ?: return
        dispatch(RoomTryOnReducerEvent.RoomChanged(room, selectedItemId = null))
    }

    private companion object {
        /** 되돌리기로 기억하는 최대 단계(마이룸과 같다). */
        const val MAX_UNDO = 50
    }
}

/** 놓아 본 배경이다(칸 id는 음수, 서버 아이템 id는 양수). */
private fun RoomBackgroundUiModel.isTryOn(): Boolean = roomItemId < 0

/** 칸의 놓임 표시를 지금 방에 맞춘다. 배경은 깔린 하나만, 인테리어는 방에 놓인 칸 모두. */
private fun RoomTryOnUIState.withPlacedMarks(): RoomTryOnUIState {
    val placedIds = room.items.mapTo(HashSet()) { it.roomItemId }
    val backgroundId = room.background?.roomItemId
    return copy(
        cells = cells.map { cell ->
            cell.copy(isPlaced = if (cell.isBackground) cell.id == backgroundId else cell.id in placedIds)
        }.toImmutableList(),
    )
}
