package com.dandi.nyummy.room.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.coachmark.coachMarkTarget
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.component.RoomFrame
import com.dandi.nyummy.room.presentation.component.RoomInventorySheet
import com.dandi.nyummy.room.presentation.component.RoomStage
import com.dandi.nyummy.room.presentation.component.rememberRoomSheetState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 마이룸(방 꾸미기). 방은 홈처럼 화면 전체(시스템 바 뒤까지)를 채우고 같은 규칙으로 그려서, 꾸민 모습과 잘리는 곳이 홈과 같다.
 * 위에는 뒤로 가기와 저장이 방 위에 뜨고, 아래 보관함 시트는 방 위로 끌어 올리고 내린다. 시트를 펼친 만큼 방도 같이 올라가
 * 냐미와 바닥 아이템이 시트에 가리지 않는다.
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
    MyRoomTutorialCoachMark(tutorial = uiState.tutorial, onIntent = viewModel::onIntent)
}

@Composable
internal fun MyRoomScreen(
    uiState: MyRoomUIState,
    onIntent: (MyRoomIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberRoomSheetState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        val density = LocalDensity.current
        // 홈과 같은 자리(화면 전체)에 같은 규칙으로 방을 둔다. 너무 넓거나 긴 화면에서만 줄여 바닥 가운데에 붙인다.
        val stage = RoomCardLayout.cardSize(maxWidth, maxHeight)
        val stageLeft = (maxWidth - stage.width) / 2
        val stageTop = maxHeight - stage.height
        val sheetTop = maxHeight - with(density) { sheetState.heightPx.toDp() }
        // 시트를 펼치면 펼친 만큼 방도 같이 올라가서 냐미와 바닥 아이템이 시트에 가리지 않는다.
        // 그래서 방 아래쪽은 다 접은 시트만큼만 늘 덮이고, 덮인 곳에는 아이템을 끌어 놓지 않는다.
        val covered = with(density) { sheetState.collapsedHeightPx.toDp() }.coerceIn(0.dp, stage.height)
        val frame = remember(stage, covered, density) { RoomFrame.of(stage.width, stage.height, covered, density) }
        RoomStage(
            frame = frame,
            background = uiState.room.background,
            items = uiState.room.items,
            selectedItemId = uiState.selectedItemId,
            cat = uiState.catMotion,
            onCatMotionFinished = { onIntent(MyRoomIntent.CatMotionFinished(it)) },
            onSelect = { onIntent(MyRoomIntent.SelectItem(it)) },
            onMove = { id, x, y, scale -> onIntent(MyRoomIntent.MoveItem(id, x, y, scale)) },
            onBringToFront = { onIntent(MyRoomIntent.BringSelectedToFront) },
            onPutAway = { onIntent(MyRoomIntent.PutSelectedAway) },
            modifier = Modifier
                // 끄는 동안 매 프레임 바뀌는 값이라 배치 단계에서만 읽는다.
                .offset {
                    val lift = sheetState.heightPx - sheetState.collapsedHeightPx
                    IntOffset(stageLeft.roundToPx(), stageTop.roundToPx() - lift)
                }
                .size(stage),
        )
        // 뒤로 가기와 저장은 방 위에 뜬다.
        NyummyTopBar(
            title = stringResource(R.string.my_room_title),
            onBackClick = { onIntent(MyRoomIntent.ClickBack) },
            trailing = {
                NyummyTextButton(
                    text = stringResource(R.string.my_room_save),
                    onClick = { onIntent(MyRoomIntent.ClickSave) },
                    enabled = uiState.canSave,
                    modifier = Modifier.coachMarkTarget(RoomSaveCoachMarkKey) { onIntent(MyRoomIntent.ClickSave) },
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.statusBarsPadding(),
        )
        // 빈 방에 처음 들어왔을 때(되돌릴 것도 놓인 것도 없을 때)는 되돌리기를 숨긴다.
        if (uiState.canUndo || uiState.room.items.isNotEmpty()) {
            NyummyIconButton(
                icon = CommonR.drawable.nyummy_ic_undo_2,
                contentDescription = stringResource(R.string.my_room_undo),
                onClick = { onIntent(MyRoomIntent.Undo) },
                style = NyummyIconButtonStyle.Filled,
                enabled = uiState.canUndo,
                modifier = Modifier.offset(
                    x = NyummyTheme.spacing.gutter,
                    y = sheetTop - NyummyTheme.spacing.s12 - NyummyTheme.size.touchTarget,
                ),
            )
        }
        RoomInventorySheet(
            state = sheetState,
            loadState = uiState.loadState,
            tabs = uiState.inventoryTabs,
            selectedCategory = uiState.selectedCategory,
            tutorialItemId = uiState.tutorial?.itemId,
            onSelectTab = { onIntent(MyRoomIntent.SelectTab(it)) },
            onInteriorClick = { id ->
                val point = frame.newItemPoint()
                onIntent(MyRoomIntent.ClickInteriorItem(id, point.x, point.y))
            },
            onBackgroundClick = { onIntent(MyRoomIntent.ClickBackground(it)) },
            onReachEnd = { onIntent(MyRoomIntent.ReachInventoryEnd(it)) },
            onShopClick = { onIntent(MyRoomIntent.ClickShop) },
            onRetry = { onIntent(MyRoomIntent.RetryLoad) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .coachMarkTarget(RoomInventoryCoachMarkKey) {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
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
                selectedCategory = RoomItemCategory.BED,
                inventoryTabs = InventoryTabUiModel.emptyTabs().map { tab ->
                    if (tab.category != RoomItemCategory.BED) return@map tab
                    tab.copy(
                        items = persistentListOf(
                            InventoryItemUiModel(1, RoomItemCategory.BED, "구름 방석", "", isPlaced = true),
                            InventoryItemUiModel(2, RoomItemCategory.BED, "나무 침대", "", isPlaced = true),
                            InventoryItemUiModel(3, RoomItemCategory.BED, "바구니 침대", "", isPlaced = false),
                        ),
                    )
                }.toImmutableList(),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MyRoomEmptyPreview() {
    NyummyTheme {
        MyRoomScreen(
            uiState = MyRoomUIState(loadState = MyRoomLoadState.LOADED),
            onIntent = {},
        )
    }
}
