package com.swm.dandi.sprite.entity

/**
 * 전체 스프라이트 시트에서 현재 frame 으로 사용할 원본 영역.
 *
 * [srcX], [srcY] 는 원본 이미지의 좌상단 기준 offset 이고, [widthPx], [heightPx] 는 잘라낼 크기다.
 * Compose Canvas 는 이 값을 `drawImage(srcOffset, srcSize)` 에 그대로 넘겨 해당 영역만 그린다.
 */
data class SpriteFrameVO(
    val index: Int = 0,
    val srcX: Int = 0,
    val srcY: Int = 0,
    val widthPx: Int = 1,
    val heightPx: Int = 1,
) {
    init {
        require(index >= 0) { "index must be greater than or equal to 0." }
        require(srcX >= 0) { "srcX must be greater than or equal to 0." }
        require(srcY >= 0) { "srcY must be greater than or equal to 0." }
        require(widthPx > 0) { "widthPx must be greater than 0." }
        require(heightPx > 0) { "heightPx must be greater than 0." }
    }

    companion object {
        val empty: SpriteFrameVO = SpriteFrameVO()
    }
}
