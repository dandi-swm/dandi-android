package com.swm.dandi.sprite.presentation

import com.swm.dandi.common.presentation.mvi.UiState
import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.entity.SpriteVO

/**
 * SpritePage sample 화면의 단일 UI 상태.
 *
 * [selectedSpec] 는 선택된 sample animation 과 배치 방식에 slider 의 frame duration 을 합성한 값이다.
 * 실제 캐릭터 상태가 아니라 sprite 렌더러 사용법을 확인하기 위한 demo state 만 담는다.
 */
data class SpriteUIState(
    val isLoading: Boolean = true,
    val selectedAnimation: SpriteSampleAnimation = SpriteSampleAnimation.Hello,
    val selectedAlignElement: SpriteAlignElement = SpriteAlignElement.Grid,
    val currentFrame: Int = SpriteSampleAnimation.Hello.spec(SpriteAlignElement.Grid).startFrame,
    val frameDurationMillis: Long = SpriteSampleAnimation.Hello
        .spec(SpriteAlignElement.Grid)
        .frameDurationMillis,
    val isPlaying: Boolean = true,
    val loop: Boolean = true,
) : UiState {
    val selectedSpec: SpriteVO
        get() = selectedAnimation
            .spec(selectedAlignElement)
            .copy(frameDurationMillis = frameDurationMillis)

    companion object {
        val empty: SpriteUIState = SpriteUIState()
    }
}
