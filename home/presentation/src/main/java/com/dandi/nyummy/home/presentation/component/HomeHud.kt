package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.home.presentation.R
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 홈 상단 HUD: 지갑(보유 코인) 카드와 우편·공지·설정 퀵 액션 버튼 줄.
 */
@Composable
internal fun HomeServiceHud(
    coinBalance: Int,
    hasUnreadNotice: Boolean,
    onWalletClick: () -> Unit,
    onMailClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        HomeWalletCard(
            coinBalance = coinBalance,
            onClick = onWalletClick,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space16))
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
            HomeQuickAction(
                iconRes = CommonR.drawable.nyummy_icon_mail,
                label = stringResource(R.string.home_action_mail),
                onClick = onMailClick,
            )
            HomeQuickAction(
                iconRes = CommonR.drawable.nyummy_icon_bell,
                label = stringResource(R.string.home_action_notice),
                showAlertDot = hasUnreadNotice,
                onClick = onNoticeClick,
            )
            HomeQuickAction(
                iconRes = CommonR.drawable.nyummy_icon_settings,
                label = stringResource(R.string.home_action_settings),
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun HomeWalletCard(
    coinBalance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20)

    Surface(
        onClick = onClick,
        modifier = modifier
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
            )
            .height(WalletCardHeight),
        shape = shape,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WalletCardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_icon_coin),
                contentDescription = null,
                modifier = Modifier.size(WalletCoinIconSize),
                tint = Color.Unspecified,
            )
            Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space12))
            Column(modifier = Modifier.weight(1f)) {
                DandiText(
                    text = stringResource(R.string.home_wallet_label),
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
                DandiText(
                    text = String.format(Locale.getDefault(), "%,d", coinBalance),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.numberStrongL,
                )
            }
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(WalletChevronSize),
                tint = colors.contentDefaultLevel1,
            )
        }
    }
}

@Composable
private fun HomeQuickAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAlertDot: Boolean = false,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape: Shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16)

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .designSystemDropShadow(
                    shape = shape,
                    shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
                )
                .size(width = QuickActionWidth, height = QuickActionHeight),
            shape = shape,
            color = colors.bgDefaultLevel1,
            contentColor = colors.contentDefaultLevel1,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(QuickActionIconSize),
                    tint = colors.contentIconLevel0,
                )
                Spacer(modifier = Modifier.height(QuickActionLabelGap))
                DandiText(
                    text = label,
                    color = colors.contentDefaultLevel1,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
            }
        }
        if (showAlertDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AlertDotInset, end = AlertDotInset)
                    .size(AlertDotSize)
                    .background(color = colors.bgBrandDefault, shape = CircleShape),
            )
        }
    }
}

/**
 * 스트릭(연속 기록) 요약 배너.
 */
@Composable
internal fun HomeStreakBanner(
    streakDays: Int,
    recordsUntilNextReward: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(StreakBannerHeight),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = colors.bgSuccessSoft,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Row(
            modifier = Modifier.padding(
                start = StreakBannerStartPadding,
                end = StreakBannerEndPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_icon_leaf),
                contentDescription = null,
                modifier = Modifier.size(StreakLeafIconSize),
                tint = colors.contentIconSuccess,
            )
            Spacer(modifier = Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            DandiText(
                text = stringResource(R.string.home_streak_days, streakDays),
                color = colors.contentAccentSage,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
            )
            Spacer(modifier = Modifier.width(StreakMessageGap))
            DandiText(
                text = stringResource(R.string.home_streak_message),
                modifier = Modifier.weight(1f),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
            )
            DandiText(
                text = stringResource(R.string.home_streak_next_reward, recordsUntilNextReward),
                color = colors.contentAccentSage,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(StreakChevronSize),
                tint = colors.contentAccentSage,
            )
        }
    }
}

private val WalletCardHeight = 64.dp
private val WalletCardHorizontalPadding = 12.dp
private val WalletCoinIconSize = 44.dp
private val WalletChevronSize = 20.dp
private val QuickActionWidth = 48.dp
private val QuickActionHeight = 62.dp
private val QuickActionIconSize = 24.dp
private val QuickActionLabelGap = 2.dp
private val AlertDotInset = 5.dp
private val AlertDotSize = 9.dp
private val StreakBannerHeight = 38.dp
private val StreakBannerStartPadding = 14.dp
private val StreakBannerEndPadding = 12.dp
private val StreakLeafIconSize = 20.dp
private val StreakMessageGap = 4.dp
private val StreakChevronSize = 14.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HomeServiceHudPreview() {
    DesignSystemTheme {
        Column {
            HomeServiceHud(
                coinBalance = 1240,
                hasUnreadNotice = true,
                onWalletClick = {},
                onMailClick = {},
                onNoticeClick = {},
                onSettingsClick = {},
            )
            Spacer(modifier = Modifier.height(16.dp))
            HomeStreakBanner(
                streakDays = 7,
                recordsUntilNextReward = 1,
                onClick = {},
            )
        }
    }
}
