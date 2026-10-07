package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 식사 기록 한 줄. 왼쪽 48 자리(사진, 분석 중이거나 실패면 냐미) + 이름(title/s) + 시각 등 보조 문구(body/s, tertiary)
 * + 선택적 상태 배지. 흰 바탕, 2px 테두리, radius m.
 *
 * 왼쪽 자리는 [leading]으로 채운다. 사진이 없으면 [NyummyMealRowPlaceholder]를 쓴다.
 * [onClick]이 있으면 눌러서 상세로 갈 수 있다.
 */
@Composable
fun NyummyMealRow(
    title: String,
    subtitle: String,
    leading: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeTone: NyummyBadgeTone = NyummyBadgeTone.Neutral,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    val click = if (onClick != null) Modifier.nyummyClickable(onClick = onClick) else Modifier.semantics(mergeDescendants = true) {}
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(click)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
            .padding(start = NyummyTheme.spacing.s12, end = NyummyTheme.spacing.s16)
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NyummyComponentDimens.MealRowLeadingSize)
                .clip(RoundedCornerShape(NyummyTheme.radius.s)),
            contentAlignment = Alignment.Center,
            content = leading,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleS, maxLines = 1)
            NyummyText(
                text = subtitle,
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
        }
        if (badge != null) NyummyBadge(text = badge, tone = badgeTone)
    }
}

/** 사진이 없는 식사의 왼쪽 자리. 연한 바탕에 카메라 아이콘을 둔다(추가 버튼으로 읽히지 않게 + 없는 아이콘). */
@Composable
fun BoxScope.NyummyMealRowPlaceholder() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(NyummyTheme.colors.bg.surfaceSunken),
    )
    Icon(
        painter = painterResource(R.drawable.nyummy_ic_camera),
        contentDescription = null,
        tint = NyummyTheme.colors.content.tertiary,
        modifier = Modifier.size(NyummyTheme.size.iconL),
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyMealRowPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyMealRow(title = "닭가슴살 샐러드", subtitle = "오후 12:24", leading = { NyummyMealRowPlaceholder() })
            NyummyMealRow(
                title = "분석 중이에요",
                subtitle = "잠시만 기다려 주세요",
                leading = { NyummyPoseImage(pose = NyummyPose.Ask, size = NyummyComponentDimens.MealRowLeadingSize) },
                badge = "분석 중",
                badgeTone = NyummyBadgeTone.Info,
            )
        }
    }
}
