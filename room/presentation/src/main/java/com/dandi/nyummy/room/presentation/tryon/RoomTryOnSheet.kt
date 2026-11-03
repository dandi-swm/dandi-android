package com.dandi.nyummy.room.presentation.tryon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.R
import com.dandi.nyummy.room.presentation.component.RoomCellGap
import com.dandi.nyummy.room.presentation.component.RoomItemCell
import com.dandi.nyummy.room.presentation.component.RoomSheetContentMaxWidth
import com.dandi.nyummy.room.presentation.component.RoomSheetHandle
import com.dandi.nyummy.room.presentation.component.RoomSheetMessage
import com.dandi.nyummy.room.presentation.component.RoomSheetState
import com.dandi.nyummy.room.presentation.component.roomSheetReveal
import kotlinx.collections.immutable.ImmutableList
import java.text.NumberFormat
import java.util.Locale

/**
 * 놓아보기 아래 "담은 아이템" 시트. 마이룸 보관함 시트와 같은 손잡이·접기·숨기기를 쓰고, 탭 자리에 머리가 있다.
 * 접으면 손잡이와 머리만 남고, 끝까지 내리면 손잡이만 남는다.
 *
 * - 머리: "담은 아이템 n개"(숫자만 브랜드색)와 "놓아 본 모습은 저장되지 않아요".
 * - 칸: 분류 탭 없이 한 줄로 넘긴다. 칸 아래에 1개 결제가, 살 수 없는 아이템은 "살 수 없어요".
 * - 맨 아래: 띄운 화면이 넘겨준 [footer](장바구니의 결제 바).
 */
@Composable
internal fun RoomTryOnSheet(
    state: RoomSheetState,
    loadState: MyRoomLoadState,
    cells: ImmutableList<RoomTryOnCellUiModel>,
    onInteriorClick: (Long) -> Unit,
    onBackgroundClick: (Long) -> Unit,
    onRetry: () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        // 손잡이와 머리: 끌어서 시트를 올리고 내린다.
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
            RoomSheetHandle(
                state = state,
                expandLabel = R.string.room_try_on_sheet_expand,
                collapseLabel = R.string.room_try_on_sheet_collapse,
            )
            // 머리 영역: 숨기는 만큼 아래부터 잘려 시트와 함께 내려간다.
            Box(modifier = Modifier.roomSheetReveal(state::visibleTabsHeightPx)) {
                TryOnSheetHeader(
                    count = cells.size,
                    modifier = Modifier
                        .onSizeChanged { state.tabsHeightPx = it.height }
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = RoomSheetContentMaxWidth)
                        .padding(horizontal = NyummyTheme.spacing.gutter)
                        .padding(bottom = NyummyTheme.spacing.s4),
                )
            }
        }
        // 칸과 결제 바: 펼친 만큼만 보인다.
        Box(modifier = Modifier.roomSheetReveal(state::visibleBodyHeightPx)) {
            Column(
                modifier = Modifier
                    .onSizeChanged { state.bodyHeightPx = it.height }
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = RoomSheetContentMaxWidth)
                        .padding(top = NyummyTheme.spacing.s12)
                        .height(CellRowHeight),
                ) {
                    when (loadState) {
                        MyRoomLoadState.LOADING -> NyummySpinner(
                            contentDescription = stringResource(R.string.my_room_loading),
                            modifier = Modifier.align(Alignment.Center),
                        )
                        MyRoomLoadState.FAILED -> RoomSheetMessage(
                            message = stringResource(R.string.room_try_on_load_failed),
                            actionText = stringResource(R.string.my_room_retry),
                            onAction = onRetry,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        MyRoomLoadState.LOADED -> TryOnCellRow(
                            cells = cells,
                            onInteriorClick = onInteriorClick,
                            onBackgroundClick = onBackgroundClick,
                        )
                    }
                }
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                footer()
            }
        }
    }
}

/** "담은 아이템 n개"(숫자만 브랜드색 굵게)와 저장되지 않는다는 안내. */
@Composable
private fun TryOnSheetHeader(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val countText = stringResource(R.string.room_try_on_header_count, count)
    val title = stringResource(R.string.room_try_on_header, countText)
    val brand = NyummyTheme.colors.content.brand
    val annotated = buildAnnotatedString {
        val countStart = title.lastIndexOf(countText)
        if (countStart < 0) {
            append(title)
        } else {
            append(title.substring(0, countStart))
            withStyle(SpanStyle(color = brand)) { append(countText) }
            append(title.substring(countStart + countText.length))
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(text = annotated, style = NyummyTheme.typography.titleS, maxLines = 1)
        Spacer(Modifier.weight(1f))
        NyummyText(
            text = stringResource(R.string.room_try_on_not_saved),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
            maxLines = 1,
        )
    }
}

/** 담은 아이템 칸을 가로 한 줄로. 배경 칸은 방 배경을 바꾸고, 인테리어 칸은 방에 놓는다. */
@Composable
private fun TryOnCellRow(
    cells: ImmutableList<RoomTryOnCellUiModel>,
    onInteriorClick: (Long) -> Unit,
    onBackgroundClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = NyummyTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(RoomCellGap),
    ) {
        items(cells, key = { it.key }) { cell ->
            RoomItemCell(
                name = cell.name,
                imageUrl = cell.imageUrl,
                isBackground = cell.isBackground,
                isPlaced = cell.isPlaced,
                onClick = { if (cell.isBackground) onBackgroundClick(cell.id) else onInteriorClick(cell.id) },
                footer = { TryOnCellPrice(price = cell.price, isPurchasable = cell.isPurchasable) },
            )
        }
    }
}

/** 칸 아래 1개 결제가(코인과 숫자). 살 수 없는 아이템은 빨간 "살 수 없어요". */
@Composable
private fun TryOnCellPrice(price: Int, isPurchasable: Boolean) {
    if (!isPurchasable) {
        NyummyText(
            text = stringResource(R.string.room_try_on_unavailable),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.danger,
            maxLines = 1,
        )
        return
    }
    val amount = NumberFormat.getNumberInstance(Locale.KOREA).format(price)
    val description = stringResource(R.string.room_try_on_price_description, amount)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(CoinSize))
        NyummyText(text = amount, style = NyummyTheme.typography.numberXs, maxLines = 1)
    }
}

/** 칸 줄 높이. 칸(80) + 이름 + 가격 줄이 들어가고, 불러오는 중이거나 실패해도 같아서 시트가 흔들리지 않는다. */
private val CellRowHeight = 132.dp
private val CoinSize = 16.dp
