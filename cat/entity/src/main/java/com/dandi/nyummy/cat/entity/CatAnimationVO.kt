package com.dandi.nyummy.cat.entity

/**
 * 내 고양이의 상태별 애니메이션 묶음. 체형마다 스프라이트가 다르고, 상태 이름과 대사는 같다.
 *
 * @property weight 체형 이름(LEAN, SLIM, NORMAL, CHUBBY, PLUMP)
 * @property animations 상태별 애니메이션. 앱이 모르는 상태는 들어 있지 않다.
 */
data class CatAnimationSetVO(
    val weight: String = "",
    val animations: Map<CatState, CatAnimationVO> = emptyMap(),
) {
    /** [state]의 애니메이션. 없으면 기본 인사 상태([CatState.FRIENDLY])로 대신하고, 그것도 없으면 null이다. */
    fun animationFor(state: CatState): CatAnimationVO? =
        animations[state] ?: animations[CatState.FRIENDLY]

    companion object {
        val empty = CatAnimationSetVO()
    }
}

/**
 * 한 상태의 애니메이션.
 *
 * @property frame 이 상태의 모든 스프라이트 시트가 공유하는 프레임 규격
 * @property groups 동작 묶음 목록. 하나를 골라 그 안의 클립을 순서대로 재생한다.
 * @property lines 이 상태에서 냐미가 할 수 있는 대사
 */
data class CatAnimationVO(
    val state: CatState,
    val frame: SpriteFrameVO,
    val groups: List<List<SpriteClipVO>>,
    val lines: List<String>,
)

/**
 * 스프라이트 시트 한 칸의 규격. 프레임은 왼쪽 위부터 행 순서로 놓여 있다.
 *
 * @property width 한 프레임의 가로 픽셀
 * @property height 한 프레임의 세로 픽셀
 * @property framesPerRow 시트 한 줄에 놓인 프레임 수
 * @property durationMs 한 프레임을 보여 주는 시간
 */
data class SpriteFrameVO(
    val width: Int,
    val height: Int,
    val framesPerRow: Int,
    val durationMs: Int,
)

/**
 * 스프라이트 시트 한 장.
 *
 * @property url 시트 이미지 주소
 * @property frames 시트에 든 프레임 수
 * @property loop 정해진 횟수만큼 되풀이해도 되는 구간인지
 */
data class SpriteClipVO(
    val url: String,
    val frames: Int,
    val loop: Boolean = false,
)
