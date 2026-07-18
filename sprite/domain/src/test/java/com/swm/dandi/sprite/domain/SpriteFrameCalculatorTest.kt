package com.swm.dandi.sprite.domain

import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.entity.SpriteFrameVO
import com.swm.dandi.sprite.entity.SpriteVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpriteFrameCalculatorTest {
    @Test
    fun frameAtReturnsGridFrameByDefault() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 8,
            framesPerRow = 4,
            frameWidthPx = 16,
            frameHeightPx = 24,
        )

        assertEquals(
            SpriteFrameVO(
                index = 6,
                srcX = 32,
                srcY = 24,
                widthPx = 16,
                heightPx = 24,
            ),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 6),
        )
    }

    @Test
    fun leftRightAlignElementReturnsOneRowLayout() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 4,
            frameWidthPx = 10,
            frameHeightPx = 10,
            alignElement = SpriteAlignElement.LeftRight,
            gapPx = 2,
            paddingPx = 1,
        )

        assertEquals(
            SpriteFrameVO(index = 0, srcX = 1, srcY = 1, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 0),
        )
        assertEquals(
            SpriteFrameVO(index = 2, srcX = 25, srcY = 1, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 2),
        )
    }

    @Test
    fun topDownAlignElementReturnsOneColumnLayout() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 4,
            frameWidthPx = 10,
            frameHeightPx = 10,
            alignElement = SpriteAlignElement.TopDown,
            gapPx = 2,
            paddingPx = 1,
        )

        assertEquals(
            SpriteFrameVO(index = 0, srcX = 1, srcY = 1, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 0),
        )
        assertEquals(
            SpriteFrameVO(index = 2, srcX = 1, srcY = 25, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 2),
        )
    }

    @Test
    fun gridAlignElementReturnsRequestedColumnLayout() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 17,
            framesPerRow = 5,
            frameWidthPx = 136,
            frameHeightPx = 136,
            alignElement = SpriteAlignElement.Grid,
        )

        assertEquals(
            SpriteFrameVO(index = 0, srcX = 0, srcY = 0, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 0),
        )
        assertEquals(
            SpriteFrameVO(index = 4, srcX = 544, srcY = 0, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 4),
        )
        assertEquals(
            SpriteFrameVO(index = 16, srcX = 136, srcY = 408, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 16),
        )
    }

    @Test
    fun binaryTreeAlignElementReturnsPackedLayout() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 17,
            frameWidthPx = 136,
            frameHeightPx = 136,
            alignElement = SpriteAlignElement.BinaryTree,
        )

        assertEquals(
            SpriteFrameVO(index = 0, srcX = 0, srcY = 0, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 0),
        )
        assertEquals(
            SpriteFrameVO(index = 4, srcX = 272, srcY = 0, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 4),
        )
        assertEquals(
            SpriteFrameVO(index = 16, srcX = 544, srcY = 0, widthPx = 136, heightPx = 136),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 16),
        )
    }

    @Test
    fun binaryTreeAlignElementAppliesGapAndPadding() {
        val spec = SpriteVO(
            spriteSheetRes = 1,
            totalFrames = 3,
            frameWidthPx = 10,
            frameHeightPx = 10,
            alignElement = SpriteAlignElement.BinaryTree,
            gapPx = 2,
            paddingPx = 1,
        )

        assertEquals(
            SpriteFrameVO(index = 0, srcX = 1, srcY = 1, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 0),
        )
        assertEquals(
            SpriteFrameVO(index = 1, srcX = 13, srcY = 1, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 1),
        )
        assertEquals(
            SpriteFrameVO(index = 2, srcX = 1, srcY = 13, widthPx = 10, heightPx = 10),
            SpriteFrameCalculator.frameAt(spec, frameIndex = 2),
        )
    }

    @Test
    fun nextFrameLoopsWhenSpecLoops() {
        val spec = SpriteVO(totalFrames = 3, loop = true)

        assertEquals(1, SpriteFrameCalculator.nextFrame(spec, currentFrame = 0))
        assertEquals(0, SpriteFrameCalculator.nextFrame(spec, currentFrame = 2))
    }

    @Test
    fun nextFrameLoopsToStartFrameWhenStartFrameIsNotZero() {
        val spec = SpriteVO(totalFrames = 5, startFrame = 2, loop = true)

        assertEquals(3, SpriteFrameCalculator.nextFrame(spec, currentFrame = 2))
        assertEquals(2, SpriteFrameCalculator.nextFrame(spec, currentFrame = 4))
    }

    @Test
    fun nextFrameStopsAtLastFrameWhenSpecDoesNotLoop() {
        val spec = SpriteVO(totalFrames = 3, loop = false)

        assertEquals(1, SpriteFrameCalculator.nextFrame(spec, currentFrame = 0))
        assertEquals(2, SpriteFrameCalculator.nextFrame(spec, currentFrame = 2))
    }

    @Test
    fun previousFrameLoopsToLastFrameWhenSpecLoops() {
        val spec = SpriteVO(totalFrames = 3, loop = true)

        assertEquals(1, SpriteFrameCalculator.previousFrame(spec, currentFrame = 2))
        assertEquals(2, SpriteFrameCalculator.previousFrame(spec, currentFrame = 0))
    }

    @Test
    fun previousFrameStopsAtStartFrameWhenSpecDoesNotLoop() {
        val spec = SpriteVO(totalFrames = 5, startFrame = 2, loop = false)

        assertEquals(2, SpriteFrameCalculator.previousFrame(spec, currentFrame = 2))
        assertEquals(3, SpriteFrameCalculator.previousFrame(spec, currentFrame = 4))
    }

    @Test
    fun specRejectsInvalidAnimationValues() {
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(totalFrames = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(framesPerRow = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(frameWidthPx = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(frameHeightPx = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(gapPx = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(paddingPx = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(frameDurationMillis = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteVO(totalFrames = 3, startFrame = 3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpriteFrameVO(widthPx = 0)
        }
    }
}
