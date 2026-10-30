package com.dandi.nyummy.home.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.home.presentation.R
import java.text.NumberFormat

/**
 * 홈 상단. 왼쪽 지갑 카드(보유 코인)는 내용 폭만큼, 오른쪽 끝에 우편, 설정 원형 버튼이 붙는다(v2 HUD).
 * 받을 우편([hasUnreadMail])이나 읽지 않은 공지([hasUnreadNotice])가 있으면 그 버튼에 빨간 점을 단다.
 * 홈 요약에 받을 우편 개수가 없어 v2의 숫자 배지 대신 점을 쓴다.
 */
@Composable
internal fun HomeHud(
    coinBalance: Int,
    hasUnreadNotice: Boolean,
    onWalletClick: () -> Unit,
    onMailClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadMail: Boolean = false,
    mailButtonModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HudHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 코인 칸은 숫자만큼 넓어지되 오른쪽 버튼과 겹치지 않게 버튼 앞까지만 쓴다. 넘치면 숫자 끝을 줄인다.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = NyummyTheme.spacing.s8),
        ) {
            HomeWalletCard(coinBalance = coinBalance, onClick = onWalletClick)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            HomeHudAction(
                icon = CommonR.drawable.nyummy_ic_mail,
                label = stringResource(R.string.home_action_mail),
                onClick = onMailClick,
                showDot = hasUnreadMail,
                modifier = mailButtonModifier,
            )
//            HomeHudAction(
//                icon = CommonR.drawable.nyummy_ic_bell,
//                label = stringResource(R.string.home_action_notice),
//                onClick = onNoticeClick,
//                showDot = hasUnreadNotice,
//            )
            HomeHudAction(
                icon = CommonR.drawable.nyummy_ic_settings,
                label = stringResource(R.string.home_action_settings),
                onClick = onSettingsClick,
            )
        }
    }
}

/** 지갑 카드. 코인 24 + "보유 코인"(label/s) + 숫자(number/l, 냐미 말투 색) + 꺾쇠. 내용 폭만큼만 차지한다. */
@Composable
private fun HomeWalletCard(
    coinBalance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    val coins = NumberFormat.getIntegerInstance().format(coinBalance)
    val description = stringResource(R.string.home_wallet_description, coins)
    Row(
        modifier = modifier
            .height(HudHeight)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(horizontal = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(CommonR.drawable.nyummy_asset_coin),
            contentDescription = null,
            modifier = Modifier.size(CoinSize),
        )
        Column(modifier = Modifier.weight(1f, fill = false)) {
            NyummyText(
                text = stringResource(R.string.home_wallet_label),
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.secondary,
                maxLines = 1,
            )
            NyummyText(
                text = coins,
                style = NyummyTheme.typography.numberL,
                color = NyummyTheme.colors.content.voice,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(NyummyTheme.size.iconXs),
        )
    }
}

/** 상단 원형 버튼(40, 흰 바탕, secondary 아이콘 22). 읽지 않은 소식이 있으면 빨간 점을 단다. 라벨은 읽어 주기로만 쓴다. */
@Composable
private fun HomeHudAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    showDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(HudActionSize)
            .nyummyShadow(CircleShape, NyummyTheme.elevation.soft)
            .nyummyClickable(onClick = onClick)
            .background(NyummyTheme.colors.bg.surface, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = NyummyTheme.colors.content.secondary,
            modifier = Modifier.size(HudIconSize),
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(DotSize)
                    .background(NyummyTheme.colors.content.danger, CircleShape)
                    .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.bg.surface, CircleShape),
            )
        }
    }
}

/** 연속 기록 배너. 흰 카드에 "N일째"(브랜드) + "냐미에게 밥을 챙겼어요!"(primary), 둘 다 label/m. 보여 주기만 하고 누를 수 없다. */
@Composable
internal fun HomeStreakBanner(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(StreakHeight)
            .semantics(mergeDescendants = true) {}
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(horizontal = NyummyTheme.spacing.s16),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = stringResource(R.string.home_streak_days, streakDays),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
        NyummyText(
            text = catNameStringResource(R.string.home_streak_message),
            style = NyummyTheme.typography.labelM,
            maxLines = 1,
        )
    }
}

/** 코인 칸 높이. 숫자(number/l, AndongKaturi)가 위아래로 잘리지 않게 내용(38)보다 넉넉히 둔다. */
internal val HudHeight = 56.dp
private val HudActionSize = 40.dp
private val HudIconSize = 22.dp
private val CoinSize = 24.dp
private val DotSize = 9.dp
internal val StreakHeight = 44.dp

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
                hasUnreadNotice = true,
                hasUnreadMail = true,
                onWalletClick = {},
                onMailClick = {},
                onNoticeClick = {},
                onSettingsClick = {},
            )
            HomeStreakBanner(streakDays = 7)
        }
    }
}
