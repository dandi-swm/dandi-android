package com.dandi.nyummy.room.presentation.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.coachmark.coachMarkTarget
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.InventoryItemUiModel
import com.dandi.nyummy.room.presentation.InventoryTabUiModel
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.R
import com.dandi.nyummy.room.presentation.RoomItemCoachMarkKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 보관함 시트 펼침 상태. [expansion]이 1이면 칸까지 다 보이고, 0이면 손잡이와 탭만, -1이면 탭까지 내려가 손잡이만
 * 보인다(숨김, 방을 다 보며 아이템을 옮길 때). 손잡이와 탭을 위아래로 끌어 바꾸고, 손을 떼면 가까운 쪽(빠르게 튕기면
 * 튕긴 쪽 다음 단계)으로 붙는다.
 */
@Stable
internal class RoomSheetState(expanded: Boolean) {

    var expansion by mutableFloatStateOf(if (expanded) 1f else 0f)
        private set

    /** 펼쳤을 때만 보이는 칸 영역의 높이(px). */
    var bodyHeightPx by mutableIntStateOf(0)
        internal set

    /** 숨기면 내려가는 탭 영역의 높이(px). */
    var tabsHeightPx by mutableIntStateOf(0)
        internal set

    /** 늘 보이는 손잡이와 아래 시스템 바 여백의 높이(px). 재기 전이면 0이다. */
    private var baseHeightPx by mutableIntStateOf(0)

    /** 다 접었을 때(손잡이와 탭)의 시트 높이(px). 펼친 정도와 상관없이 같다. 재기 전이면 0이다. */
    val collapsedHeightPx: Int
        get() = if (baseHeightPx == 0) 0 else baseHeightPx + tabsHeightPx

    /**
     * 지금 펼친 정도에서의 시트 전체 높이(px). 재기 전이면 0이다.
     * 잰 높이는 한 프레임 늦게 들어와서 끄는 동안 쓰면 방이 흔들리므로, 펼친 정도로 바로 계산한다.
     */
    val heightPx: Int
        get() = if (baseHeightPx == 0) 0 else coveredHeightPx + liftPx

    /**
     * 방 아래쪽을 덮는 높이(px): 손잡이와 지금 보이는 탭. 칸 영역이 보이는 만큼은 방이 같이 올라가므로([liftPx]) 덮지 않는다.
     * 이 높이 위까지만 아이템을 끌어 놓는다. 숨기면 손잡이만 남아 더 아래까지 놓을 수 있다.
     */
    val coveredHeightPx: Int
        get() = if (baseHeightPx == 0) 0 else baseHeightPx + visibleTabsHeightPx(tabsHeightPx)

    /** 펼친 칸 영역만큼 방을 올린다. 접거나 숨기면 0이다. */
    val liftPx: Int
        get() = visibleBodyHeightPx(bodyHeightPx)

    val isExpanded: Boolean
        get() = expansion >= 0.5f

    /** 칸 영역 [bodyHeight] 중 지금 보이는 높이(px). 시트를 그릴 때도 같은 값을 쓴다. */
    internal fun visibleBodyHeightPx(bodyHeight: Int): Int = (bodyHeight * expansion.coerceAtLeast(0f)).roundToInt()

    /** 탭 영역 [tabsHeight] 중 지금 보이는 높이(px). 숨기는 쪽(-1~0)에서만 줄어든다. */
    internal fun visibleTabsHeightPx(tabsHeight: Int): Int = (tabsHeight * (1f + expansion.coerceAtMost(0f))).roundToInt()

    /** 시트를 잰 뒤 부른다. 같은 측정에서 보이던 칸·탭 영역을 빼서 늘 보이는 높이를 얻는다. */
    internal fun onMeasured(heightPx: Int) {
        baseHeightPx = heightPx - visibleBodyHeightPx(bodyHeightPx) - visibleTabsHeightPx(tabsHeightPx)
    }

    /** 손가락을 따라간다. 펼치는 쪽(0~1)은 칸 영역 높이, 숨기는 쪽(-1~0)은 탭 높이만큼 끌면 한 단계다. */
    fun dragBy(deltaPx: Float) {
        val offset = if (expansion >= 0f) expansion * bodyHeightPx else expansion * tabsHeightPx
        val next = offset - deltaPx
        expansion = when {
            next >= 0f -> if (bodyHeightPx > 0) next / bodyHeightPx else 0f
            tabsHeightPx > 0 -> next / tabsHeightPx
            else -> 0f
        }.coerceIn(HIDDEN, EXPANDED)
    }

    /** 손을 뗀 뒤 붙는다. 위로 빠르게 튕기면 한 단계 위, 아래로 튕기면 한 단계 아래, 아니면 가까운 단계다. */
    suspend fun settle(velocity: Float) {
        val target = when {
            velocity < -FlingVelocity -> Stops.firstOrNull { it > expansion } ?: EXPANDED
            velocity > FlingVelocity -> Stops.lastOrNull { it < expansion } ?: HIDDEN
            else -> Stops.minBy { kotlin.math.abs(it - expansion) }
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

        const val HIDDEN = -1f
        const val COLLAPSED = 0f
        const val EXPANDED = 1f
        val Stops = listOf(HIDDEN, COLLAPSED, EXPANDED)
    }
}

@Composable
internal fun rememberRoomSheetState(expanded: Boolean = true): RoomSheetState = remember { RoomSheetState(expanded) }

/**
 * 마이룸 아래 보관함 시트. 손잡이와 탭을 끌어 올리고 내리며, 내리면 손잡이와 탭만 남고, 끝까지 내리면 탭도 내려가
 * 손잡이만 남는다(방 아래쪽까지 보며 아이템을 옮길 때). 손잡이를 누르거나 접힌 채 탭을 눌러도 펼친다.
 * 탭을 바꿔도 칸 영역 높이가 같아 시트가 흔들리지 않는다.
 *
 * - 탭은 서버가 주는 아이템 종류마다 하나(예 배경, 침대, 기타)이고, 칸은 가로 한 줄로 넘긴다. 끝에 가까워지면 그 종류만 더 받는다.
 * - 칸은 그림과 이름. 방에 놓인 인테리어는 그림을 흐리게 하고 "배치됨", 깔린 배경은 테두리와 "사용 중"을 붙인다.
 * - 가진 게 없으면 안내와 상점 구경하기, 있으면 줄 아래에 상점에서 더 찾기.
 *
 * @param tutorialItemId 튜토리얼이 가리킬 칸. 그 칸을 코치마크 대상으로 등록한다.
 */
@Composable
internal fun RoomInventorySheet(
    state: RoomSheetState,
    loadState: MyRoomLoadState,
    tabs: ImmutableList<InventoryTabUiModel>,
    selectedCategory: RoomItemCategory,
    tutorialItemId: Long?,
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
            RoomSheetHandle(state = state)
            // 탭 영역: 숨기는 만큼 아래부터 잘려 시트와 함께 내려간다. 크기는 그대로 잰다.
            Box(modifier = Modifier.roomSheetReveal(state::visibleTabsHeightPx)) {
                NyummyTabs(
                    tabs = tabs.map { tab -> tab.name.ifBlank { tab.category.fallbackLabel() } }.toImmutableList(),
                    selectedIndex = tabs.indexOfFirst { it.category == selectedCategory }.coerceAtLeast(0),
                    onSelect = { index ->
                        tabs.getOrNull(index)?.let { onSelectTab(it.category) }
                        if (!state.isExpanded) scope.launch { state.animateTo(1f) }
                    },
                    modifier = Modifier
                        .onSizeChanged { state.tabsHeightPx = it.height }
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = RoomSheetContentMaxWidth)
                        .padding(horizontal = NyummyTheme.spacing.gutter),
                )
            }
        }
        // 칸 영역: 펼친 만큼만 보인다. 접혀도 크기는 그대로 재고 위에서부터 잘라 보여 준다.
        Box(modifier = Modifier.roomSheetReveal(state::visibleBodyHeightPx)) {
            val items = tabs.firstOrNull { it.category == selectedCategory }?.items ?: persistentListOf()
            // 칸 줄은 화면 끝까지 넘어가게 좌우 여백 없이 두고, 줄 안쪽 여백으로 첫 칸을 맞춘다.
            Box(
                modifier = Modifier
                    .onSizeChanged { state.bodyHeightPx = it.height }
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = RoomSheetContentMaxWidth)
                    .padding(top = NyummyTheme.spacing.s12, bottom = RoomSheetBottomPadding)
                    .height(ContentHeight),
            ) {
                when {
                    loadState == MyRoomLoadState.LOADING -> NyummySpinner(
                        contentDescription = stringResource(R.string.my_room_loading),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    loadState == MyRoomLoadState.FAILED -> RoomSheetMessage(
                        message = stringResource(R.string.my_room_load_failed),
                        actionText = stringResource(R.string.my_room_retry),
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    items.isEmpty() -> RoomSheetMessage(
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
                                tutorialItemId = tutorialItemId,
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
    tutorialItemId: Long?,
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
        horizontalArrangement = Arrangement.spacedBy(RoomCellGap),
    ) {
        items(items, key = { it.roomItemId }) { item ->
            val onCellClick = { onClick(item.roomItemId) }
            RoomItemCell(
                name = item.name,
                imageUrl = item.imageUrl,
                isBackground = isBackground,
                isPlaced = item.isPlaced,
                onClick = onCellClick,
                modifier = if (item.roomItemId == tutorialItemId) Modifier.coachMarkTarget(RoomItemCoachMarkKey, onCellClick) else Modifier,
            )
        }
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

/** 보관함 칸 영역 높이. 칸 한 줄과 상점 버튼이 들어가고, 탭을 바꾸거나 비어 있어도 같다. */
private val ContentHeight = 150.dp
