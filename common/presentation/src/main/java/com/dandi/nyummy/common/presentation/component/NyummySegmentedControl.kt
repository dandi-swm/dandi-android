package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 단일 선택 세그먼트 컨트롤 (Figma `회원가입`의 gender-segment 패턴).
 *
 * 트랙 위에 선택 항목만 카드로 떠오르는 형태. 항목 수만큼 균등 분할한다.
 */
@Composable
fun NyummySegmentedControl(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val radius = DesignSystemThemeImpl.designSystemRadius
    Row(
        modifier = modifier
            .height(SegmentTrackHeight)
            .clip(DesignSystemThemeImpl.designSystemShape.cardDefault)
            .background(colors.bgSurfaceSubtle)
            .padding(SegmentTrackPadding)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(radius.radius12))
                    .background(if (selected) colors.bgDefaultLevel1 else colors.bgSurfaceSubtle)
                    .then(
                        if (selected) {
                            Modifier.border(
                                border = BorderStroke(SegmentSelectedBorderWidth, colors.borderBrandDefault),
                                shape = RoundedCornerShape(radius.radius12),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                DandiText(
                    text = option,
                    color = if (selected) colors.contentAccent else colors.contentDefaultLevel1,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
            }
        }
    }
}

private val SegmentTrackHeight = 52.dp
private val SegmentTrackPadding = 4.dp
private val SegmentSelectedBorderWidth = 1.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummySegmentedControlPreview() {
    DesignSystemTheme {
        NyummySegmentedControl(
            options = persistentListOf("남성", "여성"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignSystemThemeImpl.designSystemLayout.mobileGutter),
        )
    }
}
