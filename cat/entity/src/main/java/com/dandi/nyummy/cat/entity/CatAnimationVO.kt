package com.dandi.nyummy.cat.entity

/**
 * 내 고양이 체형 하나의 상황별 애니메이션.
 *
 * @property weight 체형 이름(LEAN, SLIM, NORMAL, CHUBBY, PLUMP)
 * @property animations 상황별 애니메이션. 앱이 모르는 상황과 고를 동작이 없는 상황은 들어 있지 않다.
 */
data class CatAnimationSetVO(
    val weight: String = "",
    val animations: Map<CatState, CatAnimationVO> = emptyMap(),
) {
    companion object {
        val empty = CatAnimationSetVO()
    }
}

/**
 * 한 상황의 애니메이션.
 *
 * @property frame 이 상황의 모든 스프라이트 시트가 공유하는 프레임 규격
 * @property motions 이 상황에서 고를 수 있는 동작. 가중치가 0보다 크고 클립이 있는 것만 들어 있다.
 */
data class CatAnimationVO(
    val state: CatState,
    val frame: SpriteFrameVO,
    val motions: List<CatMotionVO>,
)

/**
 * 동작 하나. 클립을 순서대로 재생하며, 모든 동작은 앉은 자세로 시작하고 끝난다.
 *
 * @property name 동작 이름(예 lie_down). 한 상황 안에서 겹치지 않는다.
 * @property weight 이 상황에서 이 동작이 나올 비율
 * @property clips 이어서 재생할 시트
 */
data class CatMotionVO(
    val name: String,
    val weight: Double,
    val clips: List<SpriteClipVO>,
) {
    /** 가운데 되풀이 조각을 몇 번 돌지. 정하지 않았으면 null이다. */
    val loopTimes: Int? get() = clips.firstNotNullOfOrNull { it.loopTimes }
}

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
 * @property loopTimes 가운데 되풀이 구간을 몇 번 돌지. null이면 재생 기본값이다.
 */
data class SpriteClipVO(
    val url: String,
    val frames: Int,
    val loop: Boolean = false,
    val loopTimes: Int? = null,
)
