package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 음식을 찾는 데 오래 걸린다는 안내 시트(식사 기록 06d·06e)입니다. 기다리는 냐미 장면 위에 공용 바텀 시트로 올라옵니다.
 *
 * 서버가 이어서 음식을 찾고 끝나면 알림으로 알려 주므로, 홈에서 기다리라고 안내합니다.
 * 기기 알림이 꺼져 있으면([notificationsOff]) 알림을 켜 달라는 줄을 같이 보여 줍니다.
 * 바깥을 누르거나 끌어내리거나 뒤로 가서 닫아도 [onHomeClick]으로 홈에 갑니다. 이 화면에 더 머물 일이 없기 때문입니다.
 */
@Composable
internal fun MealSlowNoticeSheet(
    notificationsOff: Boolean,
    onEnableNotificationClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NyummyBottomSheet(onDismissRequest = onHomeClick, modifier = modifier) {
        MealSlowNoticeContent(notificationsOff, onEnableNotificationClick, onHomeClick)
    }
}

/** 시트 안 내용. 창 없이 그려 프리뷰에서도 쓴다. */
@Composable
private fun ColumnScope.MealSlowNoticeContent(
    notificationsOff: Boolean,
    onEnableNotificationClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
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

private val RowStartPadding = 14.dp
private val BellSize = 20.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MealSlowNoticeContentPreview() {
    NyummyTheme { SlowNoticePreviewColumn(notificationsOff = false) }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MealSlowNoticeContentNotificationOffPreview() {
    NyummyTheme { SlowNoticePreviewColumn(notificationsOff = true) }
}

@Composable
private fun SlowNoticePreviewColumn(notificationsOff: Boolean) {
    Column(
        modifier = Modifier.padding(NyummyTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
    ) {
        MealSlowNoticeContent(notificationsOff = notificationsOff, onEnableNotificationClick = {}, onHomeClick = {})
    }
}
