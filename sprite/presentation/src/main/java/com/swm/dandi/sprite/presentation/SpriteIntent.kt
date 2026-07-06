package com.swm.dandi.sprite.presentation

import com.swm.dandi.common.presentation.mvi.MviIntent
import com.swm.dandi.sprite.entity.SpriteAlignElement

sealed interface SpriteIntent : MviIntent {
    data object Load : SpriteIntent
    data object TogglePlayback : SpriteIntent
    data object PreviousFrame : SpriteIntent
    data object NextFrame : SpriteIntent
    data class SelectAnimation(val animation: SpriteSampleAnimation) : SpriteIntent
    data class SelectAlignElement(val alignElement: SpriteAlignElement) : SpriteIntent
    data class FrameDurationChanged(val frameDurationMillis: Long) : SpriteIntent
    data class FrameChanged(val frameIndex: Int) : SpriteIntent
    data class LoopChanged(val loop: Boolean) : SpriteIntent
}
