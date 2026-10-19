package com.dandi.nyummy.common.presentation.room

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 홈 고양이방 카드와 마이룸이 함께 쓰는 방 그리기 규칙.
 *
 * - 카드는 늘 방 그림(배경)과 같은 2:3 비율이다. 주어진 자리의 폭을 먼저 채우고, 높이가 모자라면 같은 비율로 줄인다.
 *   그래서 방 그림은 어느 기기에서도 잘리지 않고, 홈과 마이룸이 각자 계산해도 같은 모습으로 보인다.
 * - 냐미는 카드 크기에 비례해 크기와 자리를 정해 늘 같은 바닥 자리에 앉는다. 카드가 작아지면 냐미도 같은 비율로 작아진다.
 */
object RoomCardLayout {

    /** 방 그림과 카드의 폭 / 높이. 모든 배경은 이 비율로 만든다(픽셀 원본 360×540). */
    const val Aspect = 2f / 3f

    /** 냐미 자리 같은 값을 정하는 기준 크기(2:3). */
    val DesignWidth = 430.dp
    val DesignHeight = 645.dp

    private val NyummyDesignSize = 155.dp
    private val NyummyDesignBottom = 133.dp

    /** [maxWidth]×[maxHeight] 자리에 들어가는 가장 큰 2:3 카드. */
    fun cardSize(maxWidth: Dp, maxHeight: Dp): DpSize {
        val width = min(maxWidth.value, maxHeight.value * Aspect).coerceAtLeast(0f).dp
        return DpSize(width, width / Aspect)
    }

    /** [width]×[height] 칸을 방 그림으로 빈틈없이 덮는 배율. 2:3 카드에서는 그림이 카드에 꼭 맞는다. */
    fun roomScale(width: Dp, height: Dp): Float = max(width / DesignWidth, height / DesignHeight)

    /** 칸 안에서 방 그림이 차지하는 자리(칸 왼쪽 위가 0). 칸이 2:3이 아니면 넘치는 쪽이 칸 밖으로 나가고 바닥은 맞춘다. */
    fun roomRect(width: Dp, height: Dp): DpRect {
        val scale = roomScale(width, height)
        val roomWidth = DesignWidth * scale
        val roomHeight = DesignHeight * scale
        val left = (width - roomWidth) / 2
        val top = height - roomHeight
        return DpRect(left = left, top = top, right = left + roomWidth, bottom = height)
    }

    /** 칸 안에서 냐미(정사각 스프라이트)가 앉는 자리. 가운데, 칸 바닥에서 일정 높이 위다. */
    fun nyummyRect(width: Dp, height: Dp): DpRect {
        val scale = roomScale(width, height)
        val size = NyummyDesignSize * scale
        val top = height - NyummyDesignBottom * scale - size
        val left = (width - size) / 2
        return DpRect(left = left, top = top, right = left + size, bottom = top + size)
    }
}

/**
 * 주어진 자리 가운데에 2:3 고양이방 카드를 둔다. 자리 크기는 그대로 차지하고, 카드는 [RoomCardLayout.cardSize] 크기다.
 * 남는 쪽(폭이 넓으면 좌우, 높이가 길면 위아래)은 비워 둔다.
 */
fun Modifier.fitRoomCard(): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val width = min(constraints.maxWidth, (constraints.maxHeight * RoomCardLayout.Aspect).roundToInt())
    val height = (width / RoomCardLayout.Aspect).roundToInt().coerceAtMost(constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place((constraints.maxWidth - width) / 2, (constraints.maxHeight - height) / 2)
    }
}
