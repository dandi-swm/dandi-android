package com.dandi.nyummy.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.room.presentation.R
import com.dandi.nyummy.room.presentation.RoomBackgroundUiModel
import com.dandi.nyummy.room.presentation.RoomPlacementUiModel
import com.dandi.nyummy.room.presentation.model.RoomGeometry
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 마이룸 무대. 배경 그림을 폭에 맞춰 바닥 기준으로 그리고(남는 위쪽은 바닥색, 모자라면 위쪽이 잘린다)
 * 그 위에 놓인 아이템을 뒤에서 앞 순서로 그린다. 픽셀 그림이라 확대할 때 번지지 않게 필터 없이 그린다.
 *
 * - 아이템을 누르면 고르고, 빈 곳을 누르면 고르기를 푼다.
 * - 아이템을 끌면 옮긴다. 끄는 동안 원래 자리는 흐리게 남기고 위에 안내를 띄운다.
 * - 두 손가락으로 벌리거나 오므리면 손가락 아래 아이템(없으면 고른 아이템)을 키우거나 줄인다.
 * - 고른 아이템에는 테두리와 도구 모음(맨 앞으로, 보관함에 넣기)을 붙인다.
 *
 * 끌기와 확대, 축소는 손을 뗄 때 한 번만 [onMove]로 알려서 되돌리기 한 단계가 된다.
 *
 * @param bottomInset 아래쪽에서 보관함 시트에 가려지는 높이. 아이템 바닥이 이 아래로 내려가지 않게 한다.
 */
@Composable
internal fun RoomStage(
    background: RoomBackgroundUiModel?,
    items: ImmutableList<RoomPlacementUiModel>,
    selectedItemId: Long?,
    bottomInset: Dp,
    onSelect: (Long?) -> Unit,
    onMove: (roomItemId: Long, x: Float, y: Float, scale: Float) -> Unit,
    onBringToFront: () -> Unit,
    onPutAway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(NyummyTheme.colors.bg.sceneRoomFloor),
    ) {
        val density = LocalDensity.current
        val frame = remember(constraints.maxWidth, constraints.maxHeight, bottomInset, density) {
            RoomFrame.of(
                stageWidth = constraints.maxWidth.toFloat(),
                stageHeight = constraints.maxHeight.toFloat(),
                bottomInset = with(density) { bottomInset.toPx() },
            )
        }
        // 그림 주소별 폭 / 높이와 받지 못한 그림. 같은 아이템을 여러 개 놓아도 한 번만 잰다.
        val aspects = remember { mutableStateMapOf<String, Float>() }
        val failedUrls = remember { mutableStateMapOf<String, Boolean>() }
        var moving by remember { mutableStateOf<MovingItem?>(null) }

        fun rectOf(item: RoomPlacementUiModel): Rect = frame.itemRect(item, aspects[item.imageUrl])

        if (background != null) {
            AsyncImage(
                model = background.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
                modifier = Modifier.placeAt(frame.roomRect),
            )
        }

        val currentItems by rememberUpdatedState(items)
        val currentSelectedId by rememberUpdatedState(selectedItemId)
        val currentOnSelect by rememberUpdatedState(onSelect)
        val currentOnMove by rememberUpdatedState(onMove)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(frame) {
                    detectRoomGestures(
                        frame = frame,
                        items = { currentItems },
                        aspectOf = { aspects[it] },
                        selectedId = { currentSelectedId },
                        onMovingChange = { moving = it },
                        onSelect = { currentOnSelect(it) },
                        onMove = { id, x, y, scale -> currentOnMove(id, x, y, scale) },
                    )
                },
        ) {
            val lifted = moving?.takeIf { it.hasMoved }
            items.forEach { item ->
                key(item.roomItemId) {
                    RoomItemImage(
                        imageUrl = item.imageUrl,
                        rect = rectOf(item),
                        // 끄는 동안 원래 자리는 흐리게 남긴다.
                        alpha = if (lifted?.roomItemId == item.roomItemId) MovingOriginAlpha else 1f,
                        isFailed = failedUrls[item.imageUrl] == true,
                        onAspect = { aspects[item.imageUrl] = it },
                        onFailed = { failedUrls[item.imageUrl] = true },
                    )
                }
            }
            val liftedItem = lifted?.let { m -> items.firstOrNull { it.roomItemId == m.roomItemId }?.let(m::applyTo) }
            if (liftedItem != null) {
                RoomItemImage(
                    imageUrl = liftedItem.imageUrl,
                    rect = rectOf(liftedItem),
                    alpha = 1f,
                    isFailed = failedUrls[liftedItem.imageUrl] == true,
                    onAspect = {},
                    onFailed = {},
                )
            }
        }

        val selected = items.firstOrNull { it.roomItemId == selectedItemId }
            ?.let { item -> moving?.takeIf { it.roomItemId == item.roomItemId }?.applyTo(item) ?: item }
        if (selected != null) {
            val rect = rectOf(selected)
            SelectionFrame(rect = rect)
            if (moving?.hasMoved != true) {
                ItemToolbar(
                    itemRect = rect,
                    stageWidth = frame.stageWidth,
                    onBringToFront = onBringToFront,
                    onPutAway = onPutAway,
                )
            }
        }
        if (moving?.hasMoved == true) {
            DragHint(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = NyummyTheme.spacing.s16),
            )
        }
    }
}

/**
 * 무대 위 방 그림의 자리(무대 좌표, px). 방 그림은 무대 폭에 맞추고 바닥을 무대 바닥에 붙인다.
 *
 * @property visibleTop 방 그림 중 보이는 맨 위(높이 대비 비율). 위쪽이 잘렸으면 0보다 크다.
 * @property visibleBottom 시트에 가리지 않는 맨 아래(높이 대비 비율)
 */
@Immutable
internal data class RoomFrame(
    val stageWidth: Float,
    val roomRect: Rect,
    val visibleTop: Float,
    val visibleBottom: Float,
) {
    /** 아이템이 무대에서 차지하는 사각형(px). 그림 크기를 아직 모르면 정사각형으로 본다. */
    fun itemRect(item: RoomPlacementUiModel, aspect: Float?): Rect =
        RoomGeometry.itemRect(
            roomWidth = roomRect.width,
            roomHeight = roomRect.height,
            x = item.x,
            y = item.y,
            scale = item.scale,
            imageAspect = aspect ?: RoomGeometry.UNKNOWN_IMAGE_ASPECT,
        ).translate(roomRect.topLeft)

    companion object {
        fun of(stageWidth: Float, stageHeight: Float, bottomInset: Float): RoomFrame {
            val roomHeight = stageWidth / RoomGeometry.BACKGROUND_ASPECT
            val roomTop = stageHeight - roomHeight
            return RoomFrame(
                stageWidth = stageWidth,
                roomRect = Rect(0f, roomTop, stageWidth, stageHeight),
                visibleTop = max(0f, -roomTop) / roomHeight,
                visibleBottom = (stageHeight - bottomInset - roomTop) / roomHeight,
            )
        }
    }
}

/** 손으로 옮기거나 키우는 중인 아이템. 손을 떼기 전까지는 화면에만 보이고 상태에는 넣지 않는다. */
@Immutable
private data class MovingItem(
    val roomItemId: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val hasMoved: Boolean = false,
) {
    fun applyTo(item: RoomPlacementUiModel) = item.copy(x = x, y = y, scale = scale)
}

private suspend fun PointerInputScope.detectRoomGestures(
    frame: RoomFrame,
    items: () -> List<RoomPlacementUiModel>,
    aspectOf: (String) -> Float?,
    selectedId: () -> Long?,
    onMovingChange: (MovingItem?) -> Unit,
    onSelect: (Long?) -> Unit,
    onMove: (roomItemId: Long, x: Float, y: Float, scale: Float) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    val placements = items()
    val touchedId = RoomGeometry.topmostAt(
        rects = placements.map { it.roomItemId to frame.itemRect(it, aspectOf(it.imageUrl)) },
        point = down.position,
    )
    var moving: MovingItem? = null
    var pastSlop = false
    var canceled = false
    var zoomSoFar = 1f
    var panSoFar = Offset.Zero
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) {
            canceled = true
            break
        }
        if (event.changes.none { it.pressed }) break
        val zoom = event.calculateZoom()
        val pan = event.calculatePan()
        if (!pastSlop) {
            zoomSoFar *= zoom
            panSoFar += pan
            val zoomMotion = abs(1f - zoomSoFar) * event.calculateCentroidSize(useCurrent = false)
            if (zoomMotion > viewConfiguration.touchSlop || panSoFar.getDistance() > viewConfiguration.touchSlop) {
                pastSlop = true
                val isPinching = event.changes.count { it.pressed } > 1
                // 한 손가락 끌기는 손가락 아래 아이템만, 두 손가락은 손가락 아래가 비었으면 고른 아이템을 다룬다.
                val targetId = touchedId ?: selectedId().takeIf { isPinching }
                val target = placements.firstOrNull { it.roomItemId == targetId }
                if (target != null) {
                    moving = MovingItem(target.roomItemId, target.x, target.y, target.scale)
                    if (selectedId() != target.roomItemId) onSelect(target.roomItemId)
                }
            }
        }
        val current = moving
        if (pastSlop && current != null) {
            val target = placements.first { it.roomItemId == current.roomItemId }
            val scale = RoomGeometry.clampScale(current.scale * zoom)
            val height = frame.itemRect(target.copy(scale = scale), aspectOf(target.imageUrl)).height / frame.roomRect.height
            // 바닥 점이 보이는 영역 안에 있고, 아이템이 절반 넘게 위로 잘리지 않게 한다.
            val position = RoomGeometry.clampPosition(
                x = current.x + pan.x / frame.roomRect.width,
                y = current.y + pan.y / frame.roomRect.height,
                minY = frame.visibleTop + height / 2f,
                maxY = frame.visibleBottom,
            )
            moving = current.copy(x = position.x, y = position.y, scale = scale, hasMoved = true)
            onMovingChange(moving)
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        }
    }
    val finished = moving
    onMovingChange(null)
    when {
        canceled -> Unit
        finished != null -> onMove(finished.roomItemId, finished.x, finished.y, finished.scale)
        !pastSlop -> onSelect(touchedId)
    }
}

/** 놓인 아이템 그림 하나. 그림을 받으면 실제 폭 / 높이를 [onAspect]로 알리고, 받지 못하면 빈 자리를 흐리게 보여 준다. */
@Composable
private fun RoomItemImage(
    imageUrl: String,
    rect: Rect,
    alpha: Float,
    isFailed: Boolean,
    onAspect: (Float) -> Unit,
    onFailed: () -> Unit,
) {
    Box(
        modifier = Modifier
            .placeAt(rect)
            .graphicsLayer { this.alpha = alpha },
    ) {
        if (isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.s)),
            )
        }
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.isSpecified && size.height > 0f) onAspect(size.width / size.height)
            },
            onError = { onFailed() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 고른 아이템 테두리. 브랜드색 굵은 선 위에 흰 점선을 겹친다. 아이템보다 조금 넓게 두른다. */
@Composable
private fun SelectionFrame(rect: Rect) {
    val density = LocalDensity.current
    val inset = with(density) { SelectionPadding.toPx() }
    val primary = NyummyTheme.colors.bg.actionPrimary
    val dash = NyummyTheme.colors.content.onAction
    Canvas(modifier = Modifier.placeAt(rect.inflate(inset))) {
        val radius = CornerRadius(SelectionRadius.toPx())
        val outer = SelectionStroke.toPx()
        val inner = SelectionDashStroke.toPx()
        drawRoundRect(
            color = primary,
            topLeft = Offset(outer / 2f, outer / 2f),
            size = size.copy(width = size.width - outer, height = size.height - outer),
            cornerRadius = radius,
            style = Stroke(width = outer),
        )
        drawRoundRect(
            color = dash,
            topLeft = Offset(outer / 2f, outer / 2f),
            size = size.copy(width = size.width - outer, height = size.height - outer),
            cornerRadius = radius,
            style = Stroke(
                width = inner,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(SelectionDash.toPx(), SelectionGap.toPx())),
            ),
        )
    }
}

/**
 * 고른 아이템 위 도구 모음(맨 앞으로, 보관함에 넣기). 아이템 가운데 위에 띄우고, 위로 자리가 없으면 아래에 둔다.
 * 무대 좌우 밖으로는 나가지 않는다.
 */
@Composable
private fun ItemToolbar(
    itemRect: Rect,
    stageWidth: Float,
    onBringToFront: () -> Unit,
    onPutAway: () -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    val selectionInset = SelectionPadding
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val gap = (ToolbarGap + selectionInset).roundToPx()
                val edge = ToolbarEdgeMargin.roundToPx()
                val above = itemRect.top.roundToInt() - gap - placeable.height
                val y = if (above >= edge) above else itemRect.bottom.roundToInt() + gap
                val x = (itemRect.center.x.roundToInt() - placeable.width / 2)
                    .coerceIn(edge, max(edge, stageWidth.roundToInt() - edge - placeable.width))
                layout(placeable.width, placeable.height) { placeable.place(x, y) }
            }
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(ToolbarPadding),
        horizontalArrangement = Arrangement.spacedBy(ToolbarButtonGap),
    ) {
        NyummyIconButton(
            icon = CommonR.drawable.nyummy_ic_bring_to_front,
            contentDescription = stringResource(R.string.my_room_bring_to_front),
            onClick = onBringToFront,
        )
        NyummyIconButton(
            icon = CommonR.drawable.nyummy_ic_archive,
            contentDescription = stringResource(R.string.my_room_put_away),
            onClick = onPutAway,
        )
    }
}

/** 끄는 동안 무대 위에 띄우는 안내. */
@Composable
private fun DragHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(NyummyTheme.colors.bg.scrim, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(horizontal = HintHorizontalPadding, vertical = NyummyTheme.spacing.s8),
    ) {
        NyummyText(
            text = stringResource(R.string.my_room_drag_hint),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.onAction,
            maxLines = 1,
        )
    }
}

/** 무대 좌표 [rect] 자리에 그 크기로 둔다. */
private fun Modifier.placeAt(rect: Rect): Modifier = this
    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
    .layout { measurable, _ ->
        val width = rect.width.roundToInt().coerceAtLeast(0)
        val height = rect.height.roundToInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.place(0, 0) }
    }

/** 끄는 동안 원래 자리에 남기는 그림의 불투명도. */
private const val MovingOriginAlpha = 0.35f

private val SelectionPadding = 6.dp
private val SelectionRadius = 12.dp
private val SelectionStroke = 4.dp
private val SelectionDashStroke = 2.dp
private val SelectionDash = 6.dp
private val SelectionGap = 4.dp
private val ToolbarGap = 8.dp
private val ToolbarEdgeMargin = 8.dp
private val ToolbarPadding = 6.dp
private val ToolbarButtonGap = 2.dp
private val HintHorizontalPadding = 14.dp
