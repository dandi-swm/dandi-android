package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * 보낸 편지(문의) 행. 앞 52 칸(움푹한 둥근 사각 + send 아이콘), 유형과 시각, 한 줄 제목, 답장 상태.
 * 답장이 왔으면 [status]를 브랜드색으로, 기다리는 중이면 회색으로 쓴다. 아래에 1px 구분선을 긋는다.
 *
 * 받은 편지(운영 메시지, 안 읽음 점)는 우편함 아이템 탭이 열릴 때 추가한다.
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
    @DrawableRes icon: Int = R.drawable.nyummy_ic_send,
) {
    val dividerColor = NyummyTheme.colors.border.subtle
    val dividerWidth = NyummyTheme.borderWidth.hairline
    Row(
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
            .padding(vertical = NyummyComponentDimens.MailRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NyummyComponentDimens.MailRowLeadingSize)
                .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.m)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = NyummyTheme.colors.content.secondary,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(NyummyComponentDimens.MailRowMetaGap)) {
                NyummyText(
                    text = type,
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                NyummyText(
                    text = time,
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
            }
            NyummyText(
                text = title,
                style = NyummyTheme.typography.titleS,
                color = NyummyTheme.colors.content.secondary,
                maxLines = 1,
            )
            NyummyText(
                text = status,
                style = NyummyTheme.typography.labelS,
                color = if (isStatusEmphasized) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
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
            )
            NyummyMailRow(
                type = "건의",
                time = "9월 28일",
                title = "히스토리에서 사진을 크게 보고 싶어요",
                status = "답장을 기다리는 중",
                isStatusEmphasized = false,
                onClick = {},
            )
        }
    }
}
