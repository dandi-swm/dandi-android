package com.dandi.nyummy.common.presentation.cutscene

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * 만화 페이지 칸 나누기. 페이지는 캔버스에서 바깥 여백을 뺀 영역이고, 칸은 겹치지 않고 페이지를 빈틈없이 나눈다.
 * 칸 사이 홈은 플레이어가 칸을 안쪽으로 줄여 그려 만든다.
 *
 * 가로 경계([Seam])는 왼쪽 끝과 오른쪽 끝 높이가 달라 비스듬할 수 있고, 줄 안의 세로 경계([Split])도
 * 위 끝과 아래 끝 x가 달라 비스듬할 수 있다. 줄마다 기울기를 엇갈리면 만화 페이지처럼 보인다.
 */
object CutscenePage {
    const val LEFT = 6f
    const val TOP = 6f
    const val RIGHT = CutsceneCanvas.WIDTH - 6f
    const val BOTTOM = CutsceneCanvas.HEIGHT - 6f

    /** 페이지 위 끝. */
    val Top = Seam(TOP, TOP)

    /** 페이지 아래 끝. */
    val Bottom = Seam(BOTTOM, BOTTOM)

    /** 가로 경계. 페이지 왼쪽 끝 높이 [left] 와 오른쪽 끝 높이 [right] 를 잇는 선이다. */
    @Immutable
    data class Seam(val left: Float, val right: Float) {
        fun yAt(x: Float): Float = left + (right - left) * (x - LEFT) / (RIGHT - LEFT)
    }

    /** 줄 안의 세로 경계. 위 경계에서 x = [top], 아래 경계에서 x = [bottom] 인 선이다. */
    @Immutable
    data class Split(val top: Float, val bottom: Float)

    /** 두 가로 경계 사이 한 줄 전체. */
    fun row(top: Seam, bottom: Seam): CutsceneShape = between(top, bottom, LEFT, LEFT, RIGHT, RIGHT)

    /** 두 가로 경계 사이 줄을 [split] 으로 나눈 왼쪽 칸. */
    fun left(top: Seam, bottom: Seam, split: Split): CutsceneShape = between(top, bottom, LEFT, LEFT, split.top, split.bottom)

    /** 두 가로 경계 사이 줄을 [split] 으로 나눈 오른쪽 칸. */
    fun right(top: Seam, bottom: Seam, split: Split): CutsceneShape = between(top, bottom, split.top, split.bottom, RIGHT, RIGHT)

    private fun between(top: Seam, bottom: Seam, topLeft: Float, bottomLeft: Float, topRight: Float, bottomRight: Float) =
        CutsceneShape(
            listOf(
                Offset(topLeft, top.yAt(topLeft)),
                Offset(topRight, top.yAt(topRight)),
                Offset(bottomRight, bottom.yAt(bottomRight)),
                Offset(bottomLeft, bottom.yAt(bottomLeft)),
            ),
        )
}
