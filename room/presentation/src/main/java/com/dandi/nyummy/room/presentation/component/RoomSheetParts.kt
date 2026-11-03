package com.dandi.nyummy.room.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.room.presentation.R
import kotlinx.coroutines.launch

/*
 * 마이룸 보관함 시트와 놓아보기 시트가 같이 쓰는 조각: 손잡이, 접히는 영역, 아이템 칸, 안내 칸.
 */

/**
 * 시트 맨 위 손잡이. 누르면 펼치거나 접는다. 끌기는 손잡이를 감싼 쪽이 맡는다.
 *
 * @param expandLabel 접혀 있을 때 손잡이 설명(누르면 펼친다)
 * @param collapseLabel 펼쳐 있을 때 손잡이 설명(누르면 접는다)
 */
@Composable
internal fun RoomSheetHandle(
    state: RoomSheetState,
    modifier: Modifier = Modifier,
    @StringRes expandLabel: Int = R.string.my_room_sheet_expand,
    @StringRes collapseLabel: Int = R.string.my_room_sheet_collapse,
) {
    val scope = rememberCoroutineScope()
    val toggleDescription = stringResource(if (state.isExpanded) collapseLabel else expandLabel)
    Box(
        modifier = modifier
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
}

/**
 * 시트를 접고 펴는 만큼만 보이는 영역. 안은 늘 같은 크기로 재고, [visibleHeight]가 돌려준 높이만큼 위에서부터 보인다.
 * 그래서 접는 동안에도 안의 칸이 다시 배치되지 않는다.
 */
internal fun Modifier.roomSheetReveal(visibleHeight: (fullHeight: Int) -> Int): Modifier = this
    .fillMaxWidth()
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        layout(placeable.width, visibleHeight(placeable.height)) { placeable.place(0, 0) }
    }

/**
 * 아이템 칸. 위에 그림, 아래에 이름(과 [footer]). 모든 종류가 같은 크기라 탭을 바꿔도 줄 높이가 같다.
 * - 인테리어: 그림을 가운데에 맞춰 보여 주고, 방에 놓였으면 흐리게 하고 "배치됨"을 붙인다.
 * - 배경: 방 그림을 칸에 꽉 채워 바닥 기준으로 보여 주고, 깔렸으면 브랜드색 테두리와 "사용 중"을 붙인다.
 *
 * @param footer 이름 아래에 더 보일 것(놓아보기의 가격). 이름과 함께 한 칸으로 읽힌다.
 */
@Composable
internal fun RoomItemCell(
    name: String,
    imageUrl: String,
    isBackground: Boolean,
    isPlaced: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    val stateText = stringResource(if (isBackground) R.string.my_room_background_in_use else R.string.my_room_item_placed)
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    val isInUseBackground = isBackground && isPlaced
    Column(
        modifier = modifier
            .width(RoomCellSize)
            .nyummyClickable(onClick = onClick)
            // 이름과 "배치됨"/"사용 중" 글자를 한 칸으로 묶어 읽는다.
            .semantics(mergeDescendants = true) { selected = isPlaced },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CellLabelGap),
    ) {
        Box(
            modifier = Modifier
                .size(RoomCellSize)
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
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(InteriorImageSize)
                        .alpha(if (isPlaced) PlacedImageAlpha else 1f),
                )
            }
            if (isPlaced) {
                StateChip(
                    text = stateText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = CellChipBottom),
                )
            }
        }
        NyummyText(
            text = name,
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.secondary,
            maxLines = 1,
        )
        footer?.invoke()
    }
}

/** 칸 위 상태 표시(배치됨, 사용 중). 연한 브랜드 바탕에 브랜드 글자. */
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

/** 시트가 비었거나 불러오지 못했을 때. 안내 한 줄과 글자 버튼. */
@Composable
internal fun RoomSheetMessage(
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

/** 시트 안 내용의 최대 폭. 넓은 화면에서는 가운데에 둔다. */
internal val RoomSheetContentMaxWidth = 480.dp

/** 칸 사이 간격. */
internal val RoomCellGap = 10.dp

/** 칸(그림 자리) 한 변. */
internal val RoomCellSize = 80.dp

/** 시트 아래 여백(시스템 바 여백과 별개). */
internal val RoomSheetBottomPadding = 28.dp

/** 방에 놓인 아이템 칸의 그림 불투명도. */
private const val PlacedImageAlpha = 0.35f

/** 손잡이 영역 높이(위 여백 10 + 손잡이 4 + 아래까지 12). 누르면 시트를 펴고 접는다. */
private val HandleTouchHeight = 26.dp
private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private val CellLabelGap = 6.dp
private val CellChipBottom = 6.dp
private val InteriorImageSize = 56.dp
private val BackgroundSelectedBorder = 3.dp
private val StateChipVerticalPadding = 3.dp
