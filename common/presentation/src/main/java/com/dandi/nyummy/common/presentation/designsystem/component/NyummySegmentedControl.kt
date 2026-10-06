package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 2~3개 중 하나를 고르는 세그먼트. 높이 48, 움푹한 바탕 안에서 선택한 칸만 흰 바탕 + 그린 2px 테두리.
 * 칸은 같은 너비로 나뉘고, 전체 너비는 [modifier]로 정한다.
 */
@Composable
fun NyummySegmentedControl(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val theme = NyummyTheme
    val containerShape = RoundedCornerShape(theme.radius.s)
    val optionShape = RoundedCornerShape(theme.radius.xs)

    Row(
        modifier = modifier
            .height(theme.size.touchTarget)
            .background(theme.colors.bg.surfaceSunken, containerShape)
            .padding(theme.spacing.s4)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s4),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = selected,
                        interactionSource = rememberNyummyInteractionSource(),
                        indication = null,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .then(
                        if (selected) {
                            Modifier
                                .background(theme.colors.bg.surface, optionShape)
                                .border(
                                    theme.borderWidth.bold,
                                    if (enabled) theme.colors.border.selected else theme.colors.border.default,
                                    optionShape,
                                )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(
                    text = option,
                    style = theme.typography.labelM,
                    color = when {
                        !enabled -> theme.colors.content.disabled
                        selected -> theme.colors.content.brand
                        else -> theme.colors.content.secondary
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummySegmentedControlPreview() {
    NyummyTheme {
        var selected by remember { mutableIntStateOf(0) }
        NyummySegmentedControl(
            options = persistentListOf("남성", "여성"),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth().padding(NyummyTheme.spacing.gutter),
        )
    }
}
