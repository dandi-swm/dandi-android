package com.dandi.nyummy.room.presentation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 방 그림 위 아이템 자리 계산. 좌표 규칙은 서버 저장값과 같다(RoomPlacementVO).
 *
 * - x, y: 아이템 바닥 가운데 점. 배경 그림 전체를 (0,0)~(1,1)로 본 비율이다.
 * - scale: 기본 크기 대비 배율. 기본 크기는 그림의 긴 변이 방 폭의 [ITEM_BASE_FRACTION]이 되는 크기다.
 *
 * 화면은 배경 그림을 폭에 맞춰 바닥 기준으로 그리므로(남는 위쪽은 바닥색, 모자라면 위쪽이 잘린다)
 * 같은 좌표가 어느 기기에서나 그림 위 같은 자리에 온다.
 */
internal object RoomGeometry {

    /** 배경 그림의 폭 / 높이. 배경들은 같은 구도(1095×1436)로 그려서 아이템 좌표를 함께 쓴다. */
    const val BACKGROUND_ASPECT = 1095f / 1436f

    /** scale 1.0일 때 아이템 그림의 긴 변이 방 폭에서 차지하는 비율. */
    const val ITEM_BASE_FRACTION = 0.3f

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 2f

    /** 보관함에서 새로 꺼낸 아이템을 놓는 자리(방 가운데 바닥). */
    const val NEW_ITEM_X = 0.5f
    const val NEW_ITEM_Y = 0.8f

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

    /** [point]에 닿은 아이템 중 맨 앞(나중에 그린) 아이템. [rects]는 뒤에서 앞 순서다. 없으면 null. */
    fun <T> topmostAt(rects: List<Pair<T, Rect>>, point: Offset): T? =
        rects.lastOrNull { (_, rect) -> rect.contains(point) }?.first

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * 아이템 바닥 점이 방 그림의 보이는 영역([minY]~[maxY], 높이 대비 비율) 안에 머물게 한다.
     * 좌우는 그림 폭 안(0~1)이다. 보이는 영역이 뒤집힌 아주 작은 화면이면 [maxY]를 쓴다.
     */
    fun clampPosition(x: Float, y: Float, minY: Float, maxY: Float): Offset =
        Offset(
            x = x.coerceIn(0f, 1f),
            y = if (minY <= maxY) y.coerceIn(minY, maxY) else maxY,
        )
}
