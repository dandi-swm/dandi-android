package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 보낸 편지(문의) 행. 위에 유형 태그와 시각, 가운데 제목(body/l), 아래 답장 상태. 아래에 1px 구분선을 긋는다.
 * 답장이 왔으면([isStatusEmphasized]) 상태를 브랜드색 굵은 글자와 체크로, 기다리는 중이면 secondary로 쓴다.
 * [isUnread]면 상태 옆에 빨간 점을 단다(홈 HUD 안 읽음 점과 같은 모양).
 */
@Composable
fun NyummyMailRow(
    type: String,
    time: String,
    title: String,
    status: String,
    isStatusEmphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUnread: Boolean = false,
) {
    val dividerColor = NyummyTheme.colors.border.subtle
    val dividerWidth = NyummyTheme.borderWidth.hairline
    val content = NyummyTheme.colors.content
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .drawBehind {
                val y = size.height - dividerWidth.toPx() / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), dividerWidth.toPx())
            }
            .padding(vertical = NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NyummyBadge(text = type, tone = NyummyBadgeTone.Neutral)
            Spacer(Modifier.weight(1f))
            NyummyText(text = time, style = NyummyTheme.typography.bodyS, color = content.tertiary, maxLines = 1)
        }
        NyummyText(text = title, style = NyummyTheme.typography.bodyL, maxLines = 1)
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = status,
                style = if (isStatusEmphasized) NyummyTheme.typography.labelS else NyummyTheme.typography.bodyS,
                color = if (isStatusEmphasized) content.brand else content.secondary,
                maxLines = 1,
            )
            if (isStatusEmphasized) {
                Icon(
                    painter = painterResource(R.drawable.nyummy_ic_check),
                    contentDescription = null,
                    tint = content.brand,
                    modifier = Modifier.size(NyummyTheme.size.iconXs),
                )
            }
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .padding(start = NyummyTheme.spacing.s2)
                        .size(NyummyComponentDimens.UnreadDotSize)
                        .background(content.danger, CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyMailRowPreview() {
    NyummyTheme {
        Column(modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter)) {
            NyummyMailRow(
                type = "버그 제보",
                time = "10월 4일",
                title = "사진을 찍은 뒤 기록이 사라졌어요",
                status = "답장이 왔어요",
                isStatusEmphasized = true,
                onClick = {},
                isUnread = true,
            )
            NyummyMailRow(
                type = "건의",
                time = "9월 28일",
                title = "히스토리에서 사진을 크게 보고 싶어요",
                status = "답장을 기다리고 있어요",
                isStatusEmphasized = false,
                onClick = {},
            )
        }
    }
}
