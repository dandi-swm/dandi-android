package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 필터, 선택 칩. 높이 36(터치 영역 48), pill 모양, 2px 테두리.
 * 선택되면 연민트 바탕 + 그린 테두리 + 브랜드색 글자.
 */
@Composable
fun NyummyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.full)
    val container = if (selected) theme.colors.bg.selected else theme.colors.bg.surface
    val border = if (selected) theme.colors.border.selected else theme.colors.border.default
    val content = if (selected) theme.colors.content.brand else theme.colors.content.secondary

    Box(
        modifier = modifier
            .semantics { this.selected = selected }
            .nyummyPressable(
                interactionSource = rememberNyummyInteractionSource(),
                enabled = true,
                role = Role.Tab,
                onClick = onClick,
            )
            .height(NyummyComponentDimens.CompactControlHeight)
            .background(container, shape)
            .border(theme.borderWidth.bold, border, shape)
            .padding(horizontal = NyummyComponentDimens.CompactControlHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(text = text, style = theme.typography.labelM, color = content, maxLines = 1)
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
        }
    }
}
