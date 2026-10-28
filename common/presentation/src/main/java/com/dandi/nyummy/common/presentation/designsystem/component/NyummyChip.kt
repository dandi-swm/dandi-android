package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 필터, 선택 칩. 높이 36, pill 모양, 테두리 없음.
 * 선택되면 초록 채움 + 흰 글자, 아니면 회색 채움 + 회색 글자.
 * 누를 수 없으면([enabled] false) 글자가 흐려진다. 고른 칩은 연초록 바탕이 남아 무엇을 골랐는지 보인다.
 */
@Composable
fun NyummyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    val bg = NyummyTheme.colors.bg
    val container = when {
        selected && enabled -> bg.actionPrimary
        selected -> bg.selected
        else -> bg.actionNeutral
    }
    val content = when {
        !enabled -> NyummyTheme.colors.content.disabled
        selected -> NyummyTheme.colors.content.onAction
        else -> NyummyTheme.colors.content.secondary
    }

    Box(
        modifier = modifier
            .semantics { this.selected = selected }
            .nyummyPressable(
                interactionSource = rememberNyummyInteractionSource(),
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .height(NyummyComponentDimens.CompactControlHeight)
            .background(container, shape)
            .padding(horizontal = NyummyTheme.spacing.s16),
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(text = text, style = NyummyTheme.typography.labelL, color = content, maxLines = 1)
    }
}

/**
 * 정보성 칩(연초록). 누를 수 없고, 주 버튼보다 가볍다.
 * 문장 속 핵심 숫자 [highlight]만 굵게 바꿔 강조한다("이번 달 **4일** 기록했어요!").
 */
@Composable
fun NyummyInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    highlight: String? = null,
) {
    val bold = NyummyTheme.typography.labelS.fontWeight
    val annotated = buildAnnotatedString {
        val start = highlight?.takeIf { it.isNotEmpty() }?.let { text.indexOf(it) } ?: -1
        if (start < 0) {
            append(text)
        } else {
            append(text.substring(0, start))
            withStyle(SpanStyle(fontWeight = bold)) { append(highlight) }
            append(text.substring(start + highlight!!.length))
        }
    }
    Box(
        modifier = modifier
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.xs))
            .padding(horizontal = NyummyTheme.spacing.s8, vertical = NyummyComponentDimens.InfoChipVerticalPadding),
    ) {
        NyummyText(text = annotated, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.brand, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyChipPreview() {
    NyummyTheme {
        var selected by remember { mutableStateOf("아침") }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            listOf("아침", "점심", "저녁").forEach { meal ->
                NyummyChip(text = meal, selected = meal == selected, onClick = { selected = meal })
            }
            NyummyChip(text = "간식", selected = true, onClick = {}, enabled = false)
        }
    }
}
