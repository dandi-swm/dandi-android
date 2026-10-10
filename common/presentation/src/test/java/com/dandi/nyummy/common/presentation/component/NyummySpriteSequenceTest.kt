package com.dandi.nyummy.common.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test

class NyummySpriteSequenceTest {

    /** 8프레임 × 100ms = 한 바퀴 800ms. */
    private fun clip(loop: Boolean = false) = NyummySpriteClip(
        sheet = NyummySpriteSheet(
            url = "https://cdn/a.png",
            frameWidth = 136,
            frameHeight = 136,
            totalFrames = 8,
            framesPerRow = 4,
            frameDurationMillis = 100,
        ),
        loop = loop,
    )

    @Test
    fun `중간의 되풀이 구간은 정해진 횟수만큼, 나머지는 한 번씩 재생하고 마지막 프레임에 멈춰 쉰다`() {
        val timeline = spriteTimeline(listOf(clip(), clip(loop = true), clip()), loopTimes = 3, restMillis = 1_000L)

        assertEquals(listOf(SpriteStep(0, 1), SpriteStep(1, 3), SpriteStep(2, 1)), timeline.steps)
        assertEquals(1_000L, timeline.holdMillis)
        assertEquals(800L + 2_400L + 800L + 1_000L, timeline.totalMillis)
    }

    @Test
    fun `마지막 클립이 되풀이 구간이면 쉬는 시간을 채울 때까지 한 바퀴씩 더 돈다`() {
        val timeline = spriteTimeline(listOf(clip(), clip(loop = true)), loopTimes = 3, restMillis = 2_000L)

        // 한 번 재생 + 쉬는 2000ms 동안 800ms씩 세 바퀴
        assertEquals(listOf(SpriteStep(0, 1), SpriteStep(1, 4)), timeline.steps)
        assertEquals(0L, timeline.holdMillis)
        assertEquals(800L + 3_200L, timeline.totalMillis)
    }

    @Test
    fun `쉬는 시간이 없으면 마지막 되풀이 구간도 한 번만 재생한다`() {
        val timeline = spriteTimeline(listOf(clip(loop = true)), loopTimes = 3, restMillis = 0L)

        assertEquals(listOf(SpriteStep(0, 1)), timeline.steps)
    }

    @Test
    fun `되풀이 횟수가 0 이하이면 한 번은 재생한다`() {
        val timeline = spriteTimeline(listOf(clip(loop = true), clip()), loopTimes = 0, restMillis = 1_000L)

        assertEquals(SpriteStep(0, 1), timeline.steps.first())
    }

    @Test
    fun `프레임은 칸 폭에 가장 가까운 정수배로 키우고, 최소 1배다`() {
        assertEquals(3, spritePixelScale(framePx = 136, availablePx = 407f))
        assertEquals(2, spritePixelScale(framePx = 136, availablePx = 315f))
        assertEquals(1, spritePixelScale(framePx = 136, availablePx = 40f))
        assertEquals(1, spritePixelScale(framePx = 0, availablePx = 400f))
    }
}
