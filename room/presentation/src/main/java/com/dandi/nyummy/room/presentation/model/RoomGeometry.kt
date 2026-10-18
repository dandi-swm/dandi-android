package com.dandi.nyummy.room.presentation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.dandi.nyummy.room.entity.RoomPlacementVO

/**
 * 방 그림 위 아이템 자리 계산.
 *
 * - x, y: 아이템 바닥 가운데 점. 그리기 편하게 배경 그림의 **왼쪽 위**를 (0,0), 오른쪽 아래를 (1,1)로 본 비율이다.
 *   서버 저장값(RoomPlacementVO)은 왼쪽 아래 기준 퍼센트(0~100)라서 [fromSavedPosition], [toSavedPosition]으로 바꾼다.
 * - scale: 기본 크기 대비 배율. 기본 크기는 그림의 긴 변이 방 폭의 [ITEM_BASE_FRACTION]이 되는 크기다.
 *
 * 방 그림은 홈 고양이방 카드와 같은 규칙(RoomCardLayout)으로 그리므로 같은 좌표가 마이룸과 홈 카드에서
 * 그림 위 같은 자리에 온다.
 */
internal object RoomGeometry {

    /** scale 1.0일 때 아이템 그림의 긴 변이 방 폭에서 차지하는 비율. */
    const val ITEM_BASE_FRACTION = 0.3f

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 2f

    /** 보관함에서 새로 꺼낸 아이템 바닥 점의 높이(방 바닥 쪽). 시트에 가리면 시트 바로 위로 올린다. */
    const val NEW_ITEM_Y = 0.8f

    private const val SAVED_POSITION_MAX = RoomPlacementVO.MAX_POSITION

    /** 그림 크기를 아직 모를 때 쓰는 폭 / 높이. 정사각형으로 보고 그림을 받으면 다시 잰다. */
    const val UNKNOWN_IMAGE_ASPECT = 1f

    /**
     * 방 그림 좌표계(그림 왼쪽 위가 0, 픽셀 단위)에서 아이템이 차지하는 사각형.
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
        val longSide = roomWidth * ITEM_BASE_FRACTION * scale
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

    /** 저장된 좌표(그림 왼쪽 아래 기준 퍼센트 0~100)를 그리는 좌표(왼쪽 위 기준 0~1)로. 범위를 넘으면 그 안으로 맞춘다. */
    fun fromSavedPosition(x: Float, y: Float): Offset = Offset(
        x = (x / SAVED_POSITION_MAX).coerceIn(0f, 1f),
        y = (1f - y / SAVED_POSITION_MAX).coerceIn(0f, 1f),
    )

    /** 그리는 좌표(왼쪽 위 기준 0~1)를 저장할 좌표(그림 왼쪽 아래 기준 퍼센트 0~100)로. 범위를 넘으면 그 안으로 맞춘다. */
    fun toSavedPosition(x: Float, y: Float): Offset = Offset(
        x = (x * SAVED_POSITION_MAX).coerceIn(0f, SAVED_POSITION_MAX),
        y = ((1f - y) * SAVED_POSITION_MAX).coerceIn(0f, SAVED_POSITION_MAX),
    )

    /** [point]에 닿은 아이템 중 맨 앞(나중에 그린) 아이템. [rects]는 뒤에서 앞 순서다. 없으면 null. */
    fun <T> topmostAt(rects: List<Pair<T, Rect>>, point: Offset): T? =
        rects.lastOrNull { (_, rect) -> rect.contains(point) }?.first

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * 아이템 바닥 점이 방 그림의 보이는 영역(가로 [minX]~[maxX], 세로 [minY]~[maxY], 그림 대비 비율) 안에 머물게 한다.
     * 보이는 영역은 언제나 그림(0~1) 안으로 다시 자른다. 영역이 뒤집힌 아주 작은 화면이면 아래 끝([maxY])을 쓴다.
     */
    fun clampPosition(x: Float, y: Float, minX: Float, maxX: Float, minY: Float, maxY: Float): Offset {
        val left = minX.coerceIn(0f, 1f)
        val right = maxX.coerceIn(left, 1f)
        val bottom = maxY.coerceIn(0f, 1f)
        return Offset(
            x = x.coerceIn(left, right),
            y = if (minY <= bottom) y.coerceIn(minY.coerceAtLeast(0f), bottom) else bottom,
        )
    }
}
