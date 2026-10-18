package com.dandi.nyummy.common.presentation.room

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 홈 고양이방 카드와 마이룸이 함께 쓰는 방 그리기 규칙. 두 화면이 같은 규칙과 같은 크기를 써야
 * 마이룸에서 꾸민 모습이 홈에서도 그대로 보인다.
 *
 * - 방 그림은 칸을 빈틈없이 덮도록 키우고 바닥을 칸 바닥에 맞춘다. 칸이 세로로 길면 좌우가, 가로로 넓으면 위쪽이 잘린다.
 * - 기기마다 잘리는 양이 달라서, 방 그림의 좌우와 위쪽 가장자리([BleedFraction]만큼)는 잘려도 티가 안 나는 것으로 그린다
 *   (나무 벽판, 화면 밖으로 걸친 가구처럼). 창문, 러그 같은 핵심과 아이템은 그 안쪽(안전 영역)에만 둔다.
 * - 카드가 너무 세로로 길면 좌우가 이 여백보다 많이 잘리므로, [limitRoomCardHeight]로 카드 높이를 묶는다.
 * - 냐미는 방 그림이 커진 배율로 크기와 자리를 정해 화면 크기와 상관없이 늘 같은 바닥 자리에 앉는다.
 *   아주 작은 칸에서도 냐미는 [NyummyMinSize] 아래로 줄이지 않는다.
 */
object RoomCardLayout {

    /** 방 그림을 배율 1로 그렸을 때의 크기. 마이룸 배경(1095×1436)도 같은 비율이다. */
    val DesignWidth = 430.dp
    val DesignHeight = 564.dp

    /** 방 그림의 좌우 각각과 위에서, 기기에 따라 잘려도 되는 몫(그림 크기 대비). 바닥 쪽은 잘리지 않는다. */
    const val BleedFraction = 0.15f

    /** 좌우가 [BleedFraction]보다 많이 잘리지 않는 가장 긴 카드의 폭 / 높이. 이보다 긴 카드는 높이를 줄인다. */
    val MinCardAspect: Float = DesignWidth / DesignHeight * (1f - 2f * BleedFraction)

    private val NyummyDesignSize = 155.dp
    private val NyummyDesignBottom = 116.dp
    private val NyummyMinSize = 120.dp

    /** [width]×[height] 칸을 방 그림으로 빈틈없이 덮는 배율. */
    fun roomScale(width: Dp, height: Dp): Float = max(width / DesignWidth, height / DesignHeight)

    /** 칸 안에서 방 그림이 차지하는 자리(칸 왼쪽 위가 0). 넘치는 쪽은 칸 밖으로 나가 잘린다. */
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
        val size = (NyummyDesignSize * scale).coerceAtLeast(NyummyMinSize)
        val top = height - NyummyDesignBottom * scale - size
        val left = (width - size) / 2
        return DpRect(left = left, top = top, right = left + size, bottom = top + size)
    }
}

/**
 * 고양이방 카드 높이에 상한을 둔다. 주어진 자리가 [RoomCardLayout.MinCardAspect]보다 세로로 길면(태블릿 세로 등)
 * 카드를 그 비율까지만 키우고 남는 높이의 가운데에 둔다. 그래서 어느 기기에서도 방 그림 좌우가 잘려도 되는 몫보다 많이 잘리지 않는다.
 */
fun Modifier.limitRoomCardHeight(): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val height = min(constraints.maxHeight, (constraints.maxWidth / RoomCardLayout.MinCardAspect).roundToInt())
    val placeable = measurable.measure(constraints.copy(minHeight = min(constraints.minHeight, height), maxHeight = height))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place((constraints.maxWidth - placeable.width) / 2, (constraints.maxHeight - placeable.height) / 2)
    }
}

/**
 * 홈 고양이방 카드 크기. 홈이 사용자가 보고 있는 화면일 때 카드를 그릴 때마다 적어 두고, 마이룸은 같은 크기로 방을 보여 준다.
 * 홈을 아직 그리지 않았으면(딥링크로 마이룸에 바로 들어옴) null이다.
 */
@Stable
class RoomCardSizeHolder {
    var size: DpSize? by mutableStateOf(null)
}

/** 앱 루트에서 하나를 공급해 홈과 마이룸이 같은 [RoomCardSizeHolder]를 본다. */
val LocalRoomCardSize = staticCompositionLocalOf { RoomCardSizeHolder() }
