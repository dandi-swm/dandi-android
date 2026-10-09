package com.dandi.nyummy.home.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.home.presentation.R
import java.text.NumberFormat
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 홈 상단. 왼쪽 지갑 카드(보유 코인)가 남는 폭을 차지하고, 오른쪽에 우편, 설정 버튼이 붙는다.
 */
@Composable
internal fun HomeHud(
    coinBalance: Int,
    onWalletClick: () -> Unit,
    onMailClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        HomeWalletCard(
            coinBalance = coinBalance,
            onClick = onWalletClick,
            modifier = Modifier.weight(1f),
        )
        HomeHudAction(
            icon = CommonR.drawable.nyummy_ic_mail,
            label = stringResource(R.string.home_action_mail),
            onClick = onMailClick,
        )
        HomeHudAction(
            icon = CommonR.drawable.nyummy_ic_settings,
            label = stringResource(R.string.home_action_settings),
            onClick = onSettingsClick,
        )
    }
}

/**
 * 지갑 카드. 코인 일러스트 32 + "보유 코인"(body/s) + 숫자(number/l) + 셰브론.
 * 폭이 좁은 화면(360 등)에서는 숫자가 잘리지 않도록 셰브론을 뺀다.
 */
@Composable
private fun HomeWalletCard(
    coinBalance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.l)
    val coins = NumberFormat.getIntegerInstance().format(coinBalance)
    val description = stringResource(R.string.home_wallet_description, coins)
    BoxWithConstraints(
        modifier = modifier
            .height(HudHeight)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape),
    ) {
        val showChevron = maxWidth >= WalletChevronMinWidth
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = WalletStartPadding, end = NyummyTheme.spacing.s8),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_asset_coin),
                contentDescription = null,
                modifier = Modifier.size(CoinSize),
            )
            Column(modifier = Modifier.weight(1f)) {
                NyummyText(
                    text = stringResource(R.string.home_wallet_label),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
                NyummyText(text = coins, style = NyummyTheme.typography.numberL, maxLines = 1)
            }
            if (showChevron) {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier.size(NyummyTheme.size.iconM),
                )
            }
        }
    }
}

/** 상단 작은 버튼. 아이콘 24 + 라벨(label/s). */
@Composable
private fun HomeHudAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Box(
        modifier = Modifier
            .width(HudActionWidth)
            .height(HudHeight)
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = NyummyTheme.colors.content.primary,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
            NyummyText(
                text = label,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.secondary,
                maxLines = 1,
            )
        }
    }
}

/** 연속 기록 배너. 새싹 일러스트 + "N일째"(number/s) + "냐미에게 밥을 챙겼어요!"(label/m), 브랜드색. */
@Composable
internal fun HomeStreakBanner(
    streakDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(StreakHeight)
            .semantics(mergeDescendants = true) {}
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.selected, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(start = StreakStartPadding, end = NyummyTheme.spacing.s16),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(CommonR.drawable.nyummy_asset_streak),
            contentDescription = null,
            modifier = Modifier.size(NyummyTheme.size.iconL),
        )
        NyummyText(
            text = stringResource(R.string.home_streak_days, streakDays),
            style = NyummyTheme.typography.numberS,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
        NyummyText(
            text = stringResource(R.string.home_streak_message),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
    }
}

private val HudHeight = 64.dp
private val HudActionWidth = 52.dp
private val WalletStartPadding = 10.dp

/** 지갑 카드가 이 폭보다 좁으면 셰브론을 숨긴다(390 화면 170, 360 화면 140). */
private val WalletChevronMinWidth = 160.dp
private val CoinSize = 32.dp
private val StreakHeight = 44.dp
private val StreakStartPadding = 14.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HomeHudPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            HomeHud(
                coinBalance = 1240,
                onWalletClick = {},
                onMailClick = {},
                onSettingsClick = {},
            )
            HomeStreakBanner(streakDays = 7, onClick = {})
        }
    }
}
