package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.runtime.Immutable

/**
 * 스프라이트 시트 한 장.
 *
 * @property url 시트 이미지 주소
 * @property frames 시트에 든 프레임 수
 * @property loop 되풀이해도 되는 구간인지
 */
@Immutable
data class NyummySpriteClip(
    val url: String,
    val frames: Int,
    val loop: Boolean = false,
)

/**
 * 시트 한 칸의 규격. 프레임은 왼쪽 위부터 행 순서로 놓여 있다.
 *
 * @property width 한 프레임의 가로 픽셀
 * @property height 한 프레임의 세로 픽셀
 * @property framesPerRow 시트 한 줄의 프레임 수
 * @property durationMs 한 프레임을 보여 주는 시간
 */
@Immutable
data class NyummySpriteFrame(
    val width: Int,
    val height: Int,
    val framesPerRow: Int,
    val durationMs: Int,
)

/** 클립 [clipIndex]를 [times]번 재생하는 한 단계. */
internal data class SpriteStep(val clipIndex: Int, val times: Int)

/**
 * 클립 목록을 재생 단계로 펼친다.
 * 되풀이 구간(loop)은 [loopTimes]번 재생하고, 나머지는 한 번 재생한다.
 * 마지막 클립이 되풀이 구간이면 한 번만 넣는다. 이후 쉬는 동안 계속 되풀이하는 것은 [NyummySpriteAnimation]이 맡는다.
 */
internal fun spriteSteps(clips: List<NyummySpriteClip>, loopTimes: Int): List<SpriteStep> =
    clips.mapIndexed { index, clip ->
        val isLast = index == clips.lastIndex
        SpriteStep(clipIndex = index, times = if (clip.loop && !isLast) loopTimes.coerceAtLeast(1) else 1)
    }

/**
 * 셀을 화면에 정수배로 키웠을 때의 배율. 픽셀이 고르게 보이도록 [availablePx]에 가장 가까운 정수배를 고르고, 최소 1배다.
 * 예: 셀 136px을 407px 칸에 그리면 3배(408px)다.
 */
internal fun spritePixelScale(cellPx: Int, availablePx: Float): Int {
    if (cellPx <= 0) return 1
    return Math.round(availablePx / cellPx).coerceAtLeast(1)
}
