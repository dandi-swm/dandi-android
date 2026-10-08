package com.dandi.nyummy.home.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteFrame
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
 * 지금 재생할 냐미 동작. 화면이 그대로 그릴 수 있게 재생 컴포넌트의 모델로 바꿔 둔다.
 *
 * @property clips 재생할 동작 묶음의 시트들
 * @property frame 시트 한 칸의 규격과 프레임 시간
 * @property sheetUrls 같은 상태의 모든 시트 주소. 다음 동작으로 넘어갈 때 멈칫하지 않게 미리 받아 둔다.
 * @property playId 바뀌면 같은 묶음이어도 처음부터 다시 재생한다
 * @property restMillis 동작이 끝난 뒤 다음 동작까지 쉬는 시간
 */
@Immutable
data class HomeCatMotion(
    val clips: ImmutableList<NyummySpriteClip>,
    val frame: NyummySpriteFrame,
    val sheetUrls: ImmutableList<String>,
    val playId: Int,
    val restMillis: Long,
)

/** [group]번째 동작 묶음을 재생할 [HomeCatMotion]으로 바꾼다. 묶음 번호가 범위를 벗어나면 첫 묶음을 쓴다. */
internal fun CatAnimationVO.toHomeCatMotion(group: Int, playId: Int, restMillis: Long): HomeCatMotion =
    HomeCatMotion(
        clips = (groups.getOrNull(group) ?: groups.firstOrNull().orEmpty())
            .map { NyummySpriteClip(url = it.url, frames = it.frames, loop = it.loop) }
            .toImmutableList(),
        frame = NyummySpriteFrame(
            width = frame.width,
            height = frame.height,
            framesPerRow = frame.framesPerRow,
            durationMs = frame.durationMs,
        ),
        sheetUrls = groups.flatten().map { it.url }.distinct().toImmutableList(),
        playId = playId,
        restMillis = restMillis,
    )
