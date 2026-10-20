package com.dandi.nyummy.common.presentation.room

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 홈 고양이방과 마이룸이 함께 쓰는 방 그리기 규칙.
 *
 * - 방 그림(9:16, 픽셀 원본 360×640)은 주어진 자리를 빈틈없이 덮도록 키우고 바닥을 자리 바닥에 맞춘다.
 *   자리가 그림보다 세로로 길면 좌우가, 가로로 넓으면 위쪽이 잘린다. 폰에서는 좌우가 한쪽 0~12% 잘린다.
 * - 그래서 방 그림의 좌우 각각과 위쪽 [BleedFraction]은 잘려도 티가 안 나는 벽으로 그린다. 자리 비율이 너무 길거나
 *   넓어 이보다 많이 잘릴 때만 자리를 줄인다.
 * - 아이템은 기기마다 위에 뜨는 화면 요소(코인, 연속 기록, 하단 내비)에 가리지 않는 곳([PlaceMinX]~[PlaceMaxX],
 *   [PlaceMinY]~[PlaceMaxY])에만 둔다. 가장 작은 폰의 화면 요소와 가장 긴 폰의 좌우 잘림을 함께 피한 범위라
 *   어느 기기에서 놓아도 다른 기기에서 보인다.
 * - 모든 배경은 벽과 바닥 경계를 같은 높이(원본 위에서 360px)에 그린다. 아이템 좌표가 그림 대비 비율이라 경계가 다르면
 *   바닥 아이템이 벽에 뜬다.
 * - 냐미는 방 그림이 커진 배율로 크기와 자리를 정해, 화면 크기와 상관없이 그림 위 같은 바닥 자리에 같은 비율로 앉는다.
 * - 아이템은 저장 좌표(그림 왼쪽 아래 기준 퍼센트)와 배율로 자리와 크기를 정한다. 그래서 홈과 마이룸, 어느 기기에서나
 *   그림 위 같은 자리에 같은 비율로 놓인다.
 */
object RoomCardLayout {

    /** 방 그림(배경)의 폭 / 높이. 모든 배경은 이 비율로 만든다(픽셀 원본 360×640, 배포 1080×1920). */
    const val Aspect = 9f / 16f

    /** 냐미 자리 같은 값을 정하는 기준 크기. 픽셀 원본과 같은 360×640이라 1dp가 원본 1px이다. */
    val DesignWidth = 360.dp
    val DesignHeight = 640.dp

    /** 방 그림의 좌우 각각과 위에서, 기기에 따라 잘려도 되는 몫(그림 크기 대비). 바닥 쪽은 잘리지 않는다. */
    const val BleedFraction = 0.12f

    /** 좌우가 [BleedFraction]보다 많이 잘리지 않는 가장 긴 자리의 폭 / 높이. 이보다 길면 높이를 줄인다. */
    const val MinCardAspect = Aspect * (1f - 2f * BleedFraction)

    /** 위쪽이 [BleedFraction]보다 많이 잘리지 않는 가장 넓은 자리의 폭 / 높이. 이보다 넓으면 폭을 줄인다. */
    const val MaxCardAspect = Aspect / (1f - BleedFraction)

    /**
     * 아이템 바닥 점을 둘 수 있는 곳(그리는 좌표, 그림 왼쪽 위 0, 오른쪽 아래 1). 저장 좌표로는 x 12~88, y 20~67이다.
     * 위는 가장 작은 폰(360×640)에서 코인과 연속 기록 띠가 덮는 곳, 아래는 하단 내비가 덮는 곳을 뺐다.
     */
    const val PlaceMinX = BleedFraction
    const val PlaceMaxX = 1f - BleedFraction
    const val PlaceMinY = 0.33f
    const val PlaceMaxY = 0.8f

    /** 냐미 크기는 그림 폭의 약 36%, 발은 그림 바닥에서 원본 111px 위다. */
    private val NyummyDesignSize = 130.dp
    private val NyummyDesignBottom = 111.dp

    /**
     * [maxWidth]×[maxHeight] 자리에 둘 방 크기. 보통은 자리를 그대로 채우고, 비율이 [MinCardAspect]보다 길거나
     * [MaxCardAspect]보다 넓을 때만 그 비율까지 줄인다.
     */
    fun cardSize(maxWidth: Dp, maxHeight: Dp): DpSize {
        if (maxWidth <= 0.dp || maxHeight <= 0.dp) return DpSize(0.dp, 0.dp)
        val aspect = maxWidth / maxHeight
        return when {
            aspect < MinCardAspect -> DpSize(maxWidth, maxWidth / MinCardAspect)
            aspect > MaxCardAspect -> DpSize(maxHeight * MaxCardAspect, maxHeight)
            else -> DpSize(maxWidth, maxHeight)
        }
    }

    /** [width]×[height] 카드를 방 그림으로 빈틈없이 덮는 배율. */
    fun roomScale(width: Dp, height: Dp): Float = max(width / DesignWidth, height / DesignHeight)

    /** 카드 안에서 방 그림이 차지하는 자리(카드 왼쪽 위가 0). 넘치는 쪽은 카드 밖으로 나가 잘리고 바닥은 맞춘다. */
    fun roomRect(width: Dp, height: Dp): DpRect {
        val scale = roomScale(width, height)
        val roomWidth = DesignWidth * scale
        val roomHeight = DesignHeight * scale
        val left = (width - roomWidth) / 2
        val top = height - roomHeight
        return DpRect(left = left, top = top, right = left + roomWidth, bottom = height)
    }

    /** 카드 안에서 냐미(정사각 스프라이트)가 앉는 자리. 가운데, 카드 바닥에서 일정 높이 위다. */
    fun nyummyRect(width: Dp, height: Dp): DpRect {
        val scale = roomScale(width, height)
        val size = NyummyDesignSize * scale
        val top = height - NyummyDesignBottom * scale - size
        val left = (width - size) / 2
        return DpRect(left = left, top = top, right = left + size, bottom = top + size)
    }

    /** scale 1.0일 때 아이템 그림의 긴 변이 방 그림 폭에서 차지하는 비율. */
    const val ItemBaseFraction = 0.3f

    const val MinItemScale = 0.5f
    const val MaxItemScale = 2f

    /** 아이템 그림 크기를 아직 모를 때 쓰는 폭 / 높이. 정사각형으로 보고 그림을 받으면 다시 잰다. */
    const val UnknownItemAspect = 1f

    /** 저장 좌표의 최댓값(그림 오른쪽 끝, 위 끝). 최솟값은 0이다. 서버, iOS와 같은 퍼센트 규칙이다. */
    private const val SavedPositionMax = 100f

    /**
     * 방 그림 좌표계(그림 왼쪽 위가 0, 픽셀 단위)에서 아이템이 차지하는 사각형.
     * 아이템은 바닥 가운데 점([x], [y], 그림 대비 0~1)에 서고, 긴 변이 방 그림 폭의 [ItemBaseFraction] × [scale]이다.
     *
     * @param roomWidth 방 그림을 그린 폭(px)
     * @param roomHeight 방 그림을 그린 높이(px)
     * @param imageAspect 아이템 그림의 폭 / 높이
     */
    fun itemRect(
        roomWidth: Float,
        roomHeight: Float,
        x: Float,
        y: Float,
        scale: Float,
        imageAspect: Float,
    ): Rect {
        val longSide = roomWidth * ItemBaseFraction * scale
        val width = if (imageAspect >= 1f) longSide else longSide * imageAspect
        val height = if (imageAspect >= 1f) longSide / imageAspect else longSide
        val bottomCenterX = x * roomWidth
        val bottom = y * roomHeight
        return Rect(
            left = bottomCenterX - width / 2f,
            top = bottom - height,
            right = bottomCenterX + width / 2f,
            bottom = bottom,
        )
    }

    fun clampItemScale(scale: Float): Float = scale.coerceIn(MinItemScale, MaxItemScale)

    /** 저장된 좌표(그림 왼쪽 아래 기준 퍼센트 0~100)를 그리는 좌표(왼쪽 위 기준 0~1)로. 범위를 넘으면 그 안으로 맞춘다. */
    fun fromSavedPosition(x: Float, y: Float): Offset = Offset(
        x = (x / SavedPositionMax).coerceIn(0f, 1f),
        y = (1f - y / SavedPositionMax).coerceIn(0f, 1f),
    )

    /** 그리는 좌표(왼쪽 위 기준 0~1)를 저장할 좌표(그림 왼쪽 아래 기준 퍼센트 0~100)로. 범위를 넘으면 그 안으로 맞춘다. */
    fun toSavedPosition(x: Float, y: Float): Offset = Offset(
        x = (x * SavedPositionMax).coerceIn(0f, SavedPositionMax),
        y = ((1f - y) * SavedPositionMax).coerceIn(0f, SavedPositionMax),
    )
}

/**
 * 주어진 자리에 고양이방 카드를 둔다. 자리 크기는 그대로 차지하고, 카드는 [RoomCardLayout.cardSize] 크기다.
 * 보통은 자리를 꽉 채운다. 줄인 경우에만 남는 쪽이 생기는데, 높이가 남으면 카드를 맨 위에, 폭이 남으면 좌우 가운데에 둔다.
 */
fun Modifier.fitRoomCard(): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val maxWidth = constraints.maxWidth
    val maxHeight = constraints.maxHeight
    val aspect = maxWidth.toFloat() / maxHeight
    val (width, height) = when {
        aspect < RoomCardLayout.MinCardAspect -> maxWidth to (maxWidth / RoomCardLayout.MinCardAspect).roundToInt()
        aspect > RoomCardLayout.MaxCardAspect -> (maxHeight * RoomCardLayout.MaxCardAspect).roundToInt() to maxHeight
        else -> maxWidth to maxHeight
    }
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(maxWidth, maxHeight) { placeable.place((maxWidth - width) / 2, 0) }
}

/**
 * 홈 고양이방 카드 크기. 앱 루트가 창 크기에서 홈이 받는 자리를 셈해 늘 공급하고, 마이룸은 이 크기로 방을 보여 준다.
 * 그래서 홈을 거치지 않고 딥링크로 마이룸에 바로 들어와도 홈과 같은 크기, 같은 잘림이다.
 */
val LocalRoomCardSize = compositionLocalOf<DpSize> { error("앱 루트에서 홈 고양이방 카드 크기를 공급해야 한다.") }
