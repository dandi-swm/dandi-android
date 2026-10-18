package com.dandi.nyummy.room.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.component.RoomInventorySheet
import com.dandi.nyummy.room.presentation.component.RoomStage
import kotlinx.collections.immutable.persistentListOf
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 마이룸(방 꾸미기). 위는 방 무대, 아래는 보관함 시트다. 시트는 무대 아래쪽에 살짝 겹쳐 올라온다.
 *
 * - 보관함 칸을 누르면 방 가운데에 놓고, 방에서 끌어 옮기고 두 손가락으로 크기를 바꾼다.
 * - 왼쪽 아래 버튼으로 한 단계씩 되돌린다.
 * - 저장하면 홈으로 돌아간다. 저장하지 않고 나가려 하면 저장할지 묻는다.
 */
@Composable
fun MyRoomPage(
    modifier: Modifier = Modifier,
    viewModel: MyRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.hasChanges) { viewModel.onIntent(MyRoomIntent.ClickBack) }

    MyRoomScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun MyRoomScreen(
    uiState: MyRoomUIState,
    onIntent: (MyRoomIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.my_room_title),
            onBackClick = { onIntent(MyRoomIntent.ClickBack) },
            trailing = {
                NyummyTextButton(
                    text = stringResource(R.string.my_room_save),
                    onClick = { onIntent(MyRoomIntent.ClickSave) },
                    enabled = uiState.canSave,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            RoomStage(
                background = uiState.room.background,
                items = uiState.room.items,
                selectedItemId = uiState.selectedItemId,
                bottomInset = SheetOverlap,
                onSelect = { onIntent(MyRoomIntent.SelectItem(it)) },
                onMove = { id, x, y, scale -> onIntent(MyRoomIntent.MoveItem(id, x, y, scale)) },
                onBringToFront = { onIntent(MyRoomIntent.BringSelectedToFront) },
                onPutAway = { onIntent(MyRoomIntent.PutSelectedAway) },
                modifier = Modifier.fillMaxSize(),
            )
            // 빈 방에 처음 들어왔을 때(되돌릴 것도 놓인 것도 없을 때)는 되돌리기를 숨긴다.
            if (uiState.canUndo || uiState.room.items.isNotEmpty()) {
                NyummyIconButton(
                    icon = CommonR.drawable.nyummy_ic_undo_2,
                    contentDescription = stringResource(R.string.my_room_undo),
                    onClick = { onIntent(MyRoomIntent.Undo) },
                    style = NyummyIconButtonStyle.Filled,
                    enabled = uiState.canUndo,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = NyummyTheme.spacing.s16, bottom = SheetOverlap + NyummyTheme.spacing.s12),
                )
            }
        }
        RoomInventorySheet(
            loadState = uiState.loadState,
            selectedTab = uiState.selectedTab,
            interiorItems = uiState.interiorItems,
            backgrounds = uiState.backgrounds,
            onSelectTab = { onIntent(MyRoomIntent.SelectTab(it)) },
            onInteriorClick = { onIntent(MyRoomIntent.ClickInteriorItem(it)) },
            onBackgroundClick = { onIntent(MyRoomIntent.ClickBackground(it)) },
            onReachEnd = { onIntent(MyRoomIntent.ReachInventoryEnd) },
            onShopClick = { onIntent(MyRoomIntent.ClickShop) },
            onRetry = { onIntent(MyRoomIntent.RetryLoad) },
            modifier = Modifier.overlapAbove(SheetOverlap),
        )
    }
}

/**
 * 시트를 [overlap]만큼 위로 올려 바로 위 요소(방 무대)에 겹친다. 겹친 만큼 위 요소가 더 넓게 측정된다.
 */
private fun Modifier.overlapAbove(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlapPx = overlap.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - overlapPx) { placeable.place(0, -overlapPx) }
}

/** 보관함 시트가 방 무대 아래쪽에 겹치는 높이. */
private val SheetOverlap = 24.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun MyRoomPreview() {
    NyummyTheme {
        MyRoomScreen(
            uiState = MyRoomUIState(
                loadState = MyRoomLoadState.LOADED,
                room = RoomDraft(
                    items = persistentListOf(
                        RoomPlacementUiModel(roomItemId = 1, imageUrl = "", x = 0.3f, y = 0.85f, scale = 1f),
                        RoomPlacementUiModel(roomItemId = 2, imageUrl = "", x = 0.7f, y = 0.8f, scale = 1.2f),
                    ),
                ),
                selectedItemId = 2,
                interiorItems = persistentListOf(
                    InventoryItemUiModel(1, RoomItemCategory.BED, "구름 방석", "", isPlaced = true),
                    InventoryItemUiModel(2, RoomItemCategory.CAT_WHEEL, "캣타워", "", isPlaced = true),
                    InventoryItemUiModel(3, RoomItemCategory.ETC, "털실 공", "", isPlaced = false),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun MyRoomEmptyPreview() {
    NyummyTheme {
        MyRoomScreen(uiState = MyRoomUIState(loadState = MyRoomLoadState.LOADED), onIntent = {})
    }
}
