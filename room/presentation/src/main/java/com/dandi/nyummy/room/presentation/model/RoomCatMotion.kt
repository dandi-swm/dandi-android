package com.dandi.nyummy.room.presentation.model

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 방에서 지금 재생할 냐미 동작. 재생 컴포넌트가 그대로 그릴 수 있게 바꿔 둔다.
 *
 * @property group 고른 동작 묶음 번호. 다음 동작을 고를 때 같은 묶음이 연달아 나오지 않게 쓴다.
 * @property sheetUrls 같은 상태의 모든 시트 주소. 다음 동작으로 넘어갈 때 멈칫하지 않게 미리 받아 둔다.
 * @property playId 바뀌면 같은 묶음이어도 처음부터 다시 재생한다
 * @property restMillis 동작이 끝난 뒤 다음 동작까지 쉬는 시간
 */
@Immutable
data class RoomCatMotion(
    val group: Int,
    val clips: ImmutableList<NyummySpriteClip>,
    val sheetUrls: ImmutableList<String>,
    val playId: Int,
    val restMillis: Long,
)

/** [group]번째 동작 묶음을 재생할 [RoomCatMotion]으로 바꾼다. 묶음 번호가 범위를 벗어나면 첫 묶음을 쓴다. */
internal fun CatAnimationVO.toRoomCatMotion(group: Int, playId: Int, restMillis: Long): RoomCatMotion =
    RoomCatMotion(
        group = group,
        clips = (groups.getOrNull(group) ?: groups.firstOrNull().orEmpty())
            .map { clip ->
                NyummySpriteClip(
                    sheet = NyummySpriteSheet(
                        url = clip.url,
                        frameWidth = frame.width,
                        frameHeight = frame.height,
                        totalFrames = clip.frames,
                        framesPerRow = frame.framesPerRow,
                        frameDurationMillis = frame.durationMs,
                    ),
                    loop = clip.loop,
                )
            }
            .toImmutableList(),
        sheetUrls = groups.flatten().map { it.url }.distinct().toImmutableList(),
        playId = playId,
        restMillis = restMillis,
    )
