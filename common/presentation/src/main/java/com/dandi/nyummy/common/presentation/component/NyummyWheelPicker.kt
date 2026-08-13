package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * 휠 피커 컨테이너 (Figma `회원가입`의 birthdate/height/weight picker 패턴).
 *
 * 카드 배경과 선택 행 위·아래 구분선을 그리고, 내부에 [NyummyWheelPicker]를
 * 하나 이상 나란히 배치한다. 각 휠은 `Modifier.weight(1f)`로 균등 분할한다.
 */
@Composable
fun NyummyWheelPickerFrame(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius24)
    Box(
        modifier = modifier
            .height(WheelPickerFrameHeight)
            .clip(shape)
            .background(colors.bgDefaultLevel1)
            .border(BorderStroke(WheelPickerFrameBorderWidth, colors.borderDefaultLevel1), shape),
    ) {
        WheelPickerDivider(offsetY = WheelPickerRowHeight)
        WheelPickerDivider(offsetY = WheelPickerRowHeight * 2)
        Row(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun WheelPickerDivider(offsetY: Dp) {
    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space16)
            .fillMaxWidth()
            .height(WheelPickerDividerThickness)
            .background(DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1),
    )
}

/**
 * 세로 스냅 휠 한 열. 가운데 행이 선택 값이며, 스크롤이 멈추면
 * [onSelectedIndexChange]로 가운데에 정착한 인덱스를 알린다.
 *
 * @param unitLabel 선택 값 오른쪽에 붙는 단위 텍스트 (예: "년", "cm")
 */
@Composable
fun NyummyWheelPicker(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unitLabel: String? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    // 위아래 스페이서를 실제 아이템으로 넣는다. contentPadding 방식은 초기
    // 스크롤(initialFirstVisibleItemIndex)과 조합될 때 패딩이 밀려나 선택 행이
    // 한 칸 어긋날 수 있다. 스페이서 방식에서는 "첫 보이는 아이템 리스트 인덱스 ==
    // 가운데 행의 데이터 인덱스"가 항상 성립한다.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val centeredIndex by remember(listState, items, selectedIndex) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .filter { it.index in 1..items.size }
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.let { (it.index - 1).coerceIn(0, items.lastIndex.coerceAtLeast(0)) }
                ?: selectedIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .collect { onSelectedIndexChange(centeredIndex) }
    }
    LaunchedEffect(selectedIndex, items.size) {
        if (!listState.isScrollInProgress && centeredIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)))
        }
    }

    Row(
        modifier = modifier.height(WheelPickerFrameHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .height(WheelPickerFrameHeight),
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            item { Spacer(Modifier.height(WheelPickerRowHeight)) }
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(WheelPickerRowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    val centered = index == centeredIndex
                    DandiText(
                        text = item,
                        color = if (centered) colors.contentDefaultLevel0 else colors.contentDefaultLevel3,
                        style = if (centered) {
                            DesignSystemThemeImpl.typeScale.displayStrongXL
                        } else {
                            DesignSystemThemeImpl.typeScale.textStrongL
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(WheelPickerRowHeight)) }
        }
        if (unitLabel != null) {
            DandiText(
                text = unitLabel,
                modifier = Modifier.padding(end = DesignSystemThemeImpl.designSystemSpacing.space16),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
        }
    }
}

private val WheelPickerFrameHeight = 150.dp
private val WheelPickerRowHeight = 50.dp
private val WheelPickerFrameBorderWidth = 1.dp
private val WheelPickerDividerThickness = 1.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyWheelPickerPreview() {
    DesignSystemTheme {
        val years = remember { (1990..2010).map(Int::toString).toImmutableList() }
        val months = remember { (1..12).map { month -> "%02d".format(month) }.toImmutableList() }
        val days = remember { (1..30).map { day -> "%02d".format(day) }.toImmutableList() }
        NyummyWheelPickerFrame(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSystemThemeImpl.designSystemLayout.mobileGutter),
        ) {
            NyummyWheelPicker(
                items = years,
                selectedIndex = 9,
                onSelectedIndexChange = {},
                modifier = Modifier.weight(1f),
                unitLabel = "년",
            )
            NyummyWheelPicker(
                items = months,
                selectedIndex = 5,
                onSelectedIndexChange = {},
                modifier = Modifier.weight(1f),
                unitLabel = "월",
            )
            NyummyWheelPicker(
                items = days,
                selectedIndex = 14,
                onSelectedIndexChange = {},
                modifier = Modifier.weight(1f),
                unitLabel = "일",
            )
        }
    }
}
