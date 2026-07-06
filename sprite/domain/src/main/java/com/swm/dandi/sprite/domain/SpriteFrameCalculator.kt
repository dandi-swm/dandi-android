package com.swm.dandi.sprite.domain

import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.entity.SpriteFrameVO
import com.swm.dandi.sprite.entity.SpriteVO

/**
 * 스프라이트 시트의 frame index 를 원본 이미지 좌표로 변환하는 순수 계산기.
 *
 * Android/Compose 의존 없이 동작하며, presentation 은 계산 결과([SpriteFrameVO])만 받아
 * Canvas 에 그린다. 잘못된 frame index 나 spec 은 복구 가능한 사용자 오류가 아니라
 * 잘못된 개발 입력으로 보고 즉시 실패시킨다.
 */
object SpriteFrameCalculator {
    /**
     * [frameIndex] 에 해당하는 원본 시트 영역을 계산한다.
     *
     * [SpriteVO.alignElement] 에 따라 Grid / BinaryTree / LeftRight / TopDown 좌표 계산을 분기한다.
     * @return Canvas 의 `drawImage(srcOffset, srcSize)` 에 그대로 사용할 frame 영역.
     */
    fun frameAt(
        spec: SpriteVO,
        frameIndex: Int,
    ): SpriteFrameVO {
        require(frameIndex in 0 until spec.totalFrames) {
            "frameIndex must be in 0 until totalFrames."
        }

        val position = when (spec.alignElement) {
            SpriteAlignElement.Grid -> gridLayoutPosition(spec, frameIndex)
            SpriteAlignElement.LeftRight -> leftRightLayoutPosition(spec, frameIndex)
            SpriteAlignElement.TopDown -> topDownLayoutPosition(spec, frameIndex)
            SpriteAlignElement.BinaryTree -> binaryTreeLayoutPosition(spec, frameIndex)
        }

        return SpriteFrameVO(
            index = frameIndex,
            srcX = position.x,
            srcY = position.y,
            widthPx = spec.frameWidthPx,
            heightPx = spec.frameHeightPx,
        )
    }

    /**
     * 현재 frame 이후에 보여줄 frame index 를 계산한다.
     *
     * 마지막 frame 에 도달했을 때 [SpriteVO.loop] 가 true 면 [SpriteVO.startFrame] 으로 돌아가고,
     * loop 가 꺼져 있으면 현재 frame 에 머무른다.
     */
    fun nextFrame(
        spec: SpriteVO,
        currentFrame: Int,
    ): Int {
        require(currentFrame in 0 until spec.totalFrames) {
            "currentFrame must be in 0 until totalFrames."
        }

        val next = currentFrame + 1
        return when {
            next < spec.totalFrames -> next
            spec.loop -> spec.startFrame
            else -> currentFrame
        }
    }

    /**
     * 현재 frame 이전에 보여줄 frame index 를 계산한다.
     *
     * [SpriteVO.startFrame] 보다 앞의 frame 으로는 내려가지 않는다. loop 가 true 면
     * 마지막 frame 으로 순환하고, loop 가 꺼져 있으면 현재 frame 에 머무른다.
     */
    fun previousFrame(
        spec: SpriteVO,
        currentFrame: Int,
    ): Int {
        require(currentFrame in 0 until spec.totalFrames) {
            "currentFrame must be in 0 until totalFrames."
        }

        val previous = currentFrame - 1
        return when {
            previous >= spec.startFrame -> previous
            spec.loop -> spec.totalFrames - 1
            else -> currentFrame
        }
    }

    private fun leftRightLayoutPosition(
        spec: SpriteVO,
        frameIndex: Int,
    ): FramePosition = FramePosition(
        x = spec.paddingPx + frameIndex * (spec.frameWidthPx + spec.gapPx),
        y = spec.paddingPx,
    )

    private fun topDownLayoutPosition(
        spec: SpriteVO,
        frameIndex: Int,
    ): FramePosition = FramePosition(
        x = spec.paddingPx,
        y = spec.paddingPx + frameIndex * (spec.frameHeightPx + spec.gapPx),
    )

    private fun gridLayoutPosition(
        spec: SpriteVO,
        frameIndex: Int,
    ): FramePosition {
        val column = frameIndex % spec.framesPerRow
        val row = frameIndex / spec.framesPerRow
        return FramePosition(
            x = spec.paddingPx + column * (spec.frameWidthPx + spec.gapPx),
            y = spec.paddingPx + row * (spec.frameHeightPx + spec.gapPx),
        )
    }

    private fun binaryTreeLayoutPosition(
        spec: SpriteVO,
        frameIndex: Int,
    ): FramePosition {
        val outerWidth = spec.frameWidthPx + spec.gapPx
        val outerHeight = spec.frameHeightPx + spec.gapPx
        var root = BinaryNode(
            x = 0,
            y = 0,
            width = outerWidth,
            height = outerHeight,
        )
        var position = FramePosition(x = 0, y = 0)

        // Binary tree packing 은 앞 프레임들의 배치 결과가 다음 프레임 좌표를 결정한다.
        // 따라서 target index 만 바로 계산하지 않고 0번부터 target index 까지 같은 순서로
        // 재생한다.
        repeat(frameIndex + 1) {
            val node = findBinaryNode(root, outerWidth, outerHeight)
                ?: growBinaryNode(root, outerWidth, outerHeight).also { grownRoot ->
                    root = grownRoot
                }.let { grownRoot ->
                    findBinaryNode(grownRoot, outerWidth, outerHeight)
                }

            checkNotNull(node) {
                "Binary tree layout could not place sprite frame."
            }

            val fit = splitBinaryNode(node, outerWidth, outerHeight)
            position = FramePosition(
                x = spec.paddingPx + fit.x,
                y = spec.paddingPx + fit.y,
            )
        }

        return position
    }

    private fun findBinaryNode(
        node: BinaryNode?,
        width: Int,
        height: Int,
    ): BinaryNode? {
        if (node == null) return null

        if (node.used) {
            return findBinaryNode(node.right, width, height)
                ?: findBinaryNode(node.down, width, height)
        }

        return if (width <= node.width && height <= node.height) {
            node
        } else {
            null
        }
    }

    private fun splitBinaryNode(
        node: BinaryNode,
        width: Int,
        height: Int,
    ): BinaryNode {
        // 현재 node 를 frame 한 칸으로 사용하고, 남은 공간을 오른쪽/아래쪽 후보 node 로
        // 나눈다.
        node.used = true
        node.down = BinaryNode(
            x = node.x,
            y = node.y + height,
            width = node.width,
            height = node.height - height,
        )
        node.right = BinaryNode(
            x = node.x + width,
            y = node.y,
            width = node.width - width,
            height = height,
        )
        return node
    }

    private fun growBinaryNode(
        root: BinaryNode,
        width: Int,
        height: Int,
    ): BinaryNode {
        val canGrowDown = width <= root.width
        val canGrowRight = height <= root.height
        val shouldGrowRight = canGrowRight && root.height >= root.width + width
        val shouldGrowDown = canGrowDown && root.width >= root.height + height

        // sprite-creator 와 같은 휴리스틱: 가능하면 더 짧은 축을 확장해 시트가
        // 한쪽으로 길어지는 것을 줄인다.
        return when {
            shouldGrowRight -> growBinaryRight(root, width)
            shouldGrowDown -> growBinaryDown(root, height)
            canGrowRight -> growBinaryRight(root, width)
            canGrowDown -> growBinaryDown(root, height)
            else -> error("Binary tree layout could not grow sprite sheet.")
        }
    }

    private fun growBinaryRight(
        root: BinaryNode,
        width: Int,
    ): BinaryNode = BinaryNode(
        used = true,
        x = 0,
        y = 0,
        width = root.width + width,
        height = root.height,
        down = root,
        right = BinaryNode(
            x = root.width,
            y = 0,
            width = width,
            height = root.height,
        ),
    )

    private fun growBinaryDown(
        root: BinaryNode,
        height: Int,
    ): BinaryNode = BinaryNode(
        used = true,
        x = 0,
        y = 0,
        width = root.width,
        height = root.height + height,
        down = BinaryNode(
            x = 0,
            y = root.height,
            width = root.width,
            height = height,
        ),
        right = root,
    )

    private data class FramePosition(
        val x: Int,
        val y: Int,
    )

    private data class BinaryNode(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        var used: Boolean = false,
        var right: BinaryNode? = null,
        var down: BinaryNode? = null,
    )
}
