package com.dandi.nyummy.common.presentation.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

/**
 * 방에 놓인 인테리어 하나(그리는 좌표).
 *
 * @property x 바닥 가운데 점의 가로 위치(방 그림 왼쪽 끝 0, 오른쪽 끝 1)
 * @property y 바닥 가운데 점의 세로 위치(방 그림 위 끝 0, 아래 끝 1)
 * @property scale 기본 크기 대비 배율
 */
@Immutable
data class RoomSceneItem(
    val roomItemId: Long,
    val imageUrl: String,
    val x: Float,
    val y: Float,
    val scale: Float,
)

/**
 * 꾸민 방. 배경과 놓인 아이템을 [RoomCardLayout] 규칙으로 그려서 마이룸에서 꾸민 모습과 같다. 누를 수 없다.
 * 픽셀 그림이라 필터 없이 그리고, 받지 못한 아이템 그림은 그리지 않는다.
 *
 * @param backgroundUrl 깔린 배경 그림. 없거나 받지 못하면 앱에 든 기본 빈 방이 보인다.
 * @param items 놓인 아이템. 뒤에서 앞 순서다.
 */
@Composable
fun RoomScene(
    backgroundUrl: String?,
    items: ImmutableList<RoomSceneItem>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val room = RoomCardLayout.roomRect(maxWidth, maxHeight)
        val roomRect = with(density) { Rect(room.left.toPx(), room.top.toPx(), room.right.toPx(), room.bottom.toPx()) }
        // 그림 주소별 폭 / 높이. 같은 아이템을 여러 개 놓아도 한 번만 잰다.
        val aspects = remember { mutableStateMapOf<String, Float>() }
        RoomBackgroundImage(imageUrl = backgroundUrl, roomRect = roomRect)
        items.forEach { item ->
            key(item.roomItemId) {
                RoomItemImage(
                    imageUrl = item.imageUrl,
                    rect = RoomCardLayout.itemRect(
                        roomWidth = roomRect.width,
                        roomHeight = roomRect.height,
                        x = item.x,
                        y = item.y,
                        scale = item.scale,
                        imageAspect = aspects[item.imageUrl] ?: RoomCardLayout.UnknownItemAspect,
                    ).translate(roomRect.topLeft),
                    onAspect = { aspects[item.imageUrl] = it },
                )
            }
        }
    }
}

/**
 * 방 그림(배경)을 [roomRect] 자리에 그린다. 자리가 그리는 곳보다 크면 넘친 쪽은 잘린다.
 * 배경이 없거나, 받는 중이거나, 받지 못하면(인터넷이 끊겼을 때도) 앱에 든 기본 빈 방(줄무늬 벽과 원목 바닥,
 * CDN 아이템 1번과 같은 그림)을 대신 그린다. 대신 그리는 그림도 픽셀 그림이라 필터 없이 그린다.
 */
@Composable
fun RoomBackgroundImage(
    imageUrl: String?,
    roomRect: Rect,
    modifier: Modifier = Modifier,
) {
    val defaultBitmap = ImageBitmap.imageResource(R.drawable.nyummy_room_default_background)
    val defaultRoom = remember(defaultBitmap) { BitmapPainter(defaultBitmap, filterQuality = FilterQuality.None) }
    AsyncImage(
        model = imageUrl?.takeIf { it.isNotBlank() },
        contentDescription = null,
        placeholder = defaultRoom,
        error = defaultRoom,
        fallback = defaultRoom,
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None,
        modifier = Modifier
            .placeAt(roomRect)
            .then(modifier),
    )
}

/**
 * 놓인 아이템 그림 하나를 [rect] 자리에 그린다. 그림을 받으면 실제 폭 / 높이를 [onAspect]로 알린다.
 *
 * @param isFailed 그림을 받지 못했다. 빈 자리를 흐리게 보여 줘서 찾아 뺄 수 있게 한다(꾸미는 화면용).
 * @param onFailed 그림을 받지 못했다.
 */
@Composable
fun RoomItemImage(
    imageUrl: String,
    rect: Rect,
    modifier: Modifier = Modifier,
    isFailed: Boolean = false,
    onAspect: (Float) -> Unit = {},
    onFailed: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .placeAt(rect)
            .then(modifier),
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

/**
 * 방 좌표 [rect] 자리에 그 크기로 둔다(부모 왼쪽 위가 0, px). 부모보다 큰 것(좌우가 잘리는 방 그림)도 왼쪽 위를 [rect]에 맞춘다.
 * 부모보다 큰 크기를 그대로 알리면 Compose가 넘친 만큼 가운데로 옮겨 그려서, 알리는 크기만 부모 안으로 줄인다.
 */
fun Modifier.placeAt(rect: Rect): Modifier = this
    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
    .layout { measurable, constraints ->
        val width = rect.width.roundToInt().coerceAtLeast(0)
        val height = rect.height.roundToInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) { placeable.place(0, 0) }
    }
