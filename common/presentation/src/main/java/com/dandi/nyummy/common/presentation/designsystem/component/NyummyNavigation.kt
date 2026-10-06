package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 상단 바. 높이 56, 왼쪽 뒤로가기(선택), 가운데 제목(title/m), 오른쪽 슬롯(글자 버튼이나 아이콘 버튼).
 * 제목은 양옆 요소와 상관없이 화면 가운데에 둔다.
 */
@Composable
fun NyummyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val minSide = NyummyTheme.size.touchTarget
    // 제목은 화면 가운데에 두되, 양옆 슬롯 중 넓은 쪽 너비만큼 양쪽을 비워 겹치지 않게 한다.
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(NyummyComponentDimens.TopBarHeight)
            .background(NyummyTheme.colors.bg.canvas)
            .padding(horizontal = NyummyTheme.spacing.s4),
        content = {
            Box(Modifier.layoutId(TopBarStart)) {
                if (onBackClick != null) {
                    NyummyIconButton(
                        icon = R.drawable.nyummy_ic_chevron_left,
                        contentDescription = "뒤로 가기",
                        onClick = onBackClick,
                    )
                }
            }
            Box(Modifier.layoutId(TopBarEnd)) { trailing?.invoke() }
            NyummyText(
                text = title,
                style = NyummyTheme.typography.titleM,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .layoutId(TopBarTitle)
                    .semantics { heading() },
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val start = measurables.first { it.layoutId == TopBarStart }.measure(loose)
        val end = measurables.first { it.layoutId == TopBarEnd }.measure(loose)
        val side = maxOf(start.width, end.width, minSide.roundToPx())
        val titleWidth = (constraints.maxWidth - side * 2).coerceAtLeast(0)
        val titlePlaceable = measurables.first { it.layoutId == TopBarTitle }
            .measure(loose.copy(maxWidth = titleWidth))
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            start.placeRelative(0, (height - start.height) / 2)
            end.placeRelative(constraints.maxWidth - end.width, (height - end.height) / 2)
            titlePlaceable.placeRelative(
                (constraints.maxWidth - titlePlaceable.width) / 2,
                (height - titlePlaceable.height) / 2,
            )
        }
    }
}

private const val TopBarStart = "start"
private const val TopBarEnd = "end"
private const val TopBarTitle = "title"

/** 하단 내비 탭 하나. [icon]은 채색 일러스트(nyummy_nav_*), [label]은 접근성 설명도 겸한다. */
@Immutable
data class NyummyBottomNavItem(
    val label: String,
    @DrawableRes val icon: Int,
)

/**
 * 하단 내비. 화면 폭을 채우는 흰 바 + 위 1px 구분선, 그림자와 둥근 모서리 없음.
 * 탭은 아이콘 28 + 라벨 12. 선택된 탭은 60×56 연민트 상자 + 브랜드색 굵은 라벨.
 * 내비게이션 바 영역까지 흰 바탕을 이어 그린다.
 */
@Composable
fun NyummyBottomNav(
    items: ImmutableList<NyummyBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = NyummyComponentDimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface),
    ) {
        NyummyDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NyummyTheme.size.bottomNav - NyummyTheme.borderWidth.hairline)
                .padding(
                    start = NyummyTheme.spacing.s8,
                    end = NyummyTheme.spacing.s8,
                    top = dimens.BottomNavTopPadding - NyummyTheme.borderWidth.hairline,
                    bottom = NyummyTheme.spacing.s12,
                )
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 탭은 균등 너비로 나눠 360dp 이하 화면에서도 서로 겹치지 않게 한다(넓은 화면에서는 최대 70dp).
            items.forEachIndexed { index, item ->
                BottomNavTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BottomNavTab(
    item: NyummyBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = NyummyComponentDimens
    Box(
        modifier = modifier
            .height(dimens.BottomNavTabHeight)
            .selectable(
                selected = selected,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = dimens.BottomNavSelectedWidth)
                .fillMaxWidth()
                .height(dimens.BottomNavSelectedHeight)
                .background(
                    color = if (selected) NyummyTheme.colors.bg.selected else Color.Transparent,
                    shape = RoundedCornerShape(NyummyTheme.radius.m),
                ),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(dimens.BottomNavIconSize),
            )
            NyummyText(
                text = item.label,
                style = if (selected) NyummyTheme.typography.labelSStrong else NyummyTheme.typography.labelS,
                color = if (selected) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
        }
    }
}

/**
 * 카테고리 탭 바. 탭은 왼쪽부터 간격 16으로 나열되고 넘치면 가로로 스크롤된다. 아래에 1px 구분선.
 * 선택된 탭은 진한 글자 + 2px 진한 밑줄(브랜드색이 아니다), 나머지는 tertiary.
 */
@Composable
fun NyummyTabs(
    tabs: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            tabs.forEachIndexed { index, label ->
                TabItem(label = label, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
        NyummyDivider()
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val dimens = NyummyComponentDimens
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .selectable(
                selected = selected,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(
                start = NyummyTheme.spacing.s4,
                end = NyummyTheme.spacing.s4,
                top = NyummyTheme.spacing.s12,
                bottom = dimens.TabItemBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NyummyText(
            text = label,
            style = NyummyTheme.typography.labelL,
            color = if (selected) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.tertiary,
            maxLines = 1,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(dimens.TabIndicatorHeight)
                .background(
                    color = if (selected) NyummyTheme.colors.content.primary else Color.Transparent,
                    shape = RoundedCornerShape(NyummyTheme.radius.full),
                ),
        )
    }
}

/** 회원가입처럼 단계가 정해진 흐름의 진행 표시. "1 / 3" + 단계 수만큼 나뉜 막대. */
@Composable
fun NyummyStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val dimens = NyummyComponentDimens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        NyummyText(
            text = "$currentStep / $totalSteps",
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.StepBarGap),
        ) {
            repeat(totalSteps) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(dimens.StepBarHeight)
                        .background(
                            color = if (index < currentStep) {
                                NyummyTheme.colors.data.progressFill
                            } else {
                                NyummyTheme.colors.data.progressTrack
                            },
                            shape = RoundedCornerShape(NyummyTheme.radius.full),
                        ),
                )
            }
        }
    }
}

/** 냐미 하단 내비 5탭(홈, 기록, 퀘스트, 업적, 상점 순서). */
val NyummyMainTabs: ImmutableList<NyummyBottomNavItem> = persistentListOf(
    NyummyBottomNavItem(label = "홈", icon = R.drawable.nyummy_nav_home),
    NyummyBottomNavItem(label = "기록", icon = R.drawable.nyummy_nav_record),
    NyummyBottomNavItem(label = "퀘스트", icon = R.drawable.nyummy_nav_quest),
    NyummyBottomNavItem(label = "업적", icon = R.drawable.nyummy_nav_achievement),
    NyummyBottomNavItem(label = "상점", icon = R.drawable.nyummy_nav_shop),
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyNavigationPreview() {
    NyummyTheme {
        var tab by remember { mutableIntStateOf(0) }
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16)) {
            NyummyTopBar(title = "히스토리", onBackClick = {})
            NyummyTopBar(
                title = "설정",
                onBackClick = {},
                trailing = { NyummyTextButton(text = "문의하기", onClick = {}) },
            )
            NyummyTabs(
                tabs = persistentListOf("전체", "모자", "옷", "소품"),
                selectedIndex = tab % 4,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter),
            )
            NyummyStepIndicator(
                currentStep = 2,
                totalSteps = 3,
                modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter),
            )
            NyummyBottomNav(items = NyummyMainTabs, selectedIndex = tab, onSelect = { tab = it })
        }
    }
}

