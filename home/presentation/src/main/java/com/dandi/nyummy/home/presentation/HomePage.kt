package com.dandi.nyummy.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.home.presentation.component.HomeMyRoomCard
import com.dandi.nyummy.home.presentation.component.HomeServiceHud
import com.dandi.nyummy.home.presentation.component.HomeStreakBanner
import com.dandi.nyummy.home.presentation.mock.HomeMockData
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 홈(마이룸) 화면. Figma `LIVE / Home · My Room` 시안을 구현한다.
 *
 * 위에서부터 HUD(지갑·퀵 액션) → 스트릭 배너 → 마이룸 카드 → `밥 주기` 버튼 순으로 쌓이며,
 * 하단 메뉴와 화면 이동은 앱 공통 Shell 이 담당한다.
 *
 * @param modifier 화면의 크기와 배치 방식을 조정합니다.
 * @param onFeedClick `밥 주기` 버튼을 눌렀을 때 호출됩니다.
 */
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onFeedClick = onFeedClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
    onFeedClick: () -> Unit,
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
            onFeedClick = onFeedClick,
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUIState,
    onIntent: (HomeIntent) -> Unit,
    onFeedClick: () -> Unit,
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
                .weight(1f),
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space20))
        HomeFeedButton(
            label = stringResource(R.string.home_feed_button),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeFeedButtonTestTag),
            onClick = onFeedClick,
        )
        Spacer(modifier = Modifier.height(FeedButtonBottomGap))
    }
}

@Composable
private fun HomeFeedButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NyummyButton(
        label = label,
        modifier = modifier.height(FeedButtonHeight),
        size = NyummyButtonSize.Large,
        leadingIcon = {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_food_salad),
                contentDescription = null,
                modifier = Modifier.size(FeedButtonIconSize),
            )
        },
        onClick = onClick,
    )
}

private val FeedButtonHeight = 60.dp
private val FeedButtonIconSize = 32.dp
private val FeedButtonBottomGap = 16.dp
private const val HomeFeedButtonTestTag = "home_feed_button"

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePagePreview() {
    DesignSystemTheme {
        HomeScreen(
            uiState = HomeUIState(summary = HomeMockData.summary),
            onIntent = {},
            onFeedClick = {},
        )
    }
}
