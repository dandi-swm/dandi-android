package com.dandi.nyummy.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.home.presentation.component.HomeMyRoomCard
import com.dandi.nyummy.home.presentation.component.HomeServiceHud
import com.dandi.nyummy.home.presentation.component.HomeStreakBanner
import com.dandi.nyummy.home.presentation.component.HomeTodaySummarySheet
import com.dandi.nyummy.home.presentation.mock.HomeMockData

/**
 * 홈(마이룸) 화면. Figma `LIVE / Home · My Room` 시안을 구현한다.
 *
 * 위에서부터 HUD(지갑·퀵 액션) → 스트릭 배너 → 마이룸 카드 순으로 쌓이며,
 * 식사 기록 진입은 앱 공통 Shell 의 하단 내비게이션 중앙 카메라 버튼이 담당한다.
 *
 * @param viewModel 홈 화면 상태를 관리하는 ViewModel.
 */
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgSurfaceIvory),
    ) {
        HomeContent(
            uiState = uiState,
            onIntent = onIntent,
        )
        if (uiState.isTodaySummarySheetVisible) {
            HomeTodaySummarySheet(
                todayRecordedCount = summary.todayRecordedCount,
                todayCalorieKcal = summary.todayCalorieKcal,
                goalCalorieKcal = summary.goalCalorieKcal,
                calorieProgress = uiState.calorieProgress,
                calorieProgressPercent = uiState.calorieProgressPercent,
                remainingCalorieKcal = uiState.remainingCalorieKcal,
                onDismiss = { onIntent(HomeIntent.DismissTodaySummarySheet) },
                onAddMeal = {
                    onIntent(HomeIntent.ClickAddMeal)
//                    onIntent(HomeIntent.DismissTodaySummarySheet)
                },
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space8))
        HomeServiceHud(
            coinBalance = summary.coinBalance,
            hasUnreadNotice = summary.hasUnreadNotice,
            onWalletClick = { onIntent(HomeIntent.ClickWallet) },
            onMailClick = { onIntent(HomeIntent.ClickMail) },
            onNoticeClick = { onIntent(HomeIntent.ClickNotice) },
            onSettingsClick = { onIntent(HomeIntent.ClickSettings) },
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space16))
        HomeStreakBanner(
            streakDays = summary.streakDays,
            recordsUntilNextReward = summary.recordsUntilNextReward,
            onClick = { onIntent(HomeIntent.ClickStreak) },
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space16))
        HomeMyRoomCard(
            speechTitle = summary.speechTitle,
            speechBody = summary.speechBody,
            todayRecordedCount = summary.todayRecordedCount,
            todayCalorieKcal = summary.todayCalorieKcal,
            goalCalorieKcal = summary.goalCalorieKcal,
            calorieProgress = uiState.calorieProgress,
            calorieProgressPercent = uiState.calorieProgressPercent,
            onTodaySummaryClick = { onIntent(HomeIntent.ClickTodaySummary) },
            isActionMenuExpanded = uiState.isRoomActionMenuExpanded,
            onToggleActionMenu = { onIntent(HomeIntent.ToggleRoomActionMenu) },
            onShareClick = { onIntent(HomeIntent.ClickShare) },
            onRoomEditClick = { onIntent(HomeIntent.ClickRoomEdit) },
            onSpeechReplayClick = { onIntent(HomeIntent.ClickSpeechReplay) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(max = RoomCardMaxHeight),
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space16))
    }
}

/** 밥 주기 버튼 제거로 방 카드가 길어질 때, 롱스크린에서 배경 크롭이 어색해지지 않도록 상한을 둔다. */
private val RoomCardMaxHeight = 560.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePagePreview() {
    DesignSystemTheme {
        HomeScreen(
            uiState = HomeUIState(summary = HomeMockData.summary),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePageTodaySummarySheetPreview() {
    DesignSystemTheme {
        HomeScreen(
            uiState = HomeUIState(
                summary = HomeMockData.summary,
                isTodaySummarySheetVisible = true,
            ),
            onIntent = {},
        )
    }
}
