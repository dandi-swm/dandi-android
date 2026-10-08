package com.dandi.nyummy.common.presentation.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class NyummySpriteTimelineTest {

    private fun clip(loop: Boolean = false) = NyummySpriteClip(url = "https://cdn/a.png", frames = 8, loop = loop)

    @Test
    fun `중간의 되풀이 구간은 정해진 횟수만큼, 나머지는 한 번씩 재생한다`() {
        val steps = spriteSteps(listOf(clip(), clip(loop = true), clip()), loopTimes = 3)

        assertEquals(listOf(SpriteStep(0, 1), SpriteStep(1, 3), SpriteStep(2, 1)), steps)
    }

    @Test
    fun `마지막 클립이 되풀이 구간이면 한 번만 넣는다(쉬는 동안 이어서 되풀이한다)`() {
        val steps = spriteSteps(listOf(clip(), clip(loop = true)), loopTimes = 3)

        assertEquals(listOf(SpriteStep(0, 1), SpriteStep(1, 1)), steps)
    }

    @Test
    fun `되풀이 횟수가 0 이하이면 한 번은 재생한다`() {
        val steps = spriteSteps(listOf(clip(loop = true), clip()), loopTimes = 0)

        assertEquals(SpriteStep(0, 1), steps.first())
    }

    @Test
    fun `셀은 칸 폭에 가장 가까운 정수배로 키우고, 최소 1배다`() {
        assertEquals(3, spritePixelScale(cellPx = 136, availablePx = 407f))
        assertEquals(2, spritePixelScale(cellPx = 136, availablePx = 315f))
        assertEquals(1, spritePixelScale(cellPx = 136, availablePx = 40f))
        assertEquals(1, spritePixelScale(cellPx = 0, availablePx = 400f))
    }
}
