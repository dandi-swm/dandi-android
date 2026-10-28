package com.dandi.nyummy.home.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.cat.domain.CatPick
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 지금 재생할 냐미 동작. 화면이 그대로 그릴 수 있게 재생 컴포넌트의 모델로 바꿔 둔다.
 *
 * @property clips 재생할 동작 묶음의 시트들(시트마다 칸 규격과 프레임 시간을 담는다)
 * @property sheetUrls 지금 동작과 다음 동작의 시트 주소. 다음 동작으로 넘어갈 때 멈칫하지 않게 미리 읽어 둔다.
 * @property playId 바뀌면 같은 묶음이어도 처음부터 다시 재생한다
 * @property restMillis 동작이 끝난 뒤 다음 동작까지 쉬는 시간
 * @property loopTimes 되풀이 조각을 몇 번 돌지. null이면 재생 컴포넌트의 기본값
 */
@Immutable
data class HomeCatMotion(
    val clips: ImmutableList<NyummySpriteClip>,
    val sheetUrls: ImmutableList<String>,
    val playId: Int,
    val restMillis: Long,
    val loopTimes: Int? = null,
)

/**
 * 고른 동작을 재생할 [HomeCatMotion]으로 바꾼다. 가운데 되풀이 조각 횟수는 동작이 정한 값을 쓴다.
 * [prefetchUrls]는 다음에 틀 동작의 시트라, 지금 동작을 도는 동안 미리 읽어 둔다.
 */
internal fun CatPick.toHomeCatMotion(
    playId: Int,
    restMillis: Long,
    prefetchUrls: List<String> = emptyList(),
): HomeCatMotion = HomeCatMotion(
    clips = motion.clips.map { it.toSpriteClip(frame, it.loop) }.toImmutableList(),
    sheetUrls = (motion.clips.map { it.url } + prefetchUrls).distinct().toImmutableList(),
    playId = playId,
    restMillis = restMillis,
    loopTimes = motion.loopTimes,
)

private fun SpriteClipVO.toSpriteClip(frame: SpriteFrameVO, loop: Boolean) = NyummySpriteClip(
    sheet = NyummySpriteSheet(
        url = url,
        frameWidth = frame.width,
        frameHeight = frame.height,
        totalFrames = frames,
        framesPerRow = frame.framesPerRow,
        frameDurationMillis = frame.durationMs,
    ),
    loop = loop,
)
