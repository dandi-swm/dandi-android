package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentShadows
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 상단 바. 높이 56, 왼쪽 뒤로가기(선택), 가운데 제목(title/s), 오른쪽 슬롯(글자 버튼이나 아이콘 버튼).
 * 제목은 양옆 요소와 상관없이 화면 가운데에 둔다.
 *
 * @param containerColor 바탕색. 마이룸처럼 그림 위에 띄울 때는 투명으로 둔다.
 */
@Composable
fun NyummyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = NyummyTheme.colors.bg.canvas,
) {
    val minSide = NyummyTheme.size.touchTarget
    // 제목은 화면 가운데에 두되, 양옆 슬롯 중 넓은 쪽 너비만큼 양쪽을 비워 겹치지 않게 한다.
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(NyummyComponentDimens.TopBarHeight)
            .background(containerColor)
            .padding(start = NyummyTheme.spacing.s8, end = NyummyTheme.spacing.s16),
        content = {
            Box(Modifier.layoutId(TopBarStart)) {
                if (onBackClick != null) {
                    NyummyIconButton(
                        icon = R.drawable.nyummy_ic_chevron_left,
                        contentDescription = stringResource(R.string.nyummy_back_description),
                        onClick = onBackClick,
                    )
                }
            }
            Box(Modifier.layoutId(TopBarEnd)) { trailing?.invoke() }
            NyummyText(
                text = title,
                style = NyummyTheme.typography.titleS,
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

/** 하단 내비 탭 하나. [icon]은 단색 채움 아이콘(nyummy_nav_*)이고 켜짐·꺼짐 색으로 칠한다. [label]은 접근성 설명도 겸한다. */
@Immutable
data class NyummyBottomNavItem(
    val label: String,
    @DrawableRes val icon: Int,
)

/** 하단 내비 가운데 동작 버튼. 탭이 아니라서 선택 상태가 없고, 누르면 바로 그 동작을 한다. [label]은 접근성 설명이다. */
@Immutable
data class NyummyBottomNavAction(
    val label: String,
    @DrawableRes val icon: Int,
)

/**
 * 하단 내비. 화면 폭을 채우는 흰 바, 위쪽 모서리 20, 위로 퍼지는 옅은 그림자.
 * 탭은 단색 채움 아이콘 24 + 라벨(label/s)이고, 켜진 탭만 브랜드색(nav/icon-on), 나머지는 nav/icon-off다.
 * 내비게이션 바 영역까지 흰 바탕을 이어 그린다.
 *
 * [centerAction]이 있으면 [centerActionIndex]번째 탭 앞에 54×54 초록 버튼(모서리 16, 초록 그림자)을 바 안에 둔다.
 * 크기는 화면 크기와 상관없이 dp로 고정이다.
 *
 * @param centerActionIndex 가운데 버튼 앞에 올 탭 수. 탭 번호([selectedIndex], [onSelect])는 가운데 버튼을 빼고 센다.
 * @param tabModifier 탭마다 덧붙일 Modifier(코치마크 대상 등록 등). 탭 번호를 받는다.
 * @param centerActionModifier 가운데 버튼에 덧붙일 Modifier(코치마크 대상 등록 등).
 */
@Composable
fun NyummyBottomNav(
    items: ImmutableList<NyummyBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: NyummyBottomNavAction? = null,
    centerActionIndex: Int = (items.size + 1) / 2,
    onCenterActionClick: () -> Unit = {},
    tabModifier: @Composable (Int) -> Modifier = { Modifier },
    centerActionModifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = NyummyTheme.radius.ml, topEnd = NyummyTheme.radius.ml)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nyummyShadow(shape, NyummyComponentShadows.BottomNav)
            .background(NyummyTheme.colors.bg.surface, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NyummyTheme.size.bottomNav)
                .padding(horizontal = NyummyTheme.spacing.s12)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 탭과 가운데 버튼은 균등 너비로 나눠 360dp 이하 화면에서도 서로 겹치지 않게 한다.
            items.forEachIndexed { index, item ->
                if (centerAction != null && index == centerActionIndex) {
                    BottomNavCenterAction(
                        action = centerAction,
                        onClick = onCenterActionClick,
                        modifier = Modifier.weight(1f),
                        buttonModifier = centerActionModifier,
                    )
                }
                BottomNavTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f).then(tabModifier(index)),
                )
            }
            if (centerAction != null && centerActionIndex >= items.size) {
                BottomNavCenterAction(
                        action = centerAction,
                        onClick = onCenterActionClick,
                        modifier = Modifier.weight(1f),
                        buttonModifier = centerActionModifier,
                    )
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

/** 하단 내비 가운데 버튼. 바 안의 54×54 초록 버튼(모서리 16)에 흰 채움 아이콘, 아래로 초록 그림자. */
@Composable
private fun BottomNavCenterAction(
    action: NyummyBottomNavAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
) {
    val dimens = NyummyComponentDimens
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .then(buttonModifier)
                .nyummyClickable(onClick = onClick, interactionSource = interactionSource)
                .semantics { contentDescription = action.label }
                .size(dimens.BottomNavActionSize)
                .nyummyShadow(shape, NyummyComponentShadows.BottomNavAction)
                .background(
                    color = if (pressed) NyummyTheme.colors.bg.actionPrimaryPressed else NyummyTheme.colors.bg.actionPrimary,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(action.icon),
                contentDescription = null,
                tint = NyummyTheme.colors.content.onAction,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        }
    }
}

@Composable
private fun BottomNavTab(
    item: NyummyBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) NyummyTheme.colors.nav.iconOn else NyummyTheme.colors.nav.iconOff
    Column(
        modifier = modifier
            .height(NyummyTheme.size.touchTarget)
            .selectable(
                selected = selected,
                interactionSource = rememberNyummyInteractionSource(),
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(NyummyTheme.size.iconL),
        )
        NyummyText(text = item.label, style = NyummyTheme.typography.labelS, color = color, maxLines = 1)
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
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
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

/**
 * 냐미 하단 내비 탭(홈, 마이룸, 기록, 상점 순서). 마이룸과 상점은 기능이 켜졌을 때만 앱이 골라 보여 준다.
 * 가운데 식사 기록 버튼([NyummyMealRecordAction])은 기록 탭 앞에 둔다.
 */
val NyummyMainTabs: ImmutableList<NyummyBottomNavItem> = persistentListOf(
    NyummyBottomNavItem(label = "홈", icon = R.drawable.nyummy_nav_home),
    NyummyBottomNavItem(label = "마이룸", icon = R.drawable.nyummy_nav_my_room),
    NyummyBottomNavItem(label = "기록", icon = R.drawable.nyummy_nav_record),
    NyummyBottomNavItem(label = "상점", icon = R.drawable.nyummy_nav_shop),
)

/** 하단 내비 가운데 식사 기록 버튼. */
val NyummyMealRecordAction = NyummyBottomNavAction(label = "식사 기록", icon = R.drawable.nyummy_nav_camera)

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
            NyummyBottomNav(
                items = NyummyMainTabs,
                selectedIndex = tab,
                onSelect = { tab = it },
                centerAction = NyummyMealRecordAction,
                centerActionIndex = 2,
            )
        }
    }
}

