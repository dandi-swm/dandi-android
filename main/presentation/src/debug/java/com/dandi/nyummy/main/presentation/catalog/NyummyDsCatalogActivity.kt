package com.dandi.nyummy.main.presentation.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 새 디자인 시스템(NyummyTheme) 컴포넌트를 Figma `03 Components`와 나란히 비교하는 디버그 카탈로그.
 *
 * 실행: adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.main.presentation.catalog.NyummyDsCatalogActivity
 */
class NyummyDsCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NyummyTheme {
                NyummyDsCatalog()
            }
        }
    }
}

@Composable
private fun NyummyDsCatalog() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .statusBarsPadding(),
    ) {
        item { CatalogSection("Button") { ButtonSection() } }
        item { CatalogSection("Text Button") { TextButtonSection() } }
        item { CatalogSection("Icon Button") { IconButtonSection() } }
        item { CatalogSection("Chip") { ChipSection() } }
        item { CatalogSection("Bottom CTA") { BottomCtaSection() } }
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        NyummyText(text = title, style = NyummyTheme.typography.titleM)
        content()
    }
}

@Composable
private fun ButtonSection() {
    NyummyButtonStyle.entries.forEach { style ->
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyButtonSize.entries.forEach { size ->
                NyummyButton(text = "밥 주기", onClick = {}, style = style, size = size)
            }
            NyummyButton(text = "밥 주기", onClick = {}, style = style, size = NyummyButtonSize.M, enabled = false)
        }
    }
    NyummyButton(
        text = "사진 추가",
        onClick = {},
        icon = R.drawable.nyummy_ic_image_plus,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextButtonSection() {
    NyummyTextButtonTone.entries.forEach { tone ->
        FlowRow {
            NyummyTextButton("지금 기록하기", {}, tone = tone, trailingIcon = R.drawable.nyummy_ic_chevron_right)
            NyummyTextButton("더보기", {}, tone = tone, size = NyummyTextButtonSize.S)
            NyummyTextButton("접기", {}, tone = tone, enabled = false)
        }
    }
}

@Composable
private fun IconButtonSection() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16)) {
        NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {})
        NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {}, style = NyummyIconButtonStyle.Filled)
        NyummyIconButton(R.drawable.nyummy_ic_settings, "설정", {}, enabled = false)
    }
}

@Composable
private fun ChipSection() {
    var selected by remember { mutableStateOf("아침") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        listOf("아침", "점심", "저녁").forEach { meal ->
            NyummyChip(text = meal, selected = meal == selected, onClick = { selected = meal })
        }
    }
}

@Composable
private fun BottomCtaSection() {
    NyummyBottomCta(primaryText = "확인", onPrimaryClick = {})
    NyummyBottomCta(primaryText = "확인", onPrimaryClick = {}, secondaryText = "닫기")
}
