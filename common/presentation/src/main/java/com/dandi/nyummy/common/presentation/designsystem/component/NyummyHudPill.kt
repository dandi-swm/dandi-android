package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 홈 HUD의 코인 알약. 숫자는 Number 서체(number/m).
 * [onAddClick]이 있으면 오른쪽에 + 버튼이 붙고 알약 전체가 충전 진입점이 된다.
 */
@Composable
fun NyummyCoinPill(
    coins: String,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
) {
    val description = stringResource(R.string.nyummy_coin_description, coins)
    HudPillSurface(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = description }
            .then(
                if (onAddClick != null) {
                    Modifier.nyummyPressable(
                        interactionSource = rememberNyummyInteractionSource(),
                        enabled = true,
                        role = Role.Button,
                        onClick = onAddClick,
                    )
                } else {
                    Modifier
                },
            ),
        endPadding = if (onAddClick != null) NyummyTheme.spacing.s8 else NyummyComponentDimens.HudPillEndPadding,
    ) {
        HudAsset(R.drawable.nyummy_asset_coin)
        NyummyText(text = coins, style = NyummyTheme.typography.numberM, maxLines = 1)
        if (onAddClick != null) {
            Box(
                modifier = Modifier
                    .size(NyummyTheme.size.iconL)
                    .background(NyummyTheme.colors.bg.selected, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.nyummy_ic_plus),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.brand,
                    modifier = Modifier.size(NyummyTheme.size.iconXs),
                )
            }
        }
    }
}

/** 홈 HUD의 연속 기록 알약. "7 일째"처럼 숫자는 브랜드색, 단위는 보조색. */
@Composable
fun NyummyStreakPill(
    days: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.nyummy_streak_description, days)
    HudPillSurface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        endPadding = NyummyComponentDimens.HudPillEndPadding,
    ) {
        HudAsset(R.drawable.nyummy_asset_streak)
        NyummyText(
            text = days.toString(),
            style = NyummyTheme.typography.numberM,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
        NyummyText(
            text = stringResource(R.string.nyummy_streak_unit),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun HudPillSurface(
    modifier: Modifier,
    endPadding: Dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Row(
        modifier = modifier
            .height(NyummyComponentDimens.HudPillHeight)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
            .padding(start = NyummyTheme.spacing.s8, end = endPadding),
        horizontalArrangement = Arrangement.spacedBy(NyummyComponentDimens.HudPillGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun HudAsset(resId: Int) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier.size(NyummyTheme.size.iconL),
    )
}

@Preview(showBackground = true)
@Composable
private fun NyummyHudPillPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
                NyummyCoinPill(coins = "1,240")
                NyummyStreakPill(days = 7)
            }
            NyummyCoinPill(coins = "1,240", onAddClick = {})
        }
    }
}
