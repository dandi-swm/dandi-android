package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 음식을 찾는 데 오래 걸린다는 안내 시트(식사 기록 06d·06e)입니다. 기다리는 냐미 장면 위를 어둡게 덮고 아래에서 올라옵니다.
 *
 * 서버가 이어서 음식을 찾고 끝나면 알림으로 알려 주므로, 홈에서 기다리라고 안내합니다.
 * 기기 알림이 꺼져 있으면([notificationsOff]) 알림을 켜 달라는 줄을 같이 보여 줍니다.
 */
@Composable
internal fun MealSlowNoticeSheet(
    notificationsOff: Boolean,
    onEnableNotificationClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.scrim),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    NyummyTheme.colors.bg.surface,
                    RoundedCornerShape(topStart = NyummyTheme.radius.l, topEnd = NyummyTheme.radius.l),
                )
                .navigationBarsPadding()
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(top = NyummyTheme.spacing.s12, bottom = NyummyTheme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            Box(
                Modifier
                    .width(HandleWidth)
                    .height(HandleHeight)
                    .background(NyummyTheme.colors.border.default, RoundedCornerShape(NyummyTheme.radius.full)),
            )
            NyummyText(text = stringResource(R.string.meal_slow_notice_title), style = NyummyTheme.typography.titleL)
            NyummyText(
                text = stringResource(R.string.meal_slow_notice_body),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
            )
            if (notificationsOff) NotificationOffRow(onEnableClick = onEnableNotificationClick)
            NyummyButton(
                text = stringResource(R.string.meal_record_go_home),
                onClick = onHomeClick,
                modifier = Modifier.fillMaxWidth(),
                size = NyummyButtonSize.L,
            )
        }
    }
}

/** 기기 알림이 꺼져 있으면 분석이 끝나도 알 수 없으니 켜 달라고 안내한다. */
@Composable
private fun NotificationOffRow(onEnableClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.promoSubtle, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(start = RowStartPadding, end = NyummyTheme.spacing.s10)
            .padding(vertical = NyummyTheme.spacing.s10),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_bell),
            contentDescription = null,
            tint = NyummyTheme.colors.content.secondary,
            modifier = Modifier.size(BellSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            NyummyText(text = stringResource(R.string.meal_slow_notice_notification_off), style = NyummyTheme.typography.labelM)
            NyummyText(
                text = stringResource(R.string.meal_slow_notice_notification_hint),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        NyummyButton(
            text = stringResource(R.string.meal_slow_notice_notification_enable),
            onClick = onEnableClick,
            size = NyummyButtonSize.S,
        )
    }
}

private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private val RowStartPadding = 14.dp
private val BellSize = 20.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealSlowNoticeSheetPreview() {
    NyummyTheme { MealSlowNoticeSheet(notificationsOff = false, onEnableNotificationClick = {}, onHomeClick = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealSlowNoticeSheetNotificationOffPreview() {
    NyummyTheme { MealSlowNoticeSheet(notificationsOff = true, onEnableNotificationClick = {}, onHomeClick = {}) }
}
