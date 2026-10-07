package com.dandi.nyummy.home.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.component.NyummySpriteView
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteAnimation
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteFrame
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyVoiceBubble
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.home.presentation.HomeCatMotion
import com.dandi.nyummy.home.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max

/**
 * 냐미가 사는 고양이방 카드. 홈에서 남는 높이를 모두 차지한다.
 *
 * - 픽셀 방 배경은 카드를 빈틈없이 덮도록 키우고 아래를 기준으로 넘치는 쪽을 자른다(Crop, 하단 정렬).
 *   카드가 세로로 길면 좌우가, 가로로 넓으면 위쪽이 잘린다.
 * - 냐미 크기와 위치는 배경이 커진 배율로 정해 화면 크기와 상관없이 늘 같은 러그 자리에 앉는다.
 * - 대사는 냐미 머리 위 말풍선, 오른쪽 위에는 방 메뉴, 아래에는 [bottom](오늘 바)을 둔다.
 *
 * @param catMotion 서버에서 받은 냐미 동작. 받는 중이면 null이다.
 * @param useFallbackCat 냐미 애니메이션을 받지 못해 기본 냐미(앱에 든 스프라이트)로 대신한다.
 */
@Composable
internal fun HomeRoomCard(
    hasRecordedToday: Boolean,
    catMotion: HomeCatMotion?,
    useFallbackCat: Boolean,
    onCatClick: () -> Unit,
    onCatMotionFinished: (playId: Int) -> Unit,
    speech: String,
    isMenuExpanded: Boolean,
    onToggleMenu: () -> Unit,
    onMyRoomClick: () -> Unit,
    onShareFriendClick: () -> Unit,
    onNyamiStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottom: @Composable () -> Unit,
) {
    val background = ImageBitmap.imageResource(R.drawable.home_room_background)
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(NyummyTheme.radius.l))
            .background(NyummyTheme.colors.bg.sceneRoomFloor),
    ) {
        val roomScale = max(maxWidth / RoomDesignWidth, maxHeight / RoomDesignHeight)
        val nyamiSize = (NyamiDesignSize * roomScale).coerceAtLeast(NyamiMinSize)
        val nyamiTop = maxHeight - NyamiDesignBottom * roomScale - nyamiSize
        Image(
            bitmap = background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize(),
        )
        val nyamiModifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = nyamiTop)
            .size(nyamiSize)
        when {
            catMotion != null -> HomeNyami(
                motion = catMotion,
                onClick = onCatClick,
                onMotionFinished = { onCatMotionFinished(catMotion.playId) },
                // 바깥 냐미가 이미 설명을 전달하므로 안쪽 기본 냐미는 설명을 빼서 두 번 읽히지 않게 한다.
                fallback = {
                    HomeFallbackNyami(hasRecordedToday = hasRecordedToday, describe = false, modifier = Modifier.fillMaxSize())
                },
                modifier = nyamiModifier,
            )
            useFallbackCat -> HomeFallbackNyami(hasRecordedToday = hasRecordedToday, modifier = nyamiModifier)
        }
        NyummyVoiceBubble(
            text = speech,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .widthIn(max = BubbleMaxWidth)
                .offset(y = -(maxHeight - nyamiTop - nyamiSize * BubbleOverlapRatio)),
        )
        HomeRoomMenu(
            isExpanded = isMenuExpanded,
            onToggle = onToggleMenu,
            onMyRoomClick = onMyRoomClick,
            onShareFriendClick = onShareFriendClick,
            onNyamiStatusClick = onNyamiStatusClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(NyummyTheme.spacing.s12),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(NyummyTheme.spacing.s16),
        ) {
            bottom()
        }
    }
}

/**
 * 러그 위의 냐미. 서버에서 받은 동작 묶음을 재생하고, 끝나면 [onMotionFinished]로 다음 동작을 요청한다.
 * 누르면 [onClick]. 시트 이미지를 받지 못하면 [fallback]을 보여 준다.
 */
@Composable
private fun HomeNyami(
    motion: HomeCatMotion,
    onClick: () -> Unit,
    onMotionFinished: () -> Unit,
    fallback: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animation = motion.animation
    val clips = remember(animation, motion.group) {
        animation.groups.getOrElse(motion.group) { animation.groups.first() }
            .map { NyummySpriteClip(url = it.url, frames = it.frames, loop = it.loop) }
            .toImmutableList()
    }
    val frame = remember(animation) {
        NyummySpriteFrame(
            width = animation.frame.width,
            height = animation.frame.height,
            framesPerRow = animation.frame.framesPerRow,
            durationMs = animation.frame.durationMs,
        )
    }
    val description = stringResource(R.string.home_character_description)
    NyummySpriteAnimation(
        clips = clips,
        frame = frame,
        restMillis = motion.restMillis,
        onFinished = onMotionFinished,
        playId = motion.playId,
        error = fallback,
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
private fun HomeFallbackNyami(
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
            contentDescription = if (describe) stringResource(R.string.home_character_description) else null,
        )
    }
}

/**
 * 방 메뉴. 접혀 있으면 발바닥 버튼 하나, 펼치면 마이룸, 친구에게 공유, 냐미 상태, 닫기가 한 줄로 나온다.
 * 흰 알약 바탕, 버튼 44, 픽셀 아이콘 32.
 */
@Composable
private fun HomeRoomMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onMyRoomClick: () -> Unit,
    onShareFriendClick: () -> Unit,
    onNyamiStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Row(
        modifier = modifier
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(MenuPadding),
        horizontalArrangement = Arrangement.spacedBy(MenuGap),
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(MenuAnimationMillis)) + expandHorizontally(tween(MenuAnimationMillis), expandFrom = Alignment.End),
            exit = fadeOut(tween(MenuAnimationMillis)) + shrinkHorizontally(tween(MenuAnimationMillis), shrinkTowards = Alignment.End),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MenuGap)) {
                RoomMenuButton(R.drawable.home_ic_my_room, stringResource(R.string.home_menu_my_room), onMyRoomClick)
                RoomMenuButton(R.drawable.home_ic_share_friend, stringResource(R.string.home_menu_share_friend), onShareFriendClick)
                RoomMenuButton(R.drawable.home_ic_nyami_status, stringResource(R.string.home_menu_nyami_status), onNyamiStatusClick)
            }
        }
        RoomMenuButton(
            icon = if (isExpanded) R.drawable.home_ic_menu_close else R.drawable.home_ic_menu_open,
            description = stringResource(if (isExpanded) R.string.home_menu_collapse else R.string.home_menu_expand),
            onClick = onToggle,
        )
    }
}

/** 픽셀 아이콘 버튼. 픽셀이 번지지 않게 필터 없이 그린다. */
@Composable
private fun RoomMenuButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MenuButtonSize)
            .nyummyClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(icon),
            contentDescription = description,
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(MenuIconSize),
        )
    }
}

/**
 * 방 배경을 배율 1로 그렸을 때의 크기와, 그때 냐미 크기, 카드 바닥에서 냐미 발까지의 높이.
 * 셋 다 배경이 커진 배율만큼 함께 커져 냐미가 러그 위에 머문다. 아주 작은 화면에서도 냐미는 120 아래로 줄이지 않는다.
 */
private val RoomDesignWidth = 430.dp
private val RoomDesignHeight = 564.dp
private val NyamiDesignSize = 155.dp
private val NyamiDesignBottom = 116.dp
private val NyamiMinSize = 120.dp

/** 말풍선 꼬리 끝이 냐미 위쪽에서 이만큼(냐미 크기 대비) 내려와 머리에 닿는다. 스프라이트 위쪽 여백 때문이다. */
private const val BubbleOverlapRatio = 0.267f
private val BubbleMaxWidth = 276.dp
private val MenuButtonSize = 44.dp
/** 픽셀 아이콘은 밀도 버킷을 32dp로 만들어 두었다. 같은 크기로 그려야 기기에서 다시 줄이며 픽셀이 뭉개지지 않는다. */
private val MenuIconSize = 32.dp
private val MenuPadding = 6.dp
private val MenuGap = 6.dp
private const val MenuAnimationMillis = 180

private val SleepLoopSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_sleep_loop_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 8,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val WakeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_wake_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val DozeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_doze_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

@Preview(showBackground = true, widthDp = 350, heightDp = 567)
@Composable
private fun HomeRoomCardPreview() {
    NyummyTheme {
        HomeRoomCard(
            hasRecordedToday = true,
            catMotion = null,
            useFallbackCat = true,
            onCatClick = {},
            onCatMotionFinished = {},
            speech = "냠냠! 오늘도 챙겨줘서 고마워",
            isMenuExpanded = true,
            onToggleMenu = {},
            onMyRoomClick = {},
            onShareFriendClick = {},
            onNyamiStatusClick = {},
            bottom = {},
        )
    }
}
