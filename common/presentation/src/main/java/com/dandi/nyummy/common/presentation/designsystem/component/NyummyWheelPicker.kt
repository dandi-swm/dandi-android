package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
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
                .background(NyummyTheme.colors.bg.selected, RoundedCornerShape(NyummyTheme.radius.s)),
        )
        Row(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * 스냅 스크롤 휠 한 열. 가운데 행이 선택 값이고 스크롤이 멈추면 [onSelectedIndexChange]로 알린다.
 * 가운데 값은 number/m + 단위(label/m), 위아래 값은 number/s tertiary.
 */
@Composable
fun NyummyWheelColumn(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    contentDescription: String? = null,
) {
    val rowHeight = NyummyComponentDimens.WheelPickerRowHeight
    val lastIndex = items.lastIndex.coerceAtLeast(0)
    // 위아래 빈 행을 실제 아이템으로 넣어 "첫 보이는 아이템 인덱스 == 가운데 데이터 인덱스"를 유지한다.
    // contentPadding 방식은 초기 스크롤 위치와 겹치면 한 칸 어긋날 수 있다.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, lastIndex))
    val centeredIndex by remember(listState, items) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .filter { it.index in 1..items.size }
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.let { (it.index - 1).coerceIn(0, lastIndex) }
                ?: selectedIndex.coerceIn(0, lastIndex)
        }
    }

    // 코루틴이 처음 값을 붙잡지 않도록 최신 선택값과 콜백을 참조한다.
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnChange by rememberUpdatedState(onSelectedIndexChange)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .collect { if (centeredIndex != latestSelectedIndex) latestOnChange(centeredIndex) }
    }
    LaunchedEffect(selectedIndex, items.size) {
        if (!listState.isScrollInProgress && centeredIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex.coerceIn(0, lastIndex))
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
        item { Spacer(Modifier.height(rowHeight)) }
        itemsIndexed(items) { index, item ->
            val centered = index == centeredIndex
            // alignByBaseline을 쓰면 Row의 세로 정렬이 무시되므로, 기준선 정렬 Row를 가운데 정렬 Box로 감싼다.
            Box(
                modifier = Modifier.fillParentMaxWidth().height(rowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
                    NyummyText(
                        text = item,
                        style = if (centered) NyummyTheme.typography.numberM else NyummyTheme.typography.numberS,
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
        item { Spacer(Modifier.height(rowHeight)) }
    }
}

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
