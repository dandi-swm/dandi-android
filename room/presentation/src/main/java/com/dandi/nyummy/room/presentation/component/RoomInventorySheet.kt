package com.dandi.nyummy.room.presentation.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.room.presentation.InventoryItemUiModel
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.MyRoomTab
import com.dandi.nyummy.room.presentation.R
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 보관함 시트 펼침 상태. [expansion]이 1이면 칸까지 다 보이고, 0이면 손잡이와 탭만 보인다.
 * 손잡이와 탭을 위아래로 끌어 바꾸고, 손을 떼면 가까운 쪽(빠르게 튕기면 튕긴 쪽)으로 붙는다.
 */
@Stable
internal class RoomSheetState(expanded: Boolean) {

    var expansion by mutableFloatStateOf(if (expanded) 1f else 0f)
        private set

    /** 펼쳤을 때만 보이는 칸 영역의 높이(px). */
    var bodyHeightPx by mutableIntStateOf(0)
        internal set

    /** 다 접었을 때의 시트 높이(px). 손잡이와 탭, 아래 시스템 바 여백이라 펼친 정도와 상관없이 같다. 재기 전이면 0이다. */
    var collapsedHeightPx by mutableIntStateOf(0)
        private set

    /**
     * 지금 펼친 정도에서의 시트 전체 높이(px). 방 무대가 시트에 가리는 높이를 계산하는 데 쓴다. 재기 전이면 0이다.
     * 잰 높이는 한 프레임 늦게 들어와서 끄는 동안 쓰면 방이 흔들리므로, 펼친 정도로 바로 계산한다.
     */
    val heightPx: Int
        get() = if (collapsedHeightPx == 0) 0 else collapsedHeightPx + visibleBodyHeightPx(bodyHeightPx)

    val isExpanded: Boolean
        get() = expansion >= 0.5f

    /** 칸 영역 [bodyHeight] 중 지금 보이는 높이(px). 시트를 그릴 때도 같은 값을 쓴다. */
    internal fun visibleBodyHeightPx(bodyHeight: Int): Int = (bodyHeight * expansion).roundToInt()

    /** 시트를 잰 뒤 부른다. 같은 측정에서 보이던 칸 영역을 빼서 다 접은 높이를 얻는다. */
    internal fun onMeasured(heightPx: Int) {
        collapsedHeightPx = heightPx - visibleBodyHeightPx(bodyHeightPx)
    }

    fun dragBy(deltaPx: Float) {
        if (bodyHeightPx > 0) expansion = (expansion - deltaPx / bodyHeightPx).coerceIn(0f, 1f)
    }

    /** 손을 뗀 뒤 붙는다. 위로 빠르게 튕기면 펼치고, 아래로 튕기면 접고, 아니면 가까운 쪽이다. */
    suspend fun settle(velocity: Float) {
        val target = when {
            velocity < -FlingVelocity -> 1f
            velocity > FlingVelocity -> 0f
            isExpanded -> 1f
            else -> 0f
        }
        animateTo(target)
    }

    suspend fun animateTo(target: Float) {
        animate(initialValue = expansion, targetValue = target, animationSpec = tween(SettleMillis)) { value, _ ->
            expansion = value
        }
    }

    private companion object {
        /** 이보다 빠르게 튕기면(px/s) 손을 뗀 자리와 상관없이 튕긴 쪽으로 붙는다. */
        const val FlingVelocity = 800f
        const val SettleMillis = 220
    }
}

@Composable
internal fun rememberRoomSheetState(expanded: Boolean = true): RoomSheetState = remember { RoomSheetState(expanded) }

/**
 * 마이룸 아래 보관함 시트. 손잡이와 탭을 끌어 올리고 내리며, 내리면 손잡이와 탭만 남아 방이 다 보인다.
 * 손잡이를 누르거나 접힌 채 탭을 눌러도 펼친다. 탭을 바꿔도 칸 영역 높이가 같아 시트가 흔들리지 않는다.
 *
 * - 인테리어: 가진 아이템을 한 개씩 칸으로. 방에 놓인 칸은 그림을 흐리게 하고 "배치됨"을 붙인다.
 * - 배경: 방 그림 미리보기와 이름. 깔린 배경은 테두리와 "사용 중"을 붙인다.
 * - 가진 게 없으면 안내와 상점 구경하기, 있으면 맨 아래에 상점에서 더 찾기.
 */
@Composable
internal fun RoomInventorySheet(
    state: RoomSheetState,
    loadState: MyRoomLoadState,
    selectedTab: MyRoomTab,
    interiorItems: ImmutableList<InventoryItemUiModel>,
    backgrounds: ImmutableList<InventoryItemUiModel>,
    onSelectTab: (MyRoomTab) -> Unit,
    onInteriorClick: (Long) -> Unit,
    onBackgroundClick: (Long) -> Unit,
    onReachEnd: () -> Unit,
    onShopClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(topStart = NyummyTheme.radius.l, topEnd = NyummyTheme.radius.l)
    val dragState = rememberDraggableState { state.dragBy(it) }
    val toggleDescription = stringResource(
        if (state.isExpanded) R.string.my_room_sheet_collapse else R.string.my_room_sheet_expand,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.onMeasured(it.height) }
            .nyummyShadow(shape, NyummyTheme.elevation.float)
            .background(NyummyTheme.colors.bg.surface, shape)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 손잡이와 탭: 끌어서 시트를 올리고 내린다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> state.settle(velocity) },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HandleTouchHeight)
                    .nyummyClickable(onClick = { scope.launch { state.animateTo(if (state.isExpanded) 0f else 1f) } })
                    .semantics { contentDescription = toggleDescription },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = HandleWidth, height = HandleHeight)
                        .background(NyummyTheme.colors.border.default, RoundedCornerShape(HandleHeight / 2)),
                )
            }
            NyummyTabs(
                tabs = persistentListOf(
                    stringResource(R.string.my_room_tab_interior),
                    stringResource(R.string.my_room_tab_background),
                ),
                selectedIndex = selectedTab.ordinal,
                onSelect = {
                    onSelectTab(MyRoomTab.entries[it])
                    if (!state.isExpanded) scope.launch { state.animateTo(1f) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = ContentMaxWidth)
                    .padding(horizontal = NyummyTheme.spacing.gutter),
            )
        }
        // 칸 영역: 펼친 만큼만 보인다. 접혀도 크기는 그대로 재고 위에서부터 잘라 보여 준다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                    val visibleHeight = state.visibleBodyHeightPx(placeable.height)
                    layout(placeable.width, visibleHeight) { placeable.place(0, 0) }
                },
        ) {
            val items = if (selectedTab == MyRoomTab.INTERIOR) interiorItems else backgrounds
            Box(
                modifier = Modifier
                    .onSizeChanged { state.bodyHeightPx = it.height }
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = ContentMaxWidth)
                    .padding(
                        start = NyummyTheme.spacing.gutter,
                        end = NyummyTheme.spacing.gutter,
                        top = NyummyTheme.spacing.s12,
                        bottom = SheetBottomPadding,
                    )
                    .height(ContentHeight),
            ) {
                when {
                    loadState == MyRoomLoadState.LOADING -> NyummySpinner(
                        contentDescription = stringResource(R.string.my_room_loading),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    loadState == MyRoomLoadState.FAILED -> InventoryMessage(
                        message = stringResource(R.string.my_room_load_failed),
                        actionText = stringResource(R.string.my_room_retry),
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    items.isEmpty() -> InventoryMessage(
                        message = stringResource(R.string.my_room_empty),
                        actionText = stringResource(R.string.my_room_go_shop),
                        onAction = onShopClick,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            MyRoomTab.INTERIOR -> InteriorGrid(
                                items = interiorItems,
                                onClick = onInteriorClick,
                                onReachEnd = onReachEnd,
                                modifier = Modifier.weight(1f),
                            )
                            MyRoomTab.BACKGROUND -> BackgroundGrid(
                                items = backgrounds,
                                onClick = onBackgroundClick,
                                onReachEnd = onReachEnd,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(NyummyTheme.spacing.s12))
                        NyummyTextButton(
                            text = stringResource(R.string.my_room_more_in_shop),
                            onClick = onShopClick,
                            tone = NyummyTextButtonTone.Neutral,
                            size = NyummyTextButtonSize.S,
                            trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteriorGrid(
    items: ImmutableList<InventoryItemUiModel>,
    onClick: (Long) -> Unit,
    onReachEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    JankScrollWatcher(gridState)
    LoadMoreEffect(gridState = gridState, itemCount = items.size, onReachEnd = onReachEnd)
    LazyVerticalGrid(
        columns = GridCells.FixedSize(InteriorCellSize),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InteriorCellGap, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(InteriorCellGap),
    ) {
        items(items, key = { it.roomItemId }) { item ->
            InteriorCell(item = item, onClick = { onClick(item.roomItemId) })
        }
    }
}

/** 인테리어 칸. 방에 놓였으면 그림을 흐리게 하고 아래에 "배치됨"을 붙인다. */
@Composable
private fun InteriorCell(
    item: InventoryItemUiModel,
    onClick: () -> Unit,
) {
    val placedText = stringResource(R.string.my_room_item_placed)
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Box(
        modifier = Modifier
            .size(InteriorCellSize)
            .clip(shape)
            .background(NyummyTheme.colors.bg.surfaceSunken, shape)
            .nyummyClickable(onClick = onClick)
            .semantics {
                contentDescription = if (item.isPlaced) "${item.name}, $placedText" else item.name
                selected = item.isPlaced
            },
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.Center)
                .size(InteriorImageSize)
                .alpha(if (item.isPlaced) PlacedImageAlpha else 1f),
        )
        if (item.isPlaced) {
            StateChip(
                text = placedText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = InteriorChipBottom),
            )
        }
    }
}

@Composable
private fun BackgroundGrid(
    items: ImmutableList<InventoryItemUiModel>,
    onClick: (Long) -> Unit,
    onReachEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    JankScrollWatcher(gridState)
    LoadMoreEffect(gridState = gridState, itemCount = items.size, onReachEnd = onReachEnd)
    LazyVerticalGrid(
        columns = GridCells.FixedSize(BackgroundThumbWidth),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BackgroundCellGap, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(BackgroundCellGap),
    ) {
        items(items, key = { it.roomItemId }) { item ->
            BackgroundCell(item = item, onClick = { onClick(item.roomItemId) })
        }
    }
}

/**
 * 배경 칸. 방 그림을 칸 폭에 맞춰 바닥 기준으로 보여 주고 아래에 이름을 둔다.
 * 깔린 배경은 브랜드색 테두리와 "사용 중", 이름도 브랜드색으로 굵게.
 */
@Composable
private fun BackgroundCell(
    item: InventoryItemUiModel,
    onClick: () -> Unit,
) {
    val inUseText = stringResource(R.string.my_room_background_in_use)
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Column(
        modifier = Modifier
            .width(BackgroundThumbWidth)
            .nyummyClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { selected = item.isPlaced },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BackgroundLabelGap),
    ) {
        Box(
            modifier = Modifier
                .size(width = BackgroundThumbWidth, height = BackgroundThumbHeight)
                .clip(shape)
                .background(NyummyTheme.colors.bg.surfaceSunken, shape)
                .then(
                    if (item.isPlaced) {
                        Modifier.border(BackgroundSelectedBorder, NyummyTheme.colors.bg.actionPrimary, shape)
                    } else {
                        Modifier
                    },
                ),
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.isPlaced) {
                StateChip(
                    text = inUseText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BackgroundChipBottom),
                )
            }
        }
        NyummyText(
            text = item.name,
            style = if (item.isPlaced) NyummyTheme.typography.labelMStrong else NyummyTheme.typography.labelM,
            color = if (item.isPlaced) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.secondary,
            maxLines = 1,
        )
    }
}

/** 칸 위 상태 표시(배치됨, 사용 중). 진한 바탕에 흰 글자. */
@Composable
private fun StateChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(NyummyTheme.colors.bg.surfaceInverse, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(horizontal = NyummyTheme.spacing.s8, vertical = StateChipVerticalPadding),
    ) {
        NyummyText(
            text = text,
            style = NyummyTheme.typography.labelSStrong,
            color = NyummyTheme.colors.content.onInverse,
            maxLines = 1,
        )
    }
}

/** 보관함이 비었거나 불러오지 못했을 때. 안내 한 줄과 글자 버튼. */
@Composable
private fun InventoryMessage(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
    ) {
        NyummyText(text = message, style = NyummyTheme.typography.titleS)
        NyummyTextButton(
            text = actionText,
            onClick = onAction,
            trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
        )
    }
}

/** 보관함 끝에서 [LOAD_MORE_THRESHOLD]칸 안쪽까지 보이면 다음 묶음을 부른다. */
@Composable
private fun LoadMoreEffect(
    gridState: LazyGridState,
    itemCount: Int,
    onReachEnd: () -> Unit,
) {
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnReachEnd by rememberUpdatedState(onReachEnd)
    LaunchedEffect(gridState) {
        snapshotFlow {
            val count = currentItemCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            count to lastVisible.coerceAtMost(count - 1)
        }
            .distinctUntilChanged()
            .filter { (count, lastVisible) -> count > 0 && lastVisible >= count - 1 - LOAD_MORE_THRESHOLD }
            .collect { currentOnReachEnd() }
    }
}

private const val LOAD_MORE_THRESHOLD = 4

/** 방에 놓인 아이템 칸의 그림 불투명도. */
private const val PlacedImageAlpha = 0.35f

private val ContentMaxWidth = 480.dp

/** 보관함 칸 영역 높이. 인테리어 두 줄이 들어가고, 탭을 바꿔도 같다. 남는 칸은 스크롤한다. */
private val ContentHeight = 206.dp
/** 손잡이 영역 높이(위 여백 10 + 손잡이 4 + 탭까지 12). 누르면 시트를 펴고 접는다. */
private val HandleTouchHeight = 26.dp
private val SheetBottomPadding = 28.dp
private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private val InteriorCellSize = 76.dp
private val InteriorCellGap = 10.dp
private val InteriorImageSize = 56.dp
private val InteriorChipBottom = 6.dp
private val BackgroundThumbWidth = 100.dp
private val BackgroundThumbHeight = 120.dp
private val BackgroundCellGap = 12.dp
private val BackgroundLabelGap = 6.dp
private val BackgroundSelectedBorder = 3.dp
private val BackgroundChipBottom = 9.dp
private val StateChipVerticalPadding = 3.dp
