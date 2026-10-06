package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

enum class NyummyTextButtonTone { Brand, Neutral, Danger }

enum class NyummyTextButtonSize {
    /** label/m, 아이콘 16, 세로 여백 12. */
    M,

    /** label/s, 아이콘 14, 세로 여백 8. */
    S,
}

/**
 * 배경, 테두리, 그림자가 없는 글자 버튼. "지금 기록하기", "다시 분석하기", "더보기" 같은 보조 행동에 쓴다.
 * 아이콘 색은 글자색을 따르고, 터치 영역은 최소 48dp를 보장한다.
 */
@Composable
fun NyummyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: NyummyTextButtonTone = NyummyTextButtonTone.Brand,
    size: NyummyTextButtonSize = NyummyTextButtonSize.M,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    val color = when {
        !enabled -> NyummyTheme.colors.content.disabled
        tone == NyummyTextButtonTone.Brand -> NyummyTheme.colors.content.brand
        tone == NyummyTextButtonTone.Neutral -> NyummyTheme.colors.content.secondary
        else -> NyummyTheme.colors.content.danger
    }
    val textStyle = if (size == NyummyTextButtonSize.M) NyummyTheme.typography.labelM else NyummyTheme.typography.labelS
    val iconSize = if (size == NyummyTextButtonSize.M) NyummyTheme.size.iconS else NyummyTheme.size.iconXs
    val verticalPadding = if (size == NyummyTextButtonSize.M) NyummyTheme.spacing.s12 else NyummyTheme.spacing.s8

    Row(
        modifier = modifier
            .nyummyPressable(
                interactionSource = rememberNyummyInteractionSource(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = NyummyTheme.spacing.s4, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painterResource(leadingIcon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
        }
        NyummyText(text = text, style = textStyle, color = color, maxLines = 1)
        if (trailingIcon != null) {
            Icon(
                painterResource(trailingIcon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyTextButtonPreview() {
    NyummyTheme {
        Column {
            NyummyTextButtonTone.entries.forEach { tone ->
                Row {
                    NyummyTextButton("지금 기록하기", {}, tone = tone, trailingIcon = R.drawable.nyummy_ic_chevron_right)
                    NyummyTextButton("더보기", {}, tone = tone, size = NyummyTextButtonSize.S)
                    NyummyTextButton("접기", {}, tone = tone, enabled = false)
                }
            }
        }
    }
}
