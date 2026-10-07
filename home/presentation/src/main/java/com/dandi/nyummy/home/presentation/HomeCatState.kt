package com.dandi.nyummy.home.presentation

import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState

/**
 * 홈 상황에 맞는 냐미 상태. 상태를 고르는 규칙은 여기 한 곳에만 둔다.
 *
 * - 식사를 기록하고 막 돌아왔으면 먹는 반응(CONTENT). 그 동작이 끝날 때까지는 유지한다.
 * - 오늘 기록이 있으면 여유(RELAXED), 없으면 기다림(CONFLICTED_CAUTIOUS).
 * 응답에 고른 상태가 없으면 기본 인사(FRIENDLY)로 대신하는 것은 애니메이션을 꺼낼 때 처리한다.
 *
 * @param recordedJustNow 화면에 돌아왔을 때 오늘 기록 수가 늘었다
 * @param isCelebrating 지금 먹는 반응을 보여 주는 중이다
 */
internal fun homeCatState(
    hasRecordedToday: Boolean,
    recordedJustNow: Boolean,
    isCelebrating: Boolean,
): CatState = when {
    recordedJustNow || isCelebrating -> CatState.CONTENT
    hasRecordedToday -> CatState.RELAXED
    else -> CatState.CONFLICTED_CAUTIOUS
}

/**
 * 지금 재생할 냐미 동작.
 *
 * @property animation 상태의 애니메이션
 * @property group 재생할 동작 묶음 번호
 * @property playId 바뀌면 같은 묶음이어도 처음부터 다시 재생한다
 * @property restMillis 동작이 끝난 뒤 다음 동작까지 쉬는 시간
 */
data class HomeCatMotion(
    val animation: CatAnimationVO,
    val group: Int,
    val playId: Int,
    val restMillis: Long,
)
