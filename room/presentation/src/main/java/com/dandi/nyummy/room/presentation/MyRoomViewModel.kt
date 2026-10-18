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
            is MyRoomIntent.SelectTab -> dispatch(MyRoomReducerEvent.TabSelected(intent.category))
            is MyRoomIntent.ClickInteriorItem -> clickInteriorItem(intent)
            is MyRoomIntent.ClickBackground -> changeBackground(intent.roomItemId)
            is MyRoomIntent.SelectItem -> dispatch(MyRoomReducerEvent.ItemSelected(intent.roomItemId))
            is MyRoomIntent.MoveItem -> moveItem(intent)
            MyRoomIntent.BringSelectedToFront -> bringSelectedToFront()
            MyRoomIntent.PutSelectedAway -> putSelectedAway()
            MyRoomIntent.Undo -> if (currentState.canUndo) dispatch(MyRoomReducerEvent.Undone)
            is MyRoomIntent.ReachInventoryEnd -> loadMoreItems(intent.category)
            // TODO: 상점 화면이 생기면 ShopPage로 연결한다.
            MyRoomIntent.ClickShop -> messageHelper.showSnackBar(iconType = IconType.WARNING, messageText = NOT_READY_MESSAGE)
        }
    }

    override fun reduce(state: MyRoomUIState, event: MyRoomReducerEvent): MyRoomUIState =
        when (event) {
            MyRoomReducerEvent.LoadStarted -> state.copy(loadState = MyRoomLoadState.LOADING)
            is MyRoomReducerEvent.Loaded -> loadedState(state, event.room)
            MyRoomReducerEvent.LoadFailed -> state.copy(loadState = MyRoomLoadState.FAILED)
            is MyRoomReducerEvent.TabSelected -> state.copy(selectedCategory = event.category)
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
            is MyRoomReducerEvent.MoreItemsLoadStarted -> state.updateTab(event.category) { it.copy(isLoadingMore = true) }
            is MyRoomReducerEvent.MoreItemsLoaded -> withMoreItems(state, event.page)
            is MyRoomReducerEvent.MoreItemsLoadStopped -> state.updateTab(event.category) { it.copy(isLoadingMore = false) }
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

    /**
     * [category] 탭의 다음 묶음을 받는다. 첫 묶음은 방과 함께 받았으니 그 뒤 묶음만 종류별 목록으로 받는다.
     * 실패하면 지금 칸은 그대로 두고, 다음에 끝에 닿을 때 다시 시도한다.
     */
    private fun loadMoreItems(category: RoomItemCategory) {
        val state = currentState
        if (state.loadState != MyRoomLoadState.LOADED) return
        val tab = state.tabOf(category) ?: return
        val cursor = tab.nextCursor ?: return
        if (tab.isLoadingMore) return
        dispatch(MyRoomReducerEvent.MoreItemsLoadStarted(category))
        viewModelScope.launch {
            getOwnedRoomItems(category, cursor)
                .onSuccess { dispatch(MyRoomReducerEvent.MoreItemsLoaded(it.copy(category = category))) }
            dispatch(MyRoomReducerEvent.MoreItemsLoadStopped(category))
        }
    }

    private fun clickInteriorItem(intent: MyRoomIntent.ClickInteriorItem) {
        val state = currentState
        if (state.room.items.any { it.roomItemId == intent.roomItemId }) {
            dispatch(MyRoomReducerEvent.ItemSelected(intent.roomItemId))
            return
        }
        val item = state.inventoryTabs
            .filter { it.category != RoomItemCategory.BACKGROUND }
            .firstNotNullOfOrNull { tab -> tab.items.firstOrNull { it.roomItemId == intent.roomItemId } }
            ?: return
        val placed = RoomPlacementUiModel(
            roomItemId = item.roomItemId,
            imageUrl = item.imageUrl,
            x = intent.x.coerceIn(0f, 1f),
            y = intent.y.coerceIn(0f, 1f),
            scale = 1f,
        )
        changeRoom(state.room.copy(items = (state.room.items + placed).toImmutableList()), selectedItemId = item.roomItemId)
    }

    private fun changeBackground(roomItemId: Long) {
        val state = currentState
        if (state.room.background?.roomItemId == roomItemId) return
        val background = state.tabOf(RoomItemCategory.BACKGROUND)?.items?.firstOrNull { it.roomItemId == roomItemId } ?: return
        changeRoom(
            state.room.copy(background = RoomBackgroundUiModel(background.roomItemId, background.imageUrl)),
            selectedItemId = state.selectedItemId,
        )
    }

    /** 옮기거나 크기를 바꾼 아이템은 맨 앞으로 온다(가장 최근에 만진 아이템이 위에 보인다). */
    private fun moveItem(intent: MyRoomIntent.MoveItem) {
        val state = currentState
        val target = state.room.items.firstOrNull { it.roomItemId == intent.roomItemId } ?: return
        val moved = target.copy(x = intent.x, y = intent.y, scale = RoomGeometry.clampScale(intent.scale))
        if (moved == target) {
            dispatch(MyRoomReducerEvent.ItemSelected(intent.roomItemId))
            return
        }
        changeRoom(
            state.room.copy(items = (state.room.items - target + moved).toImmutableList()),
            selectedItemId = intent.roomItemId,
        )
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

/**
 * 서버에서 받은 방으로 화면 상태를 처음부터 만든다. 되돌리기 목록과 고른 아이템은 비운다.
 * 보관함 탭은 종류마다 하나씩 늘 다섯 개이고, 각 탭에는 방과 함께 받은 첫 묶음을 넣는다.
 */
private fun loadedState(state: MyRoomUIState, room: RoomVO): MyRoomUIState {
    val draft = room.placements.toDraft()
    return state.copy(
        loadState = MyRoomLoadState.LOADED,
        room = draft,
        savedRoom = draft,
        undoStack = persistentListOf(),
        selectedItemId = null,
        inventoryTabs = RoomItemCategory.entries.map { category ->
            // 모르는 종류는 기타로 바뀌어 오므로 같은 종류 묶음이 여럿일 수 있다. 다음 묶음은 첫 묶음의 cursor로 받는다.
            val pages = room.categories.filter { it.category == category }
            InventoryTabUiModel(
                category = category,
                name = pages.firstOrNull()?.categoryName.orEmpty(),
                items = pages.flatMap { page -> page.items.map { it.toInventoryItem(category) } }
                    .distinctBy { it.roomItemId }
                    .toImmutableList(),
                nextCursor = pages.firstOrNull()?.nextCursor,
            )
        }.toImmutableList(),
    ).withPlacedMarks()
}

/** 다음 묶음을 그 종류 탭의 끝에 이어 붙인다. 이미 있는 칸(묶음 사이에 겹친 칸)은 빼고 붙인다. */
private fun withMoreItems(state: MyRoomUIState, page: OwnedRoomItemPageVO): MyRoomUIState =
    state.updateTab(page.category) { tab ->
        val known = tab.items.mapTo(HashSet()) { it.roomItemId }
        val fresh = page.items.filterNot { it.roomItemId in known }.map { it.toInventoryItem(page.category) }
        tab.copy(items = (tab.items + fresh).toImmutableList(), nextCursor = page.nextCursor)
    }.withPlacedMarks()

private fun MyRoomUIState.updateTab(
    category: RoomItemCategory,
    transform: (InventoryTabUiModel) -> InventoryTabUiModel,
): MyRoomUIState = copy(
    inventoryTabs = inventoryTabs.map { if (it.category == category) transform(it) else it }.toImmutableList(),
)

/** 보관함 칸의 놓임 표시를 지금 방에 맞춘다. 배경은 깔린 하나만, 인테리어는 방에 놓인 칸 모두. */
private fun MyRoomUIState.withPlacedMarks(): MyRoomUIState {
    val placedIds = room.items.mapTo(HashSet()) { it.roomItemId }
    val backgroundId = room.background?.roomItemId
    return copy(
        inventoryTabs = inventoryTabs.map { tab ->
            tab.copy(
                items = tab.items.map { item ->
                    val isPlaced = if (tab.category == RoomItemCategory.BACKGROUND) {
                        item.roomItemId == backgroundId
                    } else {
                        item.roomItemId in placedIds
                    }
                    item.copy(isPlaced = isPlaced)
                }.toImmutableList(),
            )
        }.toImmutableList(),
    )
}

/**
 * 서버의 놓인 아이템을 꾸미는 방으로 바꾼다. 배경은 하나만 쓴다. 좌표는 저장값(그림 왼쪽 아래 기준 퍼센트)을
 * 그리는 좌표로 바꾸고, 좌표와 크기는 허용 범위로 맞춘다(범위를 벗어난 값이 와도 방 안에 보이게).
 */
private fun List<RoomPlacementVO>.toDraft(): RoomDraft {
    val background = firstOrNull { it.category == RoomItemCategory.BACKGROUND }
    return RoomDraft(
        background = background?.let { RoomBackgroundUiModel(it.roomItemId, it.imageUrl) },
        items = filter { it.category != RoomItemCategory.BACKGROUND }
            .sortedBy { it.zIndex }
            .map {
                val position = RoomGeometry.fromSavedPosition(it.x, it.y)
                RoomPlacementUiModel(
                    roomItemId = it.roomItemId,
                    imageUrl = it.imageUrl,
                    x = position.x,
                    y = position.y,
                    scale = RoomGeometry.clampScale(it.scale),
                )
            }
            .toImmutableList(),
    )
}

/** 저장할 놓인 아이템 전체. 배경이 맨 뒤(zIndex 0)이고 인테리어는 1부터 그리는 순서대로다. 좌표는 저장 규칙으로 바꿔 보낸다. */
internal fun RoomDraft.toPlacements(): List<RoomPlacementVO> = buildList {
    background?.let {
        add(RoomPlacementVO(roomItemId = it.roomItemId, category = RoomItemCategory.BACKGROUND, imageUrl = it.imageUrl))
    }
    items.forEachIndexed { index, item ->
        val position = RoomGeometry.toSavedPosition(item.x, item.y)
        add(
            RoomPlacementVO(
                roomItemId = item.roomItemId,
                imageUrl = item.imageUrl,
                x = position.x,
                y = position.y,
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
