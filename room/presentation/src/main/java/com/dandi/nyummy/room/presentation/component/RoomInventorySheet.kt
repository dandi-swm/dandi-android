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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.InventoryItemUiModel
import com.dandi.nyummy.room.presentation.InventoryTabUiModel
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.R
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
 * - 탭은 서버가 주는 아이템 종류마다 하나(예 배경, 침대, 기타)이고, 칸은 가로 한 줄로 넘긴다. 끝에 가까워지면 그 종류만 더 받는다.
 * - 칸은 그림과 이름. 방에 놓인 인테리어는 그림을 흐리게 하고 "배치됨", 깔린 배경은 테두리와 "사용 중"을 붙인다.
 * - 가진 게 없으면 안내와 상점 구경하기, 있으면 줄 아래에 상점에서 더 찾기.
 */
@Composable
internal fun RoomInventorySheet(
    state: RoomSheetState,
    loadState: MyRoomLoadState,
    tabs: ImmutableList<InventoryTabUiModel>,
    selectedCategory: RoomItemCategory,
    onSelectTab: (RoomItemCategory) -> Unit,
    onInteriorClick: (Long) -> Unit,
    onBackgroundClick: (Long) -> Unit,
    onReachEnd: (RoomItemCategory) -> Unit,
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
                tabs = tabs.map { tab -> tab.name.ifBlank { tab.category.fallbackLabel() } }.toImmutableList(),
                selectedIndex = tabs.indexOfFirst { it.category == selectedCategory }.coerceAtLeast(0),
                onSelect = { index ->
                    tabs.getOrNull(index)?.let { onSelectTab(it.category) }
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
            val items = tabs.firstOrNull { it.category == selectedCategory }?.items ?: persistentListOf()
            // 칸 줄은 화면 끝까지 넘어가게 좌우 여백 없이 두고, 줄 안쪽 여백으로 첫 칸을 맞춘다.
            Box(
                modifier = Modifier
                    .onSizeChanged { state.bodyHeightPx = it.height }
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = ContentMaxWidth)
                    .padding(top = NyummyTheme.spacing.s12, bottom = SheetBottomPadding)
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
                        val isBackground = selectedCategory == RoomItemCategory.BACKGROUND
                        // 탭마다 따로 넘긴 자리를 갖는다(탭을 바꾸면 처음부터 보인다).
                        key(selectedCategory) {
                            InventoryRow(
                                items = items,
                                isBackground = isBackground,
                                onClick = if (isBackground) onBackgroundClick else onInteriorClick,
                                onReachEnd = { onReachEnd(selectedCategory) },
                            )
                        }
                        Spacer(Modifier.height(NyummyTheme.spacing.s12))
                        NyummyTextButton(
                            text = stringResource(R.string.my_room_more_in_shop),
                            onClick = onShopClick,
                            tone = NyummyTextButtonTone.Neutral,
                            size = NyummyTextButtonSize.S,
                            trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
                            modifier = Modifier.padding(start = NyummyTheme.spacing.gutter),
                        )
                    }
                }
            }
        }
    }
}

/** 한 종류의 칸을 가로 한 줄로. 끝에 가까워지면 [onReachEnd]로 다음 묶음을 부른다. */
@Composable
private fun InventoryRow(
    items: ImmutableList<InventoryItemUiModel>,
    isBackground: Boolean,
    onClick: (Long) -> Unit,
    onReachEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    LoadMoreEffect(listState = listState, itemCount = items.size, onReachEnd = onReachEnd)
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = NyummyTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        items(items, key = { it.roomItemId }) { item ->
            InventoryCell(item = item, isBackground = isBackground, onClick = { onClick(item.roomItemId) })
        }
    }
}

/**
 * 보관함 칸. 위에 그림, 아래에 이름. 모든 종류가 같은 크기라 탭을 바꿔도 줄 높이가 같다.
 * - 인테리어: 그림을 가운데에 맞춰 보여 주고, 방에 놓였으면 흐리게 하고 "배치됨"을 붙인다.
 * - 배경: 방 그림을 칸에 꽉 채워 바닥 기준으로 보여 주고, 깔렸으면 브랜드색 테두리와 "사용 중", 이름도 브랜드색으로 굵게.
 */
@Composable
private fun InventoryCell(
    item: InventoryItemUiModel,
    isBackground: Boolean,
    onClick: () -> Unit,
) {
    val stateText = stringResource(if (isBackground) R.string.my_room_background_in_use else R.string.my_room_item_placed)
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    val isInUseBackground = isBackground && item.isPlaced
    Column(
        modifier = Modifier
            .width(CellSize)
            .nyummyClickable(onClick = onClick)
            // 이름과 "배치됨"/"사용 중" 글자를 한 칸으로 묶어 읽는다.
            .semantics(mergeDescendants = true) { selected = item.isPlaced },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CellLabelGap),
    ) {
        Box(
            modifier = Modifier
                .size(CellSize)
                .clip(shape)
                .background(NyummyTheme.colors.bg.surfaceSunken, shape)
                .then(
                    if (isInUseBackground) {
                        Modifier.border(BackgroundSelectedBorder, NyummyTheme.colors.bg.actionPrimary, shape)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (isBackground) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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
            }
            if (item.isPlaced) {
                StateChip(
                    text = stateText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = CellChipBottom),
                )
            }
        }
        NyummyText(
            text = item.name,
            style = NyummyTheme.typography.labelM,
            color = if (isInUseBackground) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.secondary,
            maxLines = 1,
        )
    }
}

/** 탭 이름. 서버가 종류 이름을 주지 않았을 때만 쓴다. 앱이 이름을 모르는 종류는 종류 값을 그대로 보인다. */
@Composable
private fun RoomItemCategory.fallbackLabel(): String = when (key) {
    RoomItemCategory.BACKGROUND.key -> stringResource(R.string.my_room_category_background)
    BED_KEY -> stringResource(R.string.my_room_category_bed)
    RoomItemCategory.ETC.key -> stringResource(R.string.my_room_category_etc)
    else -> key
}

/** 칸 위 상태 표시(배치됨, 사용 중). 진한 바탕에 흰 글자. */
@Composable
private fun StateChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(horizontal = NyummyTheme.spacing.s8, vertical = StateChipVerticalPadding),
    ) {
        NyummyText(
            text = text,
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
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
    listState: LazyListState,
    itemCount: Int,
    onReachEnd: () -> Unit,
) {
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnReachEnd by rememberUpdatedState(onReachEnd)
    LaunchedEffect(listState) {
        snapshotFlow {
            val count = currentItemCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            count to lastVisible.coerceAtMost(count - 1)
        }
            .distinctUntilChanged()
            .filter { (count, lastVisible) -> count > 0 && lastVisible >= count - 1 - LOAD_MORE_THRESHOLD }
            .collect { currentOnReachEnd() }
    }
}

private const val LOAD_MORE_THRESHOLD = 4

/** 침대 종류 값. 서버가 이름을 주지 않았을 때 앱의 이름을 붙이려고만 쓴다. */
private const val BED_KEY = "BED"

/** 방에 놓인 아이템 칸의 그림 불투명도. */
private const val PlacedImageAlpha = 0.35f

private val ContentMaxWidth = 480.dp

/** 보관함 칸 영역 높이. 칸 한 줄과 상점 버튼이 들어가고, 탭을 바꾸거나 비어 있어도 같다. */
private val ContentHeight = 150.dp
/** 손잡이 영역 높이(위 여백 10 + 손잡이 4 + 탭까지 12). 누르면 시트를 펴고 접는다. */
private val HandleTouchHeight = 26.dp
private val SheetBottomPadding = 28.dp
private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private val CellSize = 80.dp
private val CellGap = 10.dp
private val CellLabelGap = 6.dp
private val CellChipBottom = 6.dp
private val InteriorImageSize = 56.dp
private val BackgroundSelectedBorder = 3.dp
private val StateChipVerticalPadding = 3.dp
