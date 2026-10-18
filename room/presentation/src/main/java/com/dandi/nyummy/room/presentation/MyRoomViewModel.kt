package com.dandi.nyummy.room.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.room.domain.GetOwnedRoomItemsUseCase
import com.dandi.nyummy.room.domain.GetRoomUseCase
import com.dandi.nyummy.room.domain.RoomItemGoneException
import com.dandi.nyummy.room.domain.SaveRoomUseCase
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.OwnedRoomItemVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.room.presentation.model.RoomGeometry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRoomViewModel @Inject constructor(
    private val getRoom: GetRoomUseCase,
    private val getOwnedRoomItems: GetOwnedRoomItemsUseCase,
    private val saveRoom: SaveRoomUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
) : MviViewModel<MyRoomIntent, MyRoomUIState, MyRoomReducerEvent>(MyRoomUIState()) {

    init {
        load()
    }

    override fun onIntent(intent: MyRoomIntent) {
        when (intent) {
            MyRoomIntent.ClickBack -> clickBack()
            MyRoomIntent.ClickSave -> save()
            MyRoomIntent.RetryLoad -> load()
            is MyRoomIntent.SelectTab -> dispatch(MyRoomReducerEvent.TabSelected(intent.tab))
            is MyRoomIntent.ClickInteriorItem -> clickInteriorItem(intent.roomItemId)
            is MyRoomIntent.ClickBackground -> changeBackground(intent.roomItemId)
            is MyRoomIntent.SelectItem -> dispatch(MyRoomReducerEvent.ItemSelected(intent.roomItemId))
            is MyRoomIntent.MoveItem -> moveItem(intent)
            MyRoomIntent.BringSelectedToFront -> bringSelectedToFront()
            MyRoomIntent.PutSelectedAway -> putSelectedAway()
            MyRoomIntent.Undo -> if (currentState.canUndo) dispatch(MyRoomReducerEvent.Undone)
            MyRoomIntent.ReachInventoryEnd -> loadMoreItems()
            // TODO: 상점 화면이 생기면 ShopPage로 연결한다.
            MyRoomIntent.ClickShop -> messageHelper.showSnackBar(iconType = IconType.WARNING, messageText = NOT_READY_MESSAGE)
        }
    }

    override fun reduce(state: MyRoomUIState, event: MyRoomReducerEvent): MyRoomUIState =
        when (event) {
            MyRoomReducerEvent.LoadStarted -> state.copy(loadState = MyRoomLoadState.LOADING)
            is MyRoomReducerEvent.Loaded -> loadedState(state, event.room)
            MyRoomReducerEvent.LoadFailed -> state.copy(loadState = MyRoomLoadState.FAILED)
            is MyRoomReducerEvent.TabSelected -> state.copy(selectedTab = event.tab)
            is MyRoomReducerEvent.RoomChanged -> state.copy(
                room = event.room,
                undoStack = (state.undoStack + state.room).takeLast(MAX_UNDO).toImmutableList(),
                selectedItemId = event.selectedItemId,
            ).withPlacedMarks()
            MyRoomReducerEvent.Undone -> {
                val previous = state.undoStack.last()
                state.copy(
                    room = previous,
                    undoStack = state.undoStack.dropLast(1).toImmutableList(),
                    selectedItemId = state.selectedItemId?.takeIf { id -> previous.items.any { it.roomItemId == id } },
                ).withPlacedMarks()
            }
            is MyRoomReducerEvent.ItemSelected -> state.copy(selectedItemId = event.roomItemId)
            MyRoomReducerEvent.MoreItemsLoadStarted -> state.copy(isLoadingMore = true)
            is MyRoomReducerEvent.MoreItemsLoaded -> withMoreItems(state, event.page)
            MyRoomReducerEvent.MoreItemsLoadStopped -> state.copy(isLoadingMore = false)
            MyRoomReducerEvent.SaveStarted -> state.copy(isSaving = true)
            is MyRoomReducerEvent.SaveFinished -> state.copy(
                isSaving = false,
                savedRoom = if (event.isSaved) state.room else state.savedRoom,
            )
        }

    private fun load() {
        dispatch(MyRoomReducerEvent.LoadStarted)
        viewModelScope.launch {
            getRoom()
                .onSuccess { dispatch(MyRoomReducerEvent.Loaded(it)) }
                .onFailure { dispatch(MyRoomReducerEvent.LoadFailed) }
        }
    }

    /** 다음 묶음이 남은 종류를 하나씩 이어 읽는다. 실패하면 거기서 멈추고 다음에 끝에 닿을 때 다시 시도한다. */
    private fun loadMoreItems() {
        val state = currentState
        if (state.isLoadingMore || state.loadState != MyRoomLoadState.LOADED) return
        val (category, cursor) = state.nextCursors.entries.firstOrNull()?.toPair() ?: return
        dispatch(MyRoomReducerEvent.MoreItemsLoadStarted)
        viewModelScope.launch {
            getOwnedRoomItems(category, cursor)
                .onSuccess { dispatch(MyRoomReducerEvent.MoreItemsLoaded(it)) }
            dispatch(MyRoomReducerEvent.MoreItemsLoadStopped)
        }
    }

    private fun clickInteriorItem(roomItemId: Long) {
        val state = currentState
        if (state.room.items.any { it.roomItemId == roomItemId }) {
            dispatch(MyRoomReducerEvent.ItemSelected(roomItemId))
            return
        }
        val item = state.interiorItems.firstOrNull { it.roomItemId == roomItemId } ?: return
        val placed = RoomPlacementUiModel(
            roomItemId = item.roomItemId,
            imageUrl = item.imageUrl,
            x = RoomGeometry.NEW_ITEM_X,
            y = RoomGeometry.NEW_ITEM_Y,
            scale = 1f,
        )
        changeRoom(state.room.copy(items = (state.room.items + placed).toImmutableList()), selectedItemId = roomItemId)
    }

    private fun changeBackground(roomItemId: Long) {
        val state = currentState
        if (state.room.background?.roomItemId == roomItemId) return
        val background = state.backgrounds.firstOrNull { it.roomItemId == roomItemId } ?: return
        changeRoom(
            state.room.copy(background = RoomBackgroundUiModel(background.roomItemId, background.imageUrl)),
            selectedItemId = state.selectedItemId,
        )
    }

    private fun moveItem(intent: MyRoomIntent.MoveItem) {
        val state = currentState
        val scale = RoomGeometry.clampScale(intent.scale)
        val moved = state.room.items.map {
            if (it.roomItemId == intent.roomItemId) it.copy(x = intent.x, y = intent.y, scale = scale) else it
        }
        if (moved == state.room.items) {
            dispatch(MyRoomReducerEvent.ItemSelected(intent.roomItemId))
            return
        }
        changeRoom(state.room.copy(items = moved.toImmutableList()), selectedItemId = intent.roomItemId)
    }

    private fun bringSelectedToFront() {
        val state = currentState
        val selected = state.room.items.firstOrNull { it.roomItemId == state.selectedItemId } ?: return
        if (state.room.items.last() == selected) return
        changeRoom(
            state.room.copy(items = (state.room.items - selected + selected).toImmutableList()),
            selectedItemId = selected.roomItemId,
        )
    }

    private fun putSelectedAway() {
        val state = currentState
        val selectedId = state.selectedItemId ?: return
        changeRoom(
            state.room.copy(items = state.room.items.filterNot { it.roomItemId == selectedId }.toImmutableList()),
            selectedItemId = null,
        )
    }

    private fun changeRoom(room: RoomDraft, selectedItemId: Long?) {
        dispatch(MyRoomReducerEvent.RoomChanged(room, selectedItemId))
    }

    private fun clickBack() {
        val state = currentState
        if (!state.hasChanges || state.loadState != MyRoomLoadState.LOADED) {
            navigationHelper.navigateToBack()
            return
        }
        messageHelper.showTwoButtonDialog(
            descText = SAVE_CONFIRM_MESSAGE,
            leftButtonText = SAVE_CONFIRM_DISCARD,
            onClickLeftButton = navigationHelper::navigateToBack,
            rightButtonText = SAVE_CONFIRM_SAVE,
            onClickRightButton = ::save,
        )
    }

    /** 꾸민 방을 저장하고, 성공하면 들어온 화면(홈)으로 돌아가 저장했다고 알린다. */
    private fun save() {
        val state = currentState
        if (!state.canSave) return
        dispatch(MyRoomReducerEvent.SaveStarted)
        viewModelScope.launch {
            saveRoom(state.room.toPlacements())
                .onSuccess {
                    dispatch(MyRoomReducerEvent.SaveFinished(isSaved = true))
                    navigationHelper.navigateToBack()
                    messageHelper.showSnackBar(iconType = IconType.SUCCESS, messageText = SAVED_MESSAGE)
                }
                .onFailure { e ->
                    dispatch(MyRoomReducerEvent.SaveFinished(isSaved = false))
                    if (e is RoomItemGoneException) {
                        messageHelper.showSnackBar(iconType = IconType.WARNING, messageText = ITEM_GONE_MESSAGE)
                        load()
                    } else {
                        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = SAVE_FAILED_MESSAGE)
                    }
                }
        }
    }

    private companion object {
        /** 되돌리기로 기억하는 최대 단계. */
        const val MAX_UNDO = 50

        const val SAVE_CONFIRM_MESSAGE = "꾸민 방을 저장할까요?"
        const val SAVE_CONFIRM_DISCARD = "저장 안 함"
        const val SAVE_CONFIRM_SAVE = "저장하기"
        const val SAVED_MESSAGE = "방을 저장했어요"
        const val SAVE_FAILED_MESSAGE = "방을 저장하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val ITEM_GONE_MESSAGE = "가지고 있지 않은 아이템이 있어 방을 다시 불러왔어요"
        const val NOT_READY_MESSAGE = "준비 중인 기능이에요."
    }
}

/** 서버에서 받은 방으로 화면 상태를 처음부터 만든다. 되돌리기 목록과 고른 아이템은 비운다. */
private fun loadedState(state: MyRoomUIState, room: RoomVO): MyRoomUIState {
    val draft = room.placements.toDraft()
    val categories = room.categories
    return state.copy(
        loadState = MyRoomLoadState.LOADED,
        room = draft,
        savedRoom = draft,
        undoStack = persistentListOf(),
        selectedItemId = null,
        interiorItems = categories.filter { it.category != RoomItemCategory.BACKGROUND }
            .flatMap { page -> page.items.map { it.toInventoryItem(page.category) } }
            .toImmutableList(),
        backgrounds = categories.filter { it.category == RoomItemCategory.BACKGROUND }
            .flatMap { page -> page.items.map { it.toInventoryItem(page.category) } }
            .toImmutableList(),
        nextCursors = categories.mapNotNull { page -> page.nextCursor?.let { page.category to it } }
            .toMap()
            .toPersistentMap(),
        isLoadingMore = false,
    ).withPlacedMarks()
}

/** 다음 묶음을 같은 종류의 마지막 칸 뒤에 이어 붙인다(보관함은 종류 순서를 지킨다). */
private fun withMoreItems(state: MyRoomUIState, page: OwnedRoomItemPageVO): MyRoomUIState {
    val newItems = page.items.map { it.toInventoryItem(page.category) }
    fun List<InventoryItemUiModel>.insert(): List<InventoryItemUiModel> {
        val known = mapTo(HashSet()) { it.roomItemId }
        val fresh = newItems.filterNot { it.roomItemId in known }
        val at = indexOfLast { it.category == page.category } + 1
        return if (at == 0) this + fresh else take(at) + fresh + drop(at)
    }
    val cursors = state.nextCursors.toMutableMap().apply {
        page.nextCursor?.let { put(page.category, it) } ?: remove(page.category)
    }
    return state.copy(
        interiorItems = if (page.category == RoomItemCategory.BACKGROUND) state.interiorItems else state.interiorItems.insert().toImmutableList(),
        backgrounds = if (page.category == RoomItemCategory.BACKGROUND) state.backgrounds.insert().toImmutableList() else state.backgrounds,
        nextCursors = cursors.toPersistentMap(),
    ).withPlacedMarks()
}

/** 보관함 칸의 놓임 표시를 지금 방에 맞춘다. */
private fun MyRoomUIState.withPlacedMarks(): MyRoomUIState {
    val placedIds = room.items.mapTo(HashSet()) { it.roomItemId }
    val backgroundId = room.background?.roomItemId
    return copy(
        interiorItems = interiorItems.map { it.copy(isPlaced = it.roomItemId in placedIds) }.toImmutableList(),
        backgrounds = backgrounds.map { it.copy(isPlaced = it.roomItemId == backgroundId) }.toImmutableList(),
    )
}

/**
 * 서버의 놓인 아이템을 꾸미는 방으로 바꾼다. 배경은 하나만 쓰고, 좌표와 크기는 허용 범위로 맞춘다
 * (고정 응답처럼 범위를 벗어난 값이 와도 방 안에 보이게).
 */
private fun List<RoomPlacementVO>.toDraft(): RoomDraft {
    val background = firstOrNull { it.category == RoomItemCategory.BACKGROUND }
    return RoomDraft(
        background = background?.let { RoomBackgroundUiModel(it.roomItemId, it.imageUrl) },
        items = filter { it.category != RoomItemCategory.BACKGROUND }
            .sortedBy { it.zIndex }
            .map {
                RoomPlacementUiModel(
                    roomItemId = it.roomItemId,
                    imageUrl = it.imageUrl,
                    x = it.x.coerceIn(0f, 1f),
                    y = it.y.coerceIn(0f, 1f),
                    scale = RoomGeometry.clampScale(it.scale),
                )
            }
            .toImmutableList(),
    )
}

/** 저장할 놓인 아이템 전체. 배경이 맨 뒤(zIndex 0)이고 인테리어는 1부터 그리는 순서대로다. */
internal fun RoomDraft.toPlacements(): List<RoomPlacementVO> = buildList {
    background?.let {
        add(RoomPlacementVO(roomItemId = it.roomItemId, category = RoomItemCategory.BACKGROUND, imageUrl = it.imageUrl))
    }
    items.forEachIndexed { index, item ->
        add(
            RoomPlacementVO(
                roomItemId = item.roomItemId,
                imageUrl = item.imageUrl,
                x = item.x,
                y = item.y,
                zIndex = index + 1,
                scale = item.scale,
            ),
        )
    }
}

private fun OwnedRoomItemVO.toInventoryItem(category: RoomItemCategory) = InventoryItemUiModel(
    roomItemId = roomItemId,
    category = category,
    name = name,
    imageUrl = imageUrl,
    isPlaced = false,
)
