package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

enum class NyummyBadgeTone { Neutral, Success, Warning, Danger, Info }

/** 상태 배지. 색만으로 의미를 전하지 않도록 항상 글자를 넣는다. */
@Composable
fun NyummyBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: NyummyBadgeTone = NyummyBadgeTone.Neutral,
) {
    val theme = NyummyTheme
    val bg = theme.colors.bg
    val content = theme.colors.content
    val (container, color) = when (tone) {
        NyummyBadgeTone.Neutral -> bg.surfaceSunken to content.secondary
        NyummyBadgeTone.Success -> bg.successSubtle to content.success
        NyummyBadgeTone.Warning -> bg.warningSubtle to content.warning
        NyummyBadgeTone.Danger -> bg.dangerSubtle to content.danger
        NyummyBadgeTone.Info -> bg.infoSubtle to content.info
    }
    Box(
        modifier = modifier
            .background(container, RoundedCornerShape(theme.radius.xs))
            .padding(horizontal = theme.spacing.s8, vertical = NyummyComponentDimens.BadgeVerticalPadding),
    ) {
        NyummyText(text = text, style = theme.typography.labelS, color = color, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyBadgePreview() {
    NyummyTheme {
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyBadgeTone.entries.forEach { NyummyBadge(text = "대기", tone = it) }
        }
    }
}
