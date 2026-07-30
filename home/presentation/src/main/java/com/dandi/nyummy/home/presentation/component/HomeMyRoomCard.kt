package com.dandi.nyummy.home.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyCharacterView
import com.dandi.nyummy.common.presentation.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.component.NyummySpeech
import com.dandi.nyummy.common.presentation.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.home.presentation.R
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/** 엎드려 조는 루프 (8프레임). */
private val HomeCharacterSleepLoopSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_sleep_loop_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 8,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

/** 엎드린 자세에서 눈 뜨고 몸을 일으켜 앉는 전환 (17프레임). */
private val HomeCharacterWakeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_wake_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

/** 앉은 자세에서 졸다가 엎드리는 전환 (17프레임). */
private val HomeCharacterDozeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_doze_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

/**
 * 홈 냐미의 행동 시퀀스 페이즈.
 *
 * LyingDown(앉아 있다 엎드림) → Dozing(엎드려 졺) → Waking(일어남) 순서로 각 시트를
 * 1회씩 재생하고, 마지막 Waking 이 끝나면 일어나 앉은 프레임에 정지한 채 머문다.
 */
private enum class HomeCharacterPhase { LyingDown, Dozing, Waking }

/**
 * 냐미가 사는 마이룸 카드.
 *
 * 룸 일러스트 배경 위에 스프라이트 캐릭터를 러그 자리에 올려 그리고,
 * 대사는 캐릭터 머리 위 말풍선([NyummyCharacterView])으로 함께 붙어 다닌다.
 */
@Composable
internal fun HomeMyRoomCard(
    speechTitle: String,
    speechBody: String,
    todayRecordedCount: Int,
    todayCalorieKcal: Int,
    goalCalorieKcal: Int,
    calorieProgress: Float,
    calorieProgressPercent: Int,
    onTodaySummaryClick: () -> Unit,
    isActionMenuExpanded: Boolean,
    onToggleActionMenu: () -> Unit,
    onShareClick: () -> Unit,
    onRoomEditClick: () -> Unit,
    onSpeechReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius32)

    Surface(
        modifier = modifier.designSystemDropShadow(
            shape = shape,
            shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
        ),
        shape = shape,
        color = colors.assetSceneRoomWall,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.home_room_background),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )

            HomeCharacter(
                speechTitle = speechTitle,
                speechBody = speechBody,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = CharacterBottomGap),
            )

            HomeRoomFloatingActions(
                isExpanded = isActionMenuExpanded,
                onToggleClick = onToggleActionMenu,
                onShareClick = onShareClick,
                onRoomEditClick = onRoomEditClick,
                onSpeechReplayClick = onSpeechReplayClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(FloatingActionPadding),
            )

            HomeTodaySummaryBar(
                todayRecordedCount = todayRecordedCount,
                todayCalorieKcal = todayCalorieKcal,
                goalCalorieKcal = goalCalorieKcal,
                calorieProgress = calorieProgress,
                calorieProgressPercent = calorieProgressPercent,
                onClick = onTodaySummaryClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(TodaySummaryPadding),
            )
        }
    }
}

/**
 * 마이룸 카드 우상단의 가로 접이식 플로팅 액션 메뉴.
 *
 * 오른쪽 끝 토글(햄버거 ↔ 닫기) 버튼을 누르면 공유 · 꾸미기 · 말풍선 다시 보기 버튼이
 * 토글에서 가까운 것부터 순서대로(스태거) 왼쪽으로 촤라락 펼쳐진다.
 */
/**
 * 행동 시퀀스를 1회 재생하는 홈 냐미.
 *
 * 페이즈마다 시트를 갈아끼우며, 유한 재생 종료 콜백([NyummyCharacterView.onAnimationEnd])으로
 * 다음 페이즈로 넘어간다. 마지막 페이즈([HomeCharacterPhase.Waking])가 끝나면 시트 교체가
 * 없으므로 마지막 프레임에 정지한 채 유지된다.
 */
@Composable
private fun HomeCharacter(
    speechTitle: String,
    speechBody: String,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(HomeCharacterPhase.LyingDown) }

    val sheet = when (phase) {
        HomeCharacterPhase.LyingDown -> HomeCharacterDozeSheet
        HomeCharacterPhase.Dozing -> HomeCharacterSleepLoopSheet
        HomeCharacterPhase.Waking -> HomeCharacterWakeSheet
    }

    NyummyCharacterView(
        sheet = sheet,
        modifier = modifier,
        speech = NyummySpeech(
            title = speechTitle.takeIf { it.isNotBlank() },
            body = speechBody,
        ).takeIf { speechBody.isNotBlank() },
        displayWidth = DesignSystemThemeImpl.designSystemSize.characterHome,
        iterations = 1,
        onAnimationEnd = {
            phase = when (phase) {
                HomeCharacterPhase.LyingDown -> HomeCharacterPhase.Dozing
                HomeCharacterPhase.Dozing -> HomeCharacterPhase.Waking
                HomeCharacterPhase.Waking -> HomeCharacterPhase.Waking
            }
        },
        contentDescription = stringResource(R.string.home_character_description),
    )
}

@Composable
private fun HomeRoomFloatingActions(
    isExpanded: Boolean,
    onToggleClick: () -> Unit,
    onShareClick: () -> Unit,
    onRoomEditClick: () -> Unit,
    onSpeechReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        Triple(R.drawable.home_icon_share_camera, stringResource(R.string.home_action_share), onShareClick),
        Triple(R.drawable.home_icon_room_edit, stringResource(R.string.home_action_room_edit), onRoomEditClick),
        Triple(R.drawable.home_icon_speech_replay, stringResource(R.string.home_action_speech_replay), onSpeechReplayClick),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { index, (iconRes, description, onClick) ->
            // 펼칠 때는 토글과 가까운 오른쪽 버튼부터, 접을 때는 왼쪽 끝 버튼부터 사라진다.
            val enterDelay = (actions.lastIndex - index) * ActionStaggerDelayMillis
            val exitDelay = index * ActionStaggerDelayMillis

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(ActionAnimDurationMillis, enterDelay)) +
                    scaleIn(tween(ActionAnimDurationMillis, enterDelay), initialScale = ActionInitialScale) +
                    expandHorizontally(tween(ActionAnimDurationMillis, enterDelay), expandFrom = Alignment.End),
                exit = fadeOut(tween(ActionAnimDurationMillis, exitDelay)) +
                    scaleOut(tween(ActionAnimDurationMillis, exitDelay), targetScale = ActionInitialScale) +
                    shrinkHorizontally(tween(ActionAnimDurationMillis, exitDelay), shrinkTowards = Alignment.End),
            ) {
                HomeRoomFloatingActionButton(
                    iconRes = iconRes,
                    contentDescription = description,
                    onClick = onClick,
                )
            }
        }
        HomeRoomFloatingActionSurface(onClick = onToggleClick) {
            Crossfade(
                targetState = isExpanded,
                animationSpec = tween(durationMillis = ToggleCrossfadeDurationMillis),
                label = "roomActionToggleIcon",
            ) { expanded ->
                Image(
                    painter = painterResource(
                        if (expanded) R.drawable.home_icon_menu_close else R.drawable.home_icon_menu_open,
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.home_action_menu_collapse else R.string.home_action_menu_expand,
                    ),
                    modifier = Modifier.size(FloatingActionIconSize),
                )
            }
        }
    }
}

/** 픽셀아트 아이콘을 담는 원형 플로팅 버튼. 픽셀 원본 색을 살리기 위해 [Image]로 그린다. */
@Composable
private fun HomeRoomFloatingActionButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeRoomFloatingActionSurface(onClick = onClick, modifier = modifier) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(FloatingActionIconSize),
        )
    }
}

/** 플로팅 액션 버튼의 공통 원형 서피스 (반투명 배경 + 드롭 섀도). */
@Composable
private fun HomeRoomFloatingActionSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        onClick = onClick,
        modifier = modifier
            .designSystemDropShadow(
                shape = CircleShape,
                shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
            )
            .size(FloatingActionButtonSize),
        shape = CircleShape,
        color = colors.bgDefaultLevel1.copy(alpha = FloatingActionBackgroundAlpha),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/** 오늘 기록 수·열량 진행률을 요약하는 하단 바. */
@Composable
private fun HomeTodaySummaryBar(
    todayRecordedCount: Int,
    todayCalorieKcal: Int,
    goalCalorieKcal: Int,
    calorieProgress: Float,
    calorieProgressPercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .designSystemDropShadow(
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                shadow = DesignSystemThemeImpl.designSystemElevation.floatingAction,
            )
            .height(TodaySummaryBarHeight),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Row(
            modifier = Modifier.padding(
                start = TodaySummaryStartPadding,
                end = TodaySummaryEndPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.home_icon_today_meal),
                contentDescription = null,
                modifier = Modifier.size(TodayMealIconSize),
            )
            Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DandiText(
                        text = stringResource(R.string.home_today_record_count, todayRecordedCount),
                        color = colors.contentAccentSage,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                    )
                    Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                    DandiText(
                        text = stringResource(
                            R.string.home_today_calorie,
                            String.format(Locale.getDefault(), "%,d", todayCalorieKcal),
                            String.format(Locale.getDefault(), "%,d", goalCalorieKcal),
                        ),
                        color = colors.contentDefaultLevel1,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                    )
                }
                Spacer(modifier = Modifier.height(TodayProgressTopGap))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NyummyLinearProgress(
                        progress = calorieProgress,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                    DandiText(
                        text = stringResource(R.string.home_progress_percent, calorieProgressPercent),
                        color = colors.contentAccentSage,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                    )
                }
            }
            Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space4))
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(TodaySummaryChevronSize),
                tint = colors.contentIconLevel0,
            )
        }
    }
}

private val CharacterBottomGap = 104.dp
private val FloatingActionPadding = 16.dp
private val FloatingActionButtonSize = 48.dp
private val FloatingActionIconSize = 28.dp
private const val FloatingActionBackgroundAlpha = 0.88f
private const val ToggleCrossfadeDurationMillis = 200
private const val ActionAnimDurationMillis = 180
private const val ActionStaggerDelayMillis = 60
private const val ActionInitialScale = 0.6f
private val TodaySummaryPadding = 16.dp
private val TodaySummaryBarHeight = 57.dp
private val TodaySummaryStartPadding = 13.dp
private val TodaySummaryEndPadding = 12.dp
private val TodayMealIconSize = 26.dp
private val TodayProgressTopGap = 5.dp
private val TodaySummaryChevronSize = 20.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun HomeMyRoomCardPreview() {
    DesignSystemTheme {
        HomeMyRoomCard(
            speechTitle = "첫 기록을 기다려요",
            speechBody = "첫 끼를 기록하면 같이 챙겨볼게!",
            todayRecordedCount = 1,
            todayCalorieKcal = 1350,
            goalCalorieKcal = 1800,
            calorieProgress = 0.75f,
            calorieProgressPercent = 75,
            onTodaySummaryClick = {},
            isActionMenuExpanded = true,
            onToggleActionMenu = {},
            onShareClick = {},
            onRoomEditClick = {},
            onSpeechReplayClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
