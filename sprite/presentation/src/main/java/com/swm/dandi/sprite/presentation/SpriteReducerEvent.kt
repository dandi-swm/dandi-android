package com.swm.dandi.sprite.presentation

import com.swm.dandi.common.presentation.mvi.ReducerEvent
import com.swm.dandi.sprite.entity.SpriteAlignElement

sealed interface SpriteReducerEvent : ReducerEvent {
    data object Ready : SpriteReducerEvent
    data object PlaybackToggled : SpriteReducerEvent
    data object PreviousFrameRequested : SpriteReducerEvent
    data object NextFrameRequested : SpriteReducerEvent
    data class AnimationSelected(val animation: SpriteSampleAnimation) : SpriteReducerEvent
    data class AlignElementSelected(val alignElement: SpriteAlignElement) : SpriteReducerEvent
    data class FrameDurationChanged(val frameDurationMillis: Long) : SpriteReducerEvent
    data class FrameChanged(val frameIndex: Int) : SpriteReducerEvent
    data class LoopChanged(val loop: Boolean) : SpriteReducerEvent
}
