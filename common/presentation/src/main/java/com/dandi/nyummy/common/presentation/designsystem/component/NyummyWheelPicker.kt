package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * 휠 피커 한 개(값 1열). 높이 150, 행 50 × 3, 가운데 선택 밴드.
 * 날짜처럼 여러 열이 필요하면 [NyummyWheelPickerFrame] 안에 [NyummyWheelColumn]을 나란히 둔다.
 */
@Composable
fun NyummyWheelPicker(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    contentDescription: String? = null,
) {
    NyummyWheelPickerFrame(modifier = modifier) {
        NyummyWheelColumn(
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            unit = unit,
            contentDescription = contentDescription,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 휠 피커 틀. 흰 바탕, 2px 테두리, radius l, 가운데 선택 밴드를 그린다.
 * 시트 안처럼 이미 바탕이 있는 곳에서는 [framed]를 false로 두어 테두리 없이 밴드만 그린다.
 */
@Composable
fun NyummyWheelPickerFrame(
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.l)
    val frame = if (framed) {
        Modifier
            .clip(shape)
            .background(NyummyTheme.colors.bg.surface)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .height(NyummyComponentDimens.WheelPickerHeight)
            .then(frame),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NyummyComponentDimens.WheelPickerBandInset)
                .height(NyummyComponentDimens.WheelPickerBandHeight)
                .background(NyummyTheme.colors.bg.surfaceSubtle, RoundedCornerShape(NyummyTheme.radius.s)),
        )
        Row(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * 스냅 스크롤 휠 한 열. 가운데 행이 선택 값이고, 굴리는 중에도 가운데 값이 바뀔 때마다 [onSelectedIndexChange]로 알린다.
 * 멈출 때만 알리면 스냅이 끝나기 전에 확인·다음을 누른 경우 화면 가운데 값이 아니라 직전 값이 남는다.
 * 묶인 다른 휠(시 → 오전/오후)도 손을 떼기 전에 함께 넘어간다.
 * 가운데 값은 number/m + 단위(label/m), 위아래 값은 number/s tertiary.
 * 바깥에서 [selectedIndex]가 바뀌면 그 값으로 바로 옮긴다.
 *
 * @param animateSelectionChange 바깥에서 [selectedIndex]가 바뀌면 바로 옮기지 않고 그 값까지 굴러간다.
 * 다른 휠을 따라 넘어가는 휠(오전/오후)에 쓴다.
 */
@Composable
fun NyummyWheelColumn(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    contentDescription: String? = null,
    animateSelectionChange: Boolean = false,
) {
    val rowHeight = NyummyComponentDimens.WheelPickerRowHeight
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    // 위아래 빈 행을 실제 아이템으로 넣어 "첫 보이는 아이템 인덱스 == 가운데 데이터 인덱스"를 유지한다.
    // contentPadding 방식은 초기 스크롤 위치와 겹치면 한 칸 어긋날 수 있다.
    val rowCount = itemCount + 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeSelectedIndex)
    val centeredRow by remember(listState, itemCount) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .filter { rowToItemIndex(it.index, itemCount) != null }
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.index
        }
    }
    val latestSelectedIndex by rememberUpdatedState(safeSelectedIndex)
    val centeredIndex by remember(itemCount) {
        derivedStateOf { centeredRow?.let { rowToItemIndex(it, itemCount) } ?: latestSelectedIndex }
    }
    // 바깥에서 값이 바뀌어 옮기는 중인 목표 값. 이 동안 지나치는 값은 사용자가 고른 게 아니므로 알리지 않는다.
    var syncTarget by remember { mutableStateOf<Int?>(null) }

    // 코루틴이 처음 값을 붙잡지 않도록 최신 콜백을 참조한다.
    val latestOnChange by rememberUpdatedState(onSelectedIndexChange)
    LaunchedEffect(listState, itemCount) {
        snapshotFlow { centeredIndex to (syncTarget == null) }
            .filter { (_, byUser) -> byUser }
            .collect { (index, _) -> if (index != latestSelectedIndex) latestOnChange(index) }
    }
    LaunchedEffect(safeSelectedIndex, itemCount) {
        val userScrolling = listState.isScrollInProgress && syncTarget == null
        if (userScrolling || centeredIndex == safeSelectedIndex) return@LaunchedEffect
        syncTarget = safeSelectedIndex
        try {
            // 첫 보이는 행(가운데 바로 위 행)의 번호가 곧 데이터 인덱스다.
            if (animateSelectionChange) {
                listState.animateScrollToItem(safeSelectedIndex)
            } else {
                listState.scrollToItem(safeSelectedIndex)
            }
        } finally {
            if (syncTarget == safeSelectedIndex) syncTarget = null
        }
    }

    val selectedLabel = items.getOrNull(centeredIndex).orEmpty() + unit.orEmpty()
    LazyColumn(
        modifier = modifier
            .height(NyummyComponentDimens.WheelPickerHeight)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                stateDescription = selectedLabel
            },
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
    ) {
        items(rowCount) { row ->
            val item = rowToItemIndex(row, itemCount)?.let(items::get)
            if (item == null) {
                Spacer(Modifier.height(rowHeight))
                return@items
            }
            val centered = row == centeredRow
            // alignByBaseline을 쓰면 Row의 세로 정렬이 무시되므로, 기준선 정렬 Row를 가운데 정렬 Box로 감싼다.
            Box(
                modifier = Modifier.fillParentMaxWidth().height(rowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
                    NyummyText(
                        text = item,
                        style = if (centered) NyummyTheme.typography.titleL else NyummyTheme.typography.bodyL,
                        color = if (centered) {
                            NyummyTheme.colors.content.primary
                        } else {
                            NyummyTheme.colors.content.tertiary
                        },
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                    if (centered && unit != null) {
                        NyummyText(
                            text = unit,
                            style = NyummyTheme.typography.labelM,
                            color = NyummyTheme.colors.content.secondary,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }
    }
}

/** 목록 행 번호를 데이터 인덱스로 바꾼다. 위아래 빈 행은 null이다. */
private fun rowToItemIndex(row: Int, itemCount: Int): Int? = (row - 1).takeIf { it in 0 until itemCount }

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyWheelPickerPreview() {
    NyummyTheme {
        val heights = remember { (140..200).map(Int::toString).toImmutableList() }
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            NyummyWheelPicker(
                items = heights,
                selectedIndex = 32,
                onSelectedIndexChange = {},
                unit = "cm",
                modifier = Modifier.weight(1f),
            )
            NyummyWheelPickerFrame(modifier = Modifier.weight(1f)) {
                val hours = remember { (0..23).map { "%02d".format(it) }.toImmutableList() }
                val minutes = remember { (0..50 step 10).map { "%02d".format(it) }.toImmutableList() }
                NyummyWheelColumn(items = hours, selectedIndex = 8, onSelectedIndexChange = {}, modifier = Modifier.weight(1f))
                NyummyWheelColumn(items = minutes, selectedIndex = 3, onSelectedIndexChange = {}, modifier = Modifier.weight(1f))
            }
        }
    }
}
