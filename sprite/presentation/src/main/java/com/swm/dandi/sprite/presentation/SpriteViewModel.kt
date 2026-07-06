package com.swm.dandi.sprite.presentation

import com.swm.dandi.common.presentation.mvi.MviViewModel
import com.swm.dandi.sprite.domain.SpriteFrameCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SpriteViewModel @Inject constructor() :
    MviViewModel<SpriteIntent, SpriteUIState, SpriteReducerEvent>(SpriteUIState.empty) {

    init {
        onIntent(SpriteIntent.Load)
    }

    override fun onIntent(intent: SpriteIntent) {
        when (intent) {
            SpriteIntent.Load -> dispatch(SpriteReducerEvent.Ready)
            SpriteIntent.TogglePlayback -> dispatch(SpriteReducerEvent.PlaybackToggled)
            SpriteIntent.PreviousFrame -> dispatch(SpriteReducerEvent.PreviousFrameRequested)
            SpriteIntent.NextFrame -> dispatch(SpriteReducerEvent.NextFrameRequested)
            is SpriteIntent.SelectAnimation -> dispatch(
                SpriteReducerEvent.AnimationSelected(intent.animation),
            )

            is SpriteIntent.SelectAlignElement -> dispatch(
                SpriteReducerEvent.AlignElementSelected(intent.alignElement),
            )

            is SpriteIntent.FrameDurationChanged -> dispatch(
                SpriteReducerEvent.FrameDurationChanged(intent.frameDurationMillis),
            )

            is SpriteIntent.FrameChanged -> dispatch(
                SpriteReducerEvent.FrameChanged(intent.frameIndex),
            )

            is SpriteIntent.LoopChanged -> dispatch(SpriteReducerEvent.LoopChanged(intent.loop))
        }
    }

    override fun reduce(
        state: SpriteUIState,
        event: SpriteReducerEvent,
    ): SpriteUIState = when (event) {
        SpriteReducerEvent.Ready -> state.copy(isLoading = false)

        SpriteReducerEvent.PlaybackToggled -> state.copy(isPlaying = !state.isPlaying)
        SpriteReducerEvent.PreviousFrameRequested -> {
            val spec = state.selectedSpec.copy(loop = state.loop)
            state.copy(
                currentFrame = SpriteFrameCalculator.previousFrame(
                    spec = spec,
                    currentFrame = state.currentFrame,
                ),
                isPlaying = false,
            )
        }

        SpriteReducerEvent.NextFrameRequested -> {
            val spec = state.selectedSpec.copy(loop = state.loop)
            state.copy(
                currentFrame = SpriteFrameCalculator.nextFrame(
                    spec = spec,
                    currentFrame = state.currentFrame,
                ),
                isPlaying = false,
            )
        }

        is SpriteReducerEvent.AnimationSelected -> state.copy(
            selectedAnimation = event.animation,
            currentFrame = event.animation.spec(state.selectedAlignElement).startFrame,
            isPlaying = true,
        )

        is SpriteReducerEvent.AlignElementSelected -> {
            val spec = state.selectedAnimation.spec(event.alignElement)
            state.copy(
                selectedAlignElement = event.alignElement,
                currentFrame = state.currentFrame.coerceIn(
                    minimumValue = 0,
                    maximumValue = spec.totalFrames - 1,
                ),
            )
        }

        is SpriteReducerEvent.FrameChanged -> state.copy(
            currentFrame = event.frameIndex.coerceIn(
                minimumValue = 0,
                maximumValue = state.selectedSpec.totalFrames - 1,
            ),
        )

        is SpriteReducerEvent.FrameDurationChanged -> state.copy(
            frameDurationMillis = event.frameDurationMillis,
        )

        is SpriteReducerEvent.LoopChanged -> state.copy(loop = event.loop)
    }
}
