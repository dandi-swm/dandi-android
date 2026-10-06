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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadge
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomNav
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCheckbox
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCodeInput
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCoinPill
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPasswordVisibilityToggle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRadio
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionCaption
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionHeader
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionHeaderWithMeta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySegmentedControl
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeleton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySkeletonShape
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStepIndicator
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStreakPill
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySwitch
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextArea
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelColumn
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelPicker
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelPickerFrame
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 새 디자인 시스템(NyummyTheme) 컴포넌트를 Figma `03 Components`와 나란히 비교하는 디버그 카탈로그.
 *
 * 실행: adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.main.presentation.catalog.NyummyDsCatalogActivity
 * 특정 섹션부터 보기: 위 명령에 `--es section "Bottom Nav"`를 붙인다.
 */
class NyummyDsCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NyummyTheme {
                NyummyDsCatalog(initialSection = intent.getStringExtra(ExtraSection))
            }
        }
    }
}

private class CatalogEntry(val title: String, val content: @Composable ColumnScope.() -> Unit)

/** 섹션 순서. 새 컴포넌트 PR마다 여기에 섹션을 더한다. */
private val CatalogEntries = listOf(
    CatalogEntry("Button") { ButtonSection() },
    CatalogEntry("Text Button") { TextButtonSection() },
    CatalogEntry("Icon Button") { IconButtonSection() },
    CatalogEntry("Chip") { ChipSection() },
    CatalogEntry("Bottom CTA") { BottomCtaSection() },
    CatalogEntry("Text Field") { TextFieldSection() },
    CatalogEntry("Text Area") { TextAreaSection() },
    CatalogEntry("Code Input") { CodeInputSection() },
    CatalogEntry("Segmented Control") { SegmentedSection() },
    CatalogEntry("Wheel Picker") { WheelPickerSection() },
    CatalogEntry("Checkbox, Radio, Switch") { SelectionSection() },
    CatalogEntry("Card") { CardSection() },
    CatalogEntry("List Row") { ListRowSection() },
    CatalogEntry("Badge, HUD Pill") { BadgeHudSection() },
    CatalogEntry("Progress") { ProgressSection() },
    CatalogEntry("Section Header, Divider, Skeleton") { StructureSection() },
    CatalogEntry("Top Bar, Tabs, Step Indicator") { NavigationSection() },
    CatalogEntry("Bottom Nav") { BottomNavSection() },
)

private const val ExtraSection = "section"

@Composable
private fun NyummyDsCatalog(initialSection: String?) {
    val initialIndex = CatalogEntries.indexOfFirst { it.title == initialSection }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .statusBarsPadding(),
    ) {
        items(CatalogEntries, key = { it.title }) { entry -> CatalogSection(entry.title, entry.content) }
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

@Composable
private fun TextFieldSection() {
    var email by remember { mutableStateOf("") }
    NyummyTextField(value = email, onValueChange = { email = it }, label = "이메일", placeholder = "example@nyummy.com", helperText = "도움말")
    NyummyTextField(value = "nyummy@", onValueChange = {}, label = "이메일", errorMessage = "이메일 형식을 확인해 주세요")
    var password by remember { mutableStateOf("password") }
    var visible by remember { mutableStateOf(false) }
    NyummyTextField(
        value = password,
        onValueChange = { password = it },
        label = "비밀번호",
        leadingIcon = R.drawable.nyummy_ic_user_round,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = { NyummyPasswordVisibilityToggle(visible = visible, onToggle = { visible = !visible }) },
    )
    NyummyTextField(value = "", onValueChange = {}, label = "이메일", placeholder = "example@nyummy.com", helperText = "도움말", enabled = false)
}

@Composable
private fun TextAreaSection() {
    var text by remember { mutableStateOf("") }
    NyummyTextArea(value = text, onValueChange = { text = it }, label = "문의 내용", placeholder = "어떤 점이 궁금하거나 불편했는지 알려 주세요")
    NyummyTextArea(value = "", onValueChange = {}, label = "문의 내용", placeholder = "어떤 점이 궁금하거나 불편했는지 알려 주세요", errorMessage = "내용을 입력해 주세요")
}

@Composable
private fun CodeInputSection() {
    var code by remember { mutableStateOf("427") }
    NyummyCodeInput(value = code, onValueChange = { code = it })
    NyummyCodeInput(value = "427915", onValueChange = {}, isError = true)
}

@Composable
private fun SegmentedSection() {
    var selected by remember { mutableIntStateOf(0) }
    NyummySegmentedControl(
        options = persistentListOf("남성", "여성"),
        selectedIndex = selected,
        onSelect = { selected = it },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WheelPickerSection() {
    val heights = remember { (140..200).map(Int::toString).toImmutableList() }
    var height by remember { mutableIntStateOf(32) }
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
        NyummyWheelPicker(
            items = heights,
            selectedIndex = height,
            onSelectedIndexChange = { height = it },
            unit = "cm",
            modifier = Modifier.weight(1f),
        )
        val hours = remember { (0..23).map { "%02d".format(it) }.toImmutableList() }
        val minutes = remember { (0..50 step 10).map { "%02d".format(it) }.toImmutableList() }
        NyummyWheelPickerFrame(modifier = Modifier.weight(1f)) {
            NyummyWheelColumn(items = hours, selectedIndex = 8, onSelectedIndexChange = {}, modifier = Modifier.weight(1f))
            NyummyWheelColumn(items = minutes, selectedIndex = 3, onSelectedIndexChange = {}, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SelectionSection() {
    var checked by remember { mutableStateOf(true) }
    var radio by remember { mutableIntStateOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyCheckbox(checked = checked, onCheckedChange = { checked = it })
        NyummyCheckbox(checked = !checked, onCheckedChange = { checked = !it })
        NyummyCheckbox(checked = false, onCheckedChange = {}, enabled = false)
        NyummyCheckbox(checked = true, onCheckedChange = {}, enabled = false)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyRadio(selected = radio == 0, onClick = { radio = 0 })
        NyummyRadio(selected = radio == 1, onClick = { radio = 1 })
        NyummyRadio(selected = false, onClick = {}, enabled = false)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummySwitch(checked = checked, onCheckedChange = { checked = it })
        NyummySwitch(checked = !checked, onCheckedChange = { checked = !it })
        NyummySwitch(checked = false, onCheckedChange = {}, enabled = false)
        NyummySwitch(checked = true, onCheckedChange = {}, enabled = false)
    }
}

@Composable
private fun CardSection() {
    NyummyCard(title = "오늘 1개 기록했어요", body = "칼로리와 탄단지는 참고로만 보여 줄게요.", modifier = Modifier.fillMaxWidth())
    NyummyCard(title = "오늘 1개 기록했어요", body = "칼로리와 탄단지는 참고로만 보여 줄게요.", onClick = {}, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ListRowSection() {
    var alarm by remember { mutableStateOf(true) }
    var agree by remember { mutableStateOf(true) }
    var choice by remember { mutableIntStateOf(0) }
    Column {
        NyummyListRow(title = "알림", subtitle = "끼니 시간에 알려 드려요", leadingIcon = R.drawable.nyummy_ic_bell, onClick = {})
        NyummyListRow(
            title = "알림",
            subtitle = "끼니 시간에 알려 드려요",
            leadingIcon = R.drawable.nyummy_ic_bell,
            trailing = NyummyListRowTrailing.Switch(alarm) { alarm = it },
        )
        NyummyListRow(
            title = "알림",
            subtitle = "끼니 시간에 알려 드려요",
            leadingIcon = R.drawable.nyummy_ic_bell,
            trailing = NyummyListRowTrailing.Value("v1.2.0"),
            onClick = {},
        )
        NyummyListRow(
            title = "알림",
            subtitle = "끼니 시간에 알려 드려요",
            leadingIcon = R.drawable.nyummy_ic_bell,
            trailing = NyummyListRowTrailing.Checkbox(agree) { agree = it },
        )
        NyummyListRow(title = "알림", subtitle = "끼니 시간에 알려 드려요", leadingIcon = R.drawable.nyummy_ic_bell, trailing = NyummyListRowTrailing.None)
        NyummyListRow(
            title = "알림",
            subtitle = "끼니 시간에 알려 드려요",
            leadingIcon = R.drawable.nyummy_ic_bell,
            trailing = NyummyListRowTrailing.Radio(choice == 0) { choice = 0 },
        )
        NyummyDivider()
        NyummyListRow(title = "다른 선택지", trailing = NyummyListRowTrailing.Radio(choice == 1) { choice = 1 })
    }
}

@Composable
private fun BadgeHudSection() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        NyummyBadgeTone.entries.forEach { NyummyBadge(text = "대기", tone = it) }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
        NyummyCoinPill(coins = "1,240")
        NyummyStreakPill(days = 7)
        NyummyCoinPill(coins = "1,240", onAddClick = {})
    }
}

@Composable
private fun ProgressSection() {
    listOf(0f, 0.1f, 0.45f, 1f).forEach { NyummyLinearProgress(progress = it, modifier = Modifier.fillMaxWidth()) }
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16)) {
        NyummyCircularProgressSize.entries.forEach { NyummyCircularProgress(size = it) }
    }
}

@Composable
private fun StructureSection() {
    Column {
        NyummySectionHeader(title = "오늘의 식사", actionText = "전체보기")
        NyummySectionCaption(title = "오늘의 식사", actionText = "전체보기")
        NyummySectionHeaderWithMeta(title = "오늘의 식사", meta = "6시간 남음")
    }
    NyummyDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16)) {
        NyummySkeleton(NyummySkeletonShape.Circle, Modifier.size(NyummyTheme.size.characterXs))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummySkeleton(NyummySkeletonShape.Line, Modifier.fillMaxWidth(0.6f))
            NyummySkeleton(NyummySkeletonShape.Block, Modifier.fillMaxWidth().height(NyummyTheme.size.bottomNav))
        }
    }
}

@Composable
private fun NavigationSection() {
    NyummyTopBar(title = "히스토리", onBackClick = {})
    NyummyTopBar(title = "설정", onBackClick = {}, trailing = { NyummyTextButton(text = "문의하기", onClick = {}) })
    var tab by remember { mutableIntStateOf(0) }
    NyummyTabs(tabs = persistentListOf("전체", "모자", "옷", "소품"), selectedIndex = tab, onSelect = { tab = it })
    var step by remember { mutableIntStateOf(1) }
    NyummyStepIndicator(currentStep = step, totalSteps = 3)
    NyummyTextButton(text = "다음 단계", onClick = { step = step % 3 + 1 })
}

@Composable
private fun BottomNavSection() {
    var selected by remember { mutableIntStateOf(0) }
    NyummyBottomNav(items = NyummyMainTabs, selectedIndex = selected, onSelect = { selected = it })
}
