package com.dandi.nyummy.room.presentation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 마이룸에서 아이템을 옮길 때의 자리 계산. 아이템 크기와 좌표 규칙은 홈과 함께 쓰는 RoomCardLayout에 있다.
 *
 * - x, y: 아이템 바닥 가운데 점. 그리기 편하게 배경 그림의 **왼쪽 위**를 (0,0), 오른쪽 아래를 (1,1)로 본 비율이다.
 */
internal object RoomGeometry {

    /** 보관함에서 새로 꺼낸 아이템 바닥 점의 높이(방 바닥 쪽, 놓을 수 있는 곳 안). 시트에 가리면 시트 바로 위로 올린다. */
    const val NEW_ITEM_Y = 0.75f

    /** [point]에 닿은 아이템 중 맨 앞(나중에 그린) 아이템. [rects]는 뒤에서 앞 순서다. 없으면 null. */
    fun <T> topmostAt(rects: List<Pair<T, Rect>>, point: Offset): T? =
        rects.lastOrNull { (_, rect) -> rect.contains(point) }?.first

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
