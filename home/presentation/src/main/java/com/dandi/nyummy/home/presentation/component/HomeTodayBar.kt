package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyLinearProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.home.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 고양이방 아래의 오늘 바. 기록 횟수가 주인공이고 kcal은 얇은 막대로만 참고하게 한다.
 *
 * - 오늘 기록 전: 카메라 버튼 + "첫 끼 기록하기 / 아직 오늘 기록이 없어요". 누르면 식사 기록으로 간다.
 * - 오늘 기록 후: 음식 일러스트 + "오늘 N개 기록" + 얇은 kcal 막대. 누르면 오늘 식사 시트가 열린다.
 */
@Composable
internal fun HomeTodayBar(
    recordedCount: Int,
    calorieProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    val recorded = recordedCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .nyummyClickable(onClick = onClick)
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(start = NyummyTheme.spacing.s16, end = BarEndPadding)
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ThumbSize)
                .background(
                    if (recorded) NyummyTheme.colors.bg.surfaceSunken else NyummyTheme.colors.bg.actionPrimary,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (recorded) {
                Image(
                    painter = painterResource(CommonR.drawable.nyummy_food_smooth_salad),
                    contentDescription = null,
                    modifier = Modifier.size(FoodSize),
                )
            } else {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_ic_camera),
                    contentDescription = null,
                    tint = NyummyTheme.colors.content.onAction,
                    modifier = Modifier.size(NyummyTheme.size.iconM),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (recorded) NyummyTheme.spacing.s8 else NyummyTheme.spacing.s2),
        ) {
            if (recorded) {
                NyummyText(
                    text = stringResource(R.string.home_today_record_count, recordedCount),
                    style = NyummyTheme.typography.titleS,
                    maxLines = 1,
                )
                NyummyLinearProgress(
                    progress = calorieProgress,
                    size = NyummyLinearProgressSize.S,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                NyummyText(
                    text = stringResource(R.string.home_today_empty_title),
                    style = NyummyTheme.typography.titleS,
                    color = NyummyTheme.colors.content.brand,
                    maxLines = 1,
                )
                NyummyText(
                    text = stringResource(R.string.home_today_empty_body),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
            }
        }
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
            contentDescription = null,
            tint = if (recorded) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.brand,
            modifier = Modifier.size(NyummyTheme.size.iconM),
        )
    }
}

private val BarEndPadding = 18.dp
private val ThumbSize = 40.dp
private val FoodSize = 30.dp

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun HomeTodayBarPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            HomeTodayBar(recordedCount = 0, calorieProgress = 0f, onClick = {})
            HomeTodayBar(recordedCount = 1, calorieProgress = 0.75f, onClick = {})
        }
    }
}
