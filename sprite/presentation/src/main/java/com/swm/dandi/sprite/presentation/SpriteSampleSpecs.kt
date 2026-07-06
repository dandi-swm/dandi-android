package com.swm.dandi.sprite.presentation

import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.entity.SpriteVO

enum class SpriteSampleAnimation(
    val labelRes: Int,
) {
    Hello(R.string.sprite_sample_animation_hello),
    Eat(R.string.sprite_sample_animation_eat),
    Sleep(R.string.sprite_sample_animation_sleep),
}

val SpriteAlignElement.labelRes: Int
    get() = when (this) {
        SpriteAlignElement.Grid -> R.string.sprite_sample_align_grid
        SpriteAlignElement.BinaryTree -> R.string.sprite_sample_align_binary_tree
        SpriteAlignElement.LeftRight -> R.string.sprite_sample_align_left_right
        SpriteAlignElement.TopDown -> R.string.sprite_sample_align_top_down
    }

val SpriteSampleAnimation.spec: SpriteVO
    get() = spec(SpriteAlignElement.Grid)

/**
 * sample 화면에서 선택한 애니메이션/배치 방식에 맞는 [SpriteVO] 를 반환한다.
 *
 * 같은 애니메이션이라도 배치 방식별로 서로 다른 이미지 리소스를 가진다.
 * frame 크기와 재생 속도는 sprite-creator 가 생성한 sample asset 기준으로 통일한다.
 */
fun SpriteSampleAnimation.spec(alignElement: SpriteAlignElement): SpriteVO =
    SpriteSampleSpecs.spec(
        animation = this,
        alignElement = alignElement,
    )

internal object SpriteSampleSpecs {
    private const val FrameSizePx = 136
    private const val FrameDurationMillis = 140L
    private const val GridFramesPerRow = 4

    fun spec(
        animation: SpriteSampleAnimation,
        alignElement: SpriteAlignElement,
    ): SpriteVO = when (animation) {
        SpriteSampleAnimation.Hello -> hello(alignElement)
        SpriteSampleAnimation.Eat -> eat(alignElement)
        SpriteSampleAnimation.Sleep -> sleep(alignElement)
    }

    private fun hello(alignElement: SpriteAlignElement): SpriteVO = sample(
        spriteSheetRes = when (alignElement) {
            SpriteAlignElement.Grid -> R.drawable.sample_cat_grid_hello_sprite_sheet
            SpriteAlignElement.BinaryTree -> R.drawable.sample_cat_binary_hello_sprite_sheet
            SpriteAlignElement.LeftRight -> R.drawable.sample_cat_left_right_hello_sprite_sheet
            SpriteAlignElement.TopDown -> R.drawable.sample_cat_top_down_hello_sprite_sheet
        },
        totalFrames = 17,
        alignElement = alignElement,
    )

    private fun eat(alignElement: SpriteAlignElement): SpriteVO = sample(
        spriteSheetRes = when (alignElement) {
            SpriteAlignElement.Grid -> R.drawable.sample_cat_grid_eat_sprite_sheet
            SpriteAlignElement.BinaryTree -> R.drawable.sample_cat_binary_eat_sprite_sheet
            SpriteAlignElement.LeftRight -> R.drawable.sample_cat_left_right_eat_sprite_sheet
            SpriteAlignElement.TopDown -> R.drawable.sample_cat_top_down_eat_sprite_sheet
        },
        totalFrames = 15,
        alignElement = alignElement,
    )

    private fun sleep(alignElement: SpriteAlignElement): SpriteVO = sample(
        spriteSheetRes = when (alignElement) {
            SpriteAlignElement.Grid -> R.drawable.sample_cat_grid_sleep_sprite_sheet
            SpriteAlignElement.BinaryTree -> R.drawable.sample_cat_binary_sleep_sprite_sheet
            SpriteAlignElement.LeftRight -> R.drawable.sample_cat_left_right_sleep_sprite_sheet
            SpriteAlignElement.TopDown -> R.drawable.sample_cat_top_down_sleep_sprite_sheet
        },
        totalFrames = 17,
        alignElement = alignElement,
    )

    private fun sample(
        spriteSheetRes: Int,
        totalFrames: Int,
        alignElement: SpriteAlignElement,
    ): SpriteVO = SpriteVO(
        spriteSheetRes = spriteSheetRes,
        totalFrames = totalFrames,
        framesPerRow = GridFramesPerRow,
        frameWidthPx = FrameSizePx,
        frameHeightPx = FrameSizePx,
        alignElement = alignElement,
        frameDurationMillis = FrameDurationMillis,
        loop = true,
    )
}
