package com.dandi.nyummy.room.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.LocalRoomCardSize
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.component.RoomFrame
import com.dandi.nyummy.room.presentation.component.RoomInventorySheet
import com.dandi.nyummy.room.presentation.component.RoomStage
import com.dandi.nyummy.room.presentation.component.rememberRoomSheetState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 마이룸(방 꾸미기). 방은 홈 고양이방 카드와 같은 크기, 같은 잘림으로 보여 줘서 꾸민 모습이 홈과 같다.
 * 아래 보관함 시트는 끌어 올리고 내린다. 올리면 방 아래쪽을 덮고, 내리면 방이 다 보인다.
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
    val sheetState = rememberRoomSheetState()
    val homeCardSize = LocalRoomCardSize.current.size
    // 시스템 바 인셋은 앱 Scaffold가 이미 밀어 두었다. 시트가 내비게이션 바 여백을 한 번 더 넣지 않게 여기서 쓴 것으로 둔다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .consumeWindowInsets(WindowInsets.systemBars),
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            val gutter = NyummyTheme.spacing.gutter
            val collapsedSheet = with(density) { sheetState.collapsedHeightPx.toDp() }
                .takeIf { it > 0.dp } ?: DefaultCollapsedSheetHeight
            val spaceWidth = minOf(maxWidth, ContentMaxWidth) - gutter * 2
            val spaceHeight = maxHeight - CardTopGap - CardBottomGap - collapsedSheet
            // 방은 홈에서 넘겨받은 고양이방 카드 크기 그대로 보여 준다(잘리는 모양까지 홈과 같게). 시트를 다 내렸을 때
            // 다 보이도록 넘치면 같은 비율로 줄인다. 홈을 거치지 않고 들어왔으면 이 화면 자리에 같은 규칙으로 카드를 정한다.
            val card = homeCardSize?.let { fitInside(it, spaceWidth, spaceHeight) }
                ?: RoomCardLayout.cardSize(spaceWidth, spaceHeight)
            val cardLeft = (maxWidth - card.width) / 2
            val cardBottom = CardTopGap + card.height
            val sheetTop = maxHeight - with(density) { sheetState.heightPx.toDp() }
            // 시트를 올리면 방 아래쪽을 덮는다. 덮인 곳에는 아이템을 끌어 놓지 않는다.
            val covered = (cardBottom - sheetTop).coerceIn(0.dp, card.height)
            val frame = remember(card, covered, density) { RoomFrame.of(card.width, card.height, covered, density) }
            RoomStage(
                frame = frame,
                background = uiState.room.background,
                items = uiState.room.items,
                selectedItemId = uiState.selectedItemId,
                onSelect = { onIntent(MyRoomIntent.SelectItem(it)) },
                onMove = { id, x, y, scale -> onIntent(MyRoomIntent.MoveItem(id, x, y, scale)) },
                onBringToFront = { onIntent(MyRoomIntent.BringSelectedToFront) },
                onPutAway = { onIntent(MyRoomIntent.PutSelectedAway) },
                modifier = Modifier
                    .offset(x = cardLeft, y = CardTopGap)
                    .size(card)
                    .clip(RoundedCornerShape(NyummyTheme.radius.l)),
            )
            // 빈 방에 처음 들어왔을 때(되돌릴 것도 놓인 것도 없을 때)는 되돌리기를 숨긴다.
            if (uiState.canUndo || uiState.room.items.isNotEmpty()) {
                val undoBottom = minOf(cardBottom, sheetTop) - NyummyTheme.spacing.s12
                NyummyIconButton(
                    icon = CommonR.drawable.nyummy_ic_undo_2,
                    contentDescription = stringResource(R.string.my_room_undo),
                    onClick = { onIntent(MyRoomIntent.Undo) },
                    style = NyummyIconButtonStyle.Filled,
                    enabled = uiState.canUndo,
                    modifier = Modifier.offset(
                        x = cardLeft + NyummyTheme.spacing.s12,
                        y = (undoBottom - NyummyTheme.size.touchTarget).coerceAtLeast(CardTopGap),
                    ),
                )
            }
            RoomInventorySheet(
                state = sheetState,
                loadState = uiState.loadState,
                tabs = uiState.inventoryTabs,
                selectedCategory = uiState.selectedCategory,
                onSelectTab = { onIntent(MyRoomIntent.SelectTab(it)) },
                onInteriorClick = { id ->
                    val point = frame.newItemPoint()
                    onIntent(MyRoomIntent.ClickInteriorItem(id, point.x, point.y))
                },
                onBackgroundClick = { onIntent(MyRoomIntent.ClickBackground(it)) },
                onReachEnd = { onIntent(MyRoomIntent.ReachInventoryEnd(it)) },
                onShopClick = { onIntent(MyRoomIntent.ClickShop) },
                onRetry = { onIntent(MyRoomIntent.RetryLoad) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** [size]를 그대로 쓰되, 자리보다 크면 가로세로 비율을 지키며 줄인다(잘리는 모양이 홈과 같도록). */
private fun fitInside(size: DpSize, maxWidth: Dp, maxHeight: Dp): DpSize {
    val scale = minOf(1f, maxWidth / size.width, maxHeight / size.height).coerceAtLeast(0f)
    return DpSize(size.width * scale, size.height * scale)
}

/** 홈 콘텐츠 최대 폭. 홈을 거치지 않고 들어왔을 때 카드 폭도 홈과 같게 이 안에서 정한다. */
private val ContentMaxWidth = 480.dp
private val CardTopGap = 12.dp
private val CardBottomGap = 12.dp

/** 시트를 재기 전 다 접은 높이 추정값(손잡이와 탭). */
private val DefaultCollapsedSheetHeight = 96.dp

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

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun MyRoomEmptyPreview() {
    NyummyTheme {
        MyRoomScreen(uiState = MyRoomUIState(loadState = MyRoomLoadState.LOADED), onIntent = {})
    }
}
