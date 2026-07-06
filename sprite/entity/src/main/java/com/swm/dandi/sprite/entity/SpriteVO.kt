package com.swm.dandi.sprite.entity

/**
 * 스프라이트 시트 한 장을 재생하기 위한 렌더링 명세.
 *
 * 좌표와 크기는 모두 원본 이미지의 pixel 단위다. presentation 은 이 명세와 현재 frame index 를
 * [com.swm.dandi.sprite.domain.SpriteFrameCalculator] 에 넘겨 원본 시트에서 잘라낼 영역을 얻는다.
 *
 * - [spriteSheetRes]: `drawable-nodpi` 등에 저장된 전체 스프라이트 시트 리소스.
 * - [totalFrames]: 실제 애니메이션에 사용하는 프레임 수. 마지막 줄의 빈 칸은
 *   포함하지 않는다.
 * - [framesPerRow]: [SpriteAlignElement.Grid] 에서 한 행에 배치된 프레임 수.
 * - [frameWidthPx], [frameHeightPx]: 한 프레임의 고정 크기.
 * - [gapPx], [paddingPx]: 프레임 사이 간격과 시트 바깥 여백.
 * - [frameDurationMillis]: 다음 프레임으로 넘어가기 전 대기 시간.
 */
data class SpriteVO(
    val spriteSheetRes: Int = 0,
    val totalFrames: Int = 1,
    val framesPerRow: Int = 1,
    val frameWidthPx: Int = 1,
    val frameHeightPx: Int = 1,
    val alignElement: SpriteAlignElement = SpriteAlignElement.Grid,
    val gapPx: Int = 0,
    val paddingPx: Int = 0,
    val frameDurationMillis: Long = 100L,
    val startFrame: Int = 0,
    val loop: Boolean = false,
) {
    init {
        require(totalFrames > 0) { "totalFrames must be greater than 0." }
        require(framesPerRow > 0) { "framesPerRow must be greater than 0." }
        require(frameWidthPx > 0) { "frameWidthPx must be greater than 0." }
        require(frameHeightPx > 0) { "frameHeightPx must be greater than 0." }
        require(gapPx >= 0) { "gapPx must be greater than or equal to 0." }
        require(paddingPx >= 0) { "paddingPx must be greater than or equal to 0." }
        require(frameDurationMillis > 0L) { "frameDurationMillis must be greater than 0." }
        require(startFrame in 0 until totalFrames) {
            "startFrame must be in 0 until totalFrames."
        }
    }

    companion object {
        val empty: SpriteVO = SpriteVO()
    }
}
