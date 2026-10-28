package com.dandi.nyummy.home.presentation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.coachmark.coachMarkTarget
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.helper.HideBottomBarEffect
import com.dandi.nyummy.common.presentation.room.fitRoomCard
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.home.presentation.component.HomeHud
import com.dandi.nyummy.home.presentation.component.HomeRoom
import com.dandi.nyummy.home.presentation.component.HomeStreakBanner
import com.dandi.nyummy.home.presentation.component.HudHeight
import com.dandi.nyummy.home.presentation.component.StreakHeight

/**
 * 홈. 냐미가 사는 방이 화면 전체(시스템 바와 하단 내비 뒤까지)를 채우고, 그 위에 지갑과 우편, 설정 → 연속 기록 배너 →
 * 눈 버튼이 뜬다. 식사 기록은 하단 내비 가운데 카메라로 들어간다.
 * 눈 버튼을 누르면 위 화면 요소, 말풍선, 하단 내비를 숨겨 냐미와 방만 보여 준다. 방과 냐미 자리는 그대로다.
 * 넓은 화면에서는 위 화면 요소의 폭을 480으로 묶어 가운데 둔다.
 */
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 식사를 기록하고 돌아오면 숫자가 바뀌어야 하므로 화면이 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(HomeIntent.ScreenResumed)
        onPauseOrDispose { viewModel.onIntent(HomeIntent.ScreenPaused) }
    }

    HomeScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
internal fun HomeScreen(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
) {
    if (uiState.isSummaryLoaded) {
        // 받은 요약 숫자가 그려진 다음 프레임에 알린다. 홈 TTI 는 API 완료가 아니라 이 시점에 끝난다.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            onIntent(HomeIntent.SummaryShown)
        }
    }
    HideBottomBarEffect(hidden = uiState.isUiHidden)
    // 건강 이야기를 보는 동안은 코치마크를 내린다.
    HomeTutorialCoachMark(tutorial = uiState.tutorial.takeIf { uiState.healthStory == null }, onIntent = onIntent)
    HomeTutorialDialogs(skipConfirm = uiState.tutorialSkipConfirm, healthStory = uiState.healthStory, onIntent = onIntent)
    HomeNoticeDialog(notice = uiState.notice, onIntent = onIntent)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        val fallbackSpeech = stringResource(
            if (uiState.hasRecordedToday) R.string.home_speech_recorded else R.string.home_speech_waiting,
        )
        HomeRoom(
            room = uiState.room,
            hasRecordedToday = uiState.hasRecordedToday,
            catMotion = uiState.catMotion,
            useFallbackCat = uiState.isCatAnimationFailed,
            onCatClick = { onIntent(HomeIntent.ClickCat) },
            onCatMotionFinished = { playId -> onIntent(HomeIntent.CatMotionFinished(playId)) },
            onCatShown = { onIntent(HomeIntent.CatShown) },
            speech = uiState.catLine ?: fallbackSpeech,
            speechKey = uiState.catLineId,
            onSpeechRevealed = { onIntent(HomeIntent.CatLineRevealed(uiState.catLineId)) },
            showSpeech = !uiState.isUiHidden,
            modifier = Modifier
                .fillMaxSize()
                // 화면 전체를 채운다. 접는 폰처럼 너무 넓거나 아주 긴 화면이면 방 그림이 많이 잘리지 않게 줄여 바닥에 붙인다.
                .fitRoomCard(),
        )
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = NyummyTheme.spacing.gutter),
        ) {
            Spacer(Modifier.height(NyummyTheme.spacing.s8))
            // 숨겨도 자리는 그대로 두어 눈 버튼이 움직이지 않게 한다.
            Box(Modifier.height(HudHeight + NyummyTheme.spacing.s10 + StreakHeight)) {
                androidx.compose.animation.AnimatedVisibility(visible = !uiState.isUiHidden, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        HomeHud(
                            coinBalance = uiState.summary.coinBalance,
                            // 공지 API가 아직 없어 읽지 않은 공지 점은 띄우지 않는다.
                            hasUnreadNotice = false,
                            onWalletClick = { onIntent(HomeIntent.ClickWallet) },
                            onMailClick = { onIntent(HomeIntent.ClickMail) },
                            mailButtonModifier = Modifier.coachMarkTarget(HomeMailCoachMarkKey) { onIntent(HomeIntent.ClickMail) },
                            onNoticeClick = { onIntent(HomeIntent.ClickNotice) },
                            onSettingsClick = { onIntent(HomeIntent.ClickSettings) },
                            hasUnreadMail = uiState.showsMailBadge,
                        )
                        Spacer(Modifier.height(NyummyTheme.spacing.s10))
                        HomeStreakBanner(streakDays = uiState.summary.streakDays)
                    }
                }
            }
            Spacer(Modifier.height(NyummyTheme.spacing.s10))
            NyummyIconButton(
                icon = if (uiState.isUiHidden) CommonR.drawable.nyummy_ic_eye_off else CommonR.drawable.nyummy_ic_eye,
                contentDescription = stringResource(
                    if (uiState.isUiHidden) R.string.home_show_ui else R.string.home_hide_ui,
                ),
                onClick = { onIntent(HomeIntent.ToggleUiHidden) },
                style = NyummyIconButtonStyle.Filled,
                tint = NyummyTheme.colors.content.secondary,
                modifier = Modifier.align(Alignment.End),
            )
            // debug 빌드에만: 냐미가 지금 어떤 기분으로 무슨 동작을 하는지 보이고, 누르면 기분을 차례로 고정한다.
            uiState.catDebugLabel?.let { label ->
                Spacer(Modifier.height(NyummyTheme.spacing.s8))
                NyummyText(
                    text = label,
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.secondary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                        .nyummyClickable(onClick = { onIntent(HomeIntent.ClickCatDebug) })
                        .padding(horizontal = NyummyTheme.spacing.s12, vertical = NyummyTheme.spacing.s4),
                )
            }
        }
    }
}

private val ContentMaxWidth = 480.dp

private val PreviewSummary = HomeSummaryVO(
    coinBalance = 1240,
    streakDays = 7,
    todayRecordedCount = 1,
    todayCalorieKcal = 1350,
    goalCalorieKcal = 1800,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeBeforeRecordPreview() {
    NyummyTheme {
        HomeScreen(uiState = HomeUIState(summary = PreviewSummary.copy(todayRecordedCount = 0)), onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeUiHiddenPreview() {
    NyummyTheme {
        HomeScreen(uiState = HomeUIState(summary = PreviewSummary, isUiHidden = true), onIntent = {})
    }
}
