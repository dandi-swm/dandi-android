package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpritePrefetch
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSequenceView
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteView
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyVoiceBubble
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.common.presentation.room.RoomScene
import com.dandi.nyummy.home.presentation.HomeCatMotion
import com.dandi.nyummy.home.presentation.HomeRoomUiModel
import com.dandi.nyummy.home.presentation.R
import kotlinx.collections.immutable.persistentListOf

/**
 * 냐미가 사는 고양이방. 홈에서는 화면 전체를 채우고, 크기는 부르는 쪽이 정한다.
 *
 * - 방은 마이룸에서 꾸민 그대로([room], 배경과 아이템) 그린다. 마이룸이 꺼져 있거나 배경이 없으면 기본 빈 방을 깐다.
 *   방 그림은 자리를 빈틈없이 덮고 아래를 기준으로 맞춘다. 자리가 세로로 길면 좌우가, 넓으면 위쪽이 잘린다.
 * - 냐미 크기와 위치는 방 그림이 커진 배율로 정해 화면 크기와 상관없이 늘 같은 바닥 자리에 앉는다.
 *   이 규칙은 마이룸과 함께 쓰는 [RoomCardLayout]에 있다.
 * - 대사는 냐미 머리 위 말풍선이다. [showSpeech]가 꺼지면(눈 버튼으로 화면 요소를 숨겼을 때) 감춘다.
 *
 * @param room 꾸민 방. null이면 아이템 없이 기본 빈 방만 그린다.
 * @param catMotion 서버에서 받은 냐미 동작. 받는 중이면 null이다.
 * @param useFallbackCat 냐미 애니메이션을 받지 못해 기본 냐미(앱에 든 스프라이트)로 대신한다.
 * @param onCatShown 냐미가 처음 화면에 보인 순간(시트를 받아 그렸거나 기본 냐미를 그렸을 때). 홈 TTI 에 쓴다.
 */
@Composable
internal fun HomeRoom(
    room: HomeRoomUiModel?,
    hasRecordedToday: Boolean,
    catMotion: HomeCatMotion?,
    useFallbackCat: Boolean,
    onCatClick: () -> Unit,
    onCatMotionFinished: (playId: Int) -> Unit,
    onCatShown: () -> Unit,
    speech: String,
    showSpeech: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag(RoomCardTag),
    ) {
        val nyummyRect = RoomCardLayout.nyummyRect(maxWidth, maxHeight)
        val nyummySize = nyummyRect.right - nyummyRect.left
        val nyummyTop = nyummyRect.top
        RoomScene(
            backgroundUrl = room?.backgroundUrl,
            items = room?.items ?: persistentListOf(),
            modifier = Modifier.fillMaxSize(),
        )
        val nyummyModifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = nyummyTop)
            .size(nyummySize)
        when {
            catMotion != null -> HomeNyummy(
                motion = catMotion,
                displayWidth = nyummySize,
                onClick = onCatClick,
                onMotionFinished = { onCatMotionFinished(catMotion.playId) },
                onShown = onCatShown,
                // 바깥 냐미가 이미 설명을 전달하므로 안쪽 기본 냐미는 설명을 빼서 두 번 읽히지 않게 한다.
                fallback = {
                    HomeFallbackNyummy(hasRecordedToday = hasRecordedToday, describe = false, modifier = Modifier.fillMaxSize())
                },
                modifier = nyummyModifier,
            )
            useFallbackCat -> {
                HomeFallbackNyummy(hasRecordedToday = hasRecordedToday, modifier = nyummyModifier)
                // 앱에 든 기본 냐미는 받을 이미지가 없어 그리는 즉시 보인다. 그려진 다음 프레임에 알린다.
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    onCatShown()
                }
            }
        }
        if (showSpeech) {
            NyummyVoiceBubble(
                text = speech,
                typing = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = NyummyTheme.spacing.gutter)
                    .widthIn(max = BubbleMaxWidth)
                    // 말풍선 아래 끝을 냐미 그림 칸의 위 끝에 맞춘다. 동작마다 머리 높이가 달라도 냐미를 가리지 않는다.
                    .offset(y = -(maxHeight - nyummyTop)),
            )
        }
    }
}

/**
 * 러그 위의 냐미. 서버에서 받은 동작 묶음을 재생하고, 끝나면 [onMotionFinished]로 다음 동작을 요청한다.
 * 누르면 [onClick]. 시트 이미지를 받지 못하면 [fallback]을 보여 준다.
 */
@Composable
private fun HomeNyummy(
    motion: HomeCatMotion,
    displayWidth: Dp,
    onClick: () -> Unit,
    onMotionFinished: () -> Unit,
    onShown: () -> Unit,
    fallback: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 같은 상태의 다른 동작으로 넘어갈 때 시트를 받느라 멈칫하지 않게 상태의 모든 시트를 미리 받아 둔다.
    NyummySpritePrefetch(urls = motion.sheetUrls)
    val description = stringResource(R.string.home_character_description)
    NyummySpriteSequenceView(
        clips = motion.clips,
        restMillis = motion.restMillis,
        onFinished = onMotionFinished,
        displayWidth = displayWidth,
        playId = motion.playId,
        // 카드 크기에 비례한 자리 크기 그대로 그린다(정수배로 반올림하면 자리보다 커져 방에 비해 커 보인다).
        integerScale = false,
        error = fallback,
        onShown = onShown,
        modifier = modifier
            .nyummyClickable(onClick = onClick)
            .semantics { contentDescription = description },
    )
}

/**
 * 냐미 애니메이션을 받지 못했을 때(아직 고양이가 없거나 서버, 네트워크 실패) 쓰는 기본 냐미. 앱에 든 스프라이트를 쓴다.
 * 오늘 기록 전이면 엎드려 조는 동작을 반복하고, 기록 후면 일어나 앉은 뒤 그 자세로 머문다.
 */
@Composable
private fun HomeFallbackNyummy(
    hasRecordedToday: Boolean,
    modifier: Modifier = Modifier,
    describe: Boolean = true,
) {
    var lyingDown by remember(hasRecordedToday) { mutableStateOf(!hasRecordedToday) }
    val sheet = when {
        hasRecordedToday -> WakeSheet
        lyingDown -> DozeSheet
        else -> SleepLoopSheet
    }
    BoxWithConstraints(modifier = modifier) {
        NyummySpriteView(
            sheet = sheet,
            displayWidth = maxWidth,
            iterations = if (sheet == SleepLoopSheet) null else 1,
            onAnimationEnd = { if (sheet == DozeSheet) lyingDown = false },
            // 서버 냐미와 같이 카드 크기에 비례한 자리 크기 그대로 그린다.
            integerScale = false,
            contentDescription = if (describe) stringResource(R.string.home_character_description) else null,
        )
    }
}

/** 고양이방. 테스트가 방 크기를 잴 때 찾는다. */
internal const val RoomCardTag = "home_room_card"

private val BubbleMaxWidth = 276.dp

private val SleepLoopSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyummy_sleep_loop_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 8,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val WakeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyummy_wake_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val DozeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyummy_doze_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeRoomPreview() {
    NyummyTheme {
        HomeRoom(
            room = null,
            hasRecordedToday = true,
            catMotion = null,
            useFallbackCat = true,
            onCatClick = {},
            onCatMotionFinished = {},
            onCatShown = {},
            speech = "냠냠! 오늘도 챙겨줘서 고마워",
            showSpeech = true,
        )
    }
}
