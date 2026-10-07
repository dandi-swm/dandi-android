package com.dandi.nyummy.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.home.presentation.component.HomeHud
import com.dandi.nyummy.home.presentation.component.HomeRoomCard
import com.dandi.nyummy.home.presentation.component.HomeStreakBanner
import com.dandi.nyummy.home.presentation.component.HomeTodayBar
import com.dandi.nyummy.home.presentation.component.HomeTodaySheet

/**
 * 홈. 위에서부터 지갑과 우편, 공지, 설정 → 연속 기록 배너 → 고양이방 카드(남는 높이 전부) 순서로 쌓는다.
 * 식사 기록은 하단 내비 카메라나 오늘 바(오늘 기록이 없을 때)로 들어간다.
 * 넓은 화면에서는 콘텐츠 폭을 480으로 묶어 가운데 둔다.
 */
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 식사를 기록하고 돌아오면 숫자가 바뀌어야 하므로 화면이 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(HomeIntent.ScreenResumed)
        onPauseOrDispose { }
    }

    HomeScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
internal fun HomeScreen(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = NyummyTheme.spacing.gutter),
        ) {
            Spacer(Modifier.height(NyummyTheme.spacing.s8))
            HomeHud(
                coinBalance = uiState.summary.coinBalance,
                // 공지 API가 아직 없어 읽지 않은 공지 점은 띄우지 않는다.
                hasUnreadNotice = false,
                onWalletClick = { onIntent(HomeIntent.ClickWallet) },
                onMailClick = { onIntent(HomeIntent.ClickMail) },
                onNoticeClick = { onIntent(HomeIntent.ClickNotice) },
                onSettingsClick = { onIntent(HomeIntent.ClickSettings) },
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s12))
            HomeStreakBanner(
                streakDays = uiState.summary.streakDays,
                onClick = { onIntent(HomeIntent.ClickStreak) },
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s12))
            HomeRoomCard(
                hasRecordedToday = uiState.hasRecordedToday,
                speech = stringResource(
                    if (uiState.hasRecordedToday) R.string.home_speech_recorded else R.string.home_speech_waiting,
                ),
                isMenuExpanded = uiState.isRoomMenuExpanded,
                onToggleMenu = { onIntent(HomeIntent.ToggleRoomMenu) },
                onMyRoomClick = { onIntent(HomeIntent.ClickMyRoom) },
                onShareFriendClick = { onIntent(HomeIntent.ClickShareFriend) },
                onNyamiStatusClick = { onIntent(HomeIntent.ClickNyamiStatus) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                HomeTodayBar(
                    recordedCount = uiState.summary.todayRecordedCount,
                    calorieProgress = uiState.calorieProgress,
                    onClick = { onIntent(HomeIntent.ClickTodayBar) },
                )
            }
            Spacer(Modifier.height(NyummyTheme.spacing.s16))
        }
        if (uiState.isTodaySheetVisible) {
            HomeTodaySheet(
                recordedCount = uiState.summary.todayRecordedCount,
                meals = uiState.todayMeals,
                isLoading = uiState.isTodayMealsLoading,
                isFailed = uiState.isTodayMealsFailed,
                onRetry = { onIntent(HomeIntent.RetryTodayMeals) },
                onAddMeal = { onIntent(HomeIntent.ClickAddMeal) },
                onDismiss = { onIntent(HomeIntent.DismissTodaySheet) },
            )
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

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun HomeBeforeRecordPreview() {
    NyummyTheme {
        HomeScreen(uiState = HomeUIState(summary = PreviewSummary.copy(todayRecordedCount = 0)), onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun HomeAfterRecordPreview() {
    NyummyTheme {
        HomeScreen(uiState = HomeUIState(summary = PreviewSummary, isRoomMenuExpanded = true), onIntent = {})
    }
}
