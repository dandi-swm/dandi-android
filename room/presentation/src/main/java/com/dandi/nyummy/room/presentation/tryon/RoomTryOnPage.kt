package com.dandi.nyummy.room.presentation.tryon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.entity.RoomTryOnItemVO
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.R
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomDraft
import com.dandi.nyummy.room.presentation.RoomPlacementUiModel
import com.dandi.nyummy.room.presentation.component.RoomFrame
import com.dandi.nyummy.room.presentation.component.RoomStage
import com.dandi.nyummy.room.presentation.component.rememberRoomSheetState
import com.dandi.nyummy.room.presentation.roomPill
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 놓아보기(사기 전에 내 방에 놓아 보기). 띄운 화면(장바구니) 위를 전체 화면으로 덮는다. 라우트가 아니다.
 *
 * 방은 마이룸과 같은 자리에 같은 규칙으로 그리고, 마이룸 배경만 가져온다(가진 인테리어는 보이지 않는다). 아래 "담은 아이템" 시트에서 칸을 눌러
 * 놓아 보고, 마이룸처럼 끌어 옮기고 크기를 바꾸고 되돌린다. 저장 버튼은 없고, 뒤로 가기는 묻지 않고 [onClose]를 부른다.
 *
 * 상태는 이 화면을 띄운 엔트리에 묶인 ViewModel이 들고 있어서, 닫았다 다시 열면 놓아 둔 모습이 그대로다.
 *
 * @param items 담은 아이템 칸(수량만큼). 바뀌면 없어진 칸의 아이템은 방에서도 뺀다.
 * @param footer 시트 맨 아래에 그릴 것(장바구니의 결제 바).
 * @param onClose 닫기. 뒤로 가기(화면 위 버튼, 시스템 뒤로 가기)가 부른다.
 */
@Composable
fun RoomTryOnPage(
    items: ImmutableList<RoomTryOnItemVO>,
    footer: @Composable () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoomTryOnViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(items) { viewModel.onIntent(RoomTryOnIntent.ItemsChanged(items)) }
    BackHandler(onBack = onClose)
    RoomTryOnScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onClose = onClose,
        footer = footer,
        modifier = modifier,
    )
}

@Composable
internal fun RoomTryOnScreen(
    uiState: RoomTryOnUIState,
    onIntent: (RoomTryOnIntent) -> Unit,
    onClose: () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberRoomSheetState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            // 아래 화면(장바구니)으로 손가락이 새지 않게 이 화면이 받는다.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        val density = LocalDensity.current
        // 마이룸과 같은 자리(화면 전체)에 같은 규칙으로 방을 둔다. 시트를 펼친 만큼 방도 같이 올라간다.
        val stage = RoomCardLayout.cardSize(maxWidth, maxHeight)
        val stageLeft = (maxWidth - stage.width) / 2
        val stageTop = maxHeight - stage.height
        val sheetTop = maxHeight - with(density) { sheetState.heightPx.toDp() }
        val covered = with(density) { sheetState.coveredHeightPx.toDp() }.coerceIn(0.dp, stage.height)
        val frame = remember(stage, covered, density) { RoomFrame.of(stage.width, stage.height, covered, density) }
        RoomStage(
            frame = frame,
            background = uiState.room.background,
            items = uiState.room.items,
            selectedItemId = uiState.selectedItemId,
            cat = uiState.catMotion,
            onCatMotionFinished = { onIntent(RoomTryOnIntent.CatMotionFinished(it)) },
            onSelect = { onIntent(RoomTryOnIntent.SelectItem(it)) },
            onMove = { id, x, y, scale -> onIntent(RoomTryOnIntent.MoveItem(id, x, y, scale)) },
            onBringToFront = { onIntent(RoomTryOnIntent.BringSelectedToFront) },
            onPutAway = { onIntent(RoomTryOnIntent.PutSelectedAway) },
            putAwayLabel = R.string.room_try_on_put_away,
            modifier = Modifier
                // 끄는 동안 매 프레임 바뀌는 값이라 배치 단계에서만 읽는다.
                .offset {
                    val lift = sheetState.liftPx
                    IntOffset(stageLeft.roundToPx(), stageTop.roundToPx() - lift)
                }
                .size(stage),
        )
        // 뒤로 가기와 제목은 방 위에 뜬 흰 원·알약이다. 저장할 것이 없어 저장 버튼은 없다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
        ) {
            NyummyIconButton(
                icon = CommonR.drawable.nyummy_ic_chevron_left,
                contentDescription = stringResource(CommonR.string.nyummy_back_description),
                onClick = onClose,
                style = NyummyIconButtonStyle.Filled,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            NyummyText(
                text = stringResource(R.string.room_try_on_title),
                style = NyummyTheme.typography.titleS,
                modifier = Modifier
                    .align(Alignment.Center)
                    .roomPill()
                    .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
            )
        }
        // 마이룸과 같이, 되돌릴 것도 놓인 것도 없을 때(빈 방에 처음 들어왔을 때)는 되돌리기를 숨긴다.
        if (uiState.canUndo || uiState.room.items.isNotEmpty()) {
            NyummyIconButton(
                icon = CommonR.drawable.nyummy_ic_undo_2,
                contentDescription = stringResource(R.string.my_room_undo),
                onClick = { onIntent(RoomTryOnIntent.Undo) },
                style = NyummyIconButtonStyle.Filled,
                enabled = uiState.canUndo,
                modifier = Modifier.offset(
                    x = NyummyTheme.spacing.gutter,
                    y = sheetTop - NyummyTheme.spacing.s12 - NyummyTheme.size.touchTarget,
                ),
            )
        }
        RoomTryOnSheet(
            state = sheetState,
            loadState = uiState.loadState,
            cells = uiState.cells,
            onInteriorClick = { id ->
                val point = frame.newItemPoint()
                onIntent(RoomTryOnIntent.ClickInteriorCell(id, point.x, point.y))
            },
            onBackgroundClick = { onIntent(RoomTryOnIntent.ClickBackgroundCell(it)) },
            onRetry = { onIntent(RoomTryOnIntent.RetryLoad) },
            footer = footer,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private val PreviewCells = persistentListOf(
    RoomTryOnCellUiModel(key = "9:0", id = -4, name = "민트 체크 방", imageUrl = "", isBackground = true, price = 1_000, isPurchasable = true),
    RoomTryOnCellUiModel(key = "7:0", id = -1, name = "발바닥 방석", imageUrl = "", isBackground = false, price = 150, isPurchasable = true),
    RoomTryOnCellUiModel(key = "7:1", id = -2, name = "발바닥 방석", imageUrl = "", isBackground = false, price = 150, isPurchasable = true),
    RoomTryOnCellUiModel(key = "8:0", id = -3, name = "플로어 조명", imageUrl = "", isBackground = false, price = 320, isPurchasable = true),
    RoomTryOnCellUiModel(key = "5:0", id = -5, name = "단풍 러그", imageUrl = "", isBackground = false, price = 400, isPurchasable = false),
)

private val PreviewOwnedRoom = RoomDraft(background = RoomBackgroundUiModel(roomItemId = 100, imageUrl = ""))

@Composable
private fun PreviewFooter() {
    NyummyText(
        text = "결제 바(장바구니가 넘긴다)",
        style = NyummyTheme.typography.bodyS,
        modifier = Modifier.padding(NyummyTheme.spacing.gutter),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomTryOnEnteredPreview() {
    NyummyTheme {
        RoomTryOnScreen(
            uiState = RoomTryOnUIState(loadState = MyRoomLoadState.LOADED, room = PreviewOwnedRoom, cells = PreviewCells),
            onIntent = {},
            onClose = {},
            footer = { PreviewFooter() },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomTryOnPlacedPreview() {
    val room = PreviewOwnedRoom.copy(
        background = RoomBackgroundUiModel(roomItemId = -4, imageUrl = ""),
        items = persistentListOf(
            *PreviewOwnedRoom.items.toTypedArray(),
            RoomPlacementUiModel(roomItemId = -1, imageUrl = "", x = 0.7f, y = 0.9f, scale = 1f),
            RoomPlacementUiModel(roomItemId = -3, imageUrl = "", x = 0.2f, y = 0.6f, scale = 1.2f),
        ),
    )
    NyummyTheme {
        RoomTryOnScreen(
            uiState = RoomTryOnUIState(
                loadState = MyRoomLoadState.LOADED,
                room = room,
                originalBackground = PreviewOwnedRoom.background,
                undoStack = persistentListOf(PreviewOwnedRoom),
                selectedItemId = -3,
                cells = persistentListOf(
                    PreviewCells[0].copy(isPlaced = true),
                    PreviewCells[1].copy(isPlaced = true),
                    PreviewCells[2],
                    PreviewCells[3].copy(isPlaced = true),
                    PreviewCells[4],
                ),
            ),
            onIntent = {},
            onClose = {},
            footer = { PreviewFooter() },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomTryOnUnavailablePreview() {
    NyummyTheme {
        RoomTryOnScreen(
            uiState = RoomTryOnUIState(
                loadState = MyRoomLoadState.LOADED,
                room = PreviewOwnedRoom,
                // 살 수 없는 칸이 먼저 보이게 줄의 뒤쪽만 둔다.
                cells = persistentListOf(PreviewCells[4], PreviewCells[3]),
            ),
            onIntent = {},
            onClose = {},
            footer = { PreviewFooter() },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomTryOnFailedPreview() {
    NyummyTheme {
        RoomTryOnScreen(
            uiState = RoomTryOnUIState(loadState = MyRoomLoadState.FAILED, cells = PreviewCells),
            onIntent = {},
            onClose = {},
            footer = { PreviewFooter() },
        )
    }
}
