package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyCalendarHeaderAction
import com.dandi.nyummy.common.presentation.component.NyummyCalendarHeaderDirection
import com.dandi.nyummy.common.presentation.component.NyummyCalendarWeekday
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import kotlinx.collections.immutable.ImmutableList

/**
 * 월 이동 헤더(오늘 칩 포함), 요일 헤더, 6주 날짜 그리드, 하단 캡션으로 이루어진 월간 캘린더입니다.
 *
 * 카드 컨테이너 없이 화면 배경 위에 플랫하게 놓이고, 기록이 있는 날은 음식 아이콘과
 * 초록 도트로 표시한다. 선택한 날은 소프트 그린 블록 + 진초록 원 안의 날짜로 강조한다.
 */
@Composable
internal fun HistoryCalendarCard(
    monthLabel: String,
    days: ImmutableList<HistoryCalendarDayUiModel>,
    selectedDate: HistoryDateVO,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    onClickToday: () -> Unit,
    onSelectDate: (HistoryDateVO) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
    ) {
        CalendarMonthHeader(
            monthLabel = monthLabel,
            onClickPreviousMonth = onClickPreviousMonth,
            onClickNextMonth = onClickNextMonth,
            onClickToday = onClickToday,
        )
        Spacer(Modifier.height(CalendarHeaderBottomGap))
        CalendarWeekdayHeader()
        Spacer(Modifier.height(CalendarWeekdayBottomGap))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CalendarDividerHeight)
                .background(colors.borderCalendarGrid),
        )
        Spacer(Modifier.height(CalendarGridTopGap))
        days.chunked(GRID_COLUMN_COUNT).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    HistoryCalendarDayCell(
                        day = day,
                        selected = day.inCurrentMonth && day.date == selectedDate,
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(CalendarCaptionTopGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_history_bowl),
                contentDescription = null,
                modifier = Modifier.size(CaptionIconSize),
                tint = colors.contentDefaultLevel2,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            DandiText(
                text = stringResource(R.string.history_calendar_caption),
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthLabel: String,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    onClickToday: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DandiText(
            text = monthLabel,
            modifier = Modifier.weight(1f),
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularL,
            overflow = TextOverflow.Clip,
        )
        Box(
            modifier = Modifier
                .clip(DesignSystemThemeImpl.designSystemShape.pill)
                .background(colors.bgSuccessSoft)
                .clickable(role = Role.Button, onClick = onClickToday)
                .padding(
                    horizontal = DesignSystemThemeImpl.designSystemSpacing.space12,
                    vertical = DesignSystemThemeImpl.designSystemSpacing.space8,
                ),
        ) {
            DandiText(
                text = stringResource(R.string.history_today_chip),
                color = colors.contentAccentSage,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
        }
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
        NyummyCalendarHeaderAction(
            direction = NyummyCalendarHeaderDirection.Previous,
            onClick = onClickPreviousMonth,
        )
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
        NyummyCalendarHeaderAction(
            direction = NyummyCalendarHeaderDirection.Next,
            onClick = onClickNextMonth,
        )
    }
}

@Composable
private fun CalendarWeekdayHeader() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAY_LABELS.forEachIndexed { index, label ->
            DandiText(
                text = label,
                modifier = Modifier.weight(1f),
                color = when (index) {
                    0 -> colors.contentCalendarSunday
                    WEEKDAY_LABELS.lastIndex -> colors.contentCalendarSaturday
                    else -> colors.contentCalendarWeekday
                },
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 날짜 셀. 날짜 숫자 아래에 기록 음식 아이콘(최대 2개)과 기록 도트를 쌓는다.
 * 선택된 날은 셀 전체를 소프트 그린 라운드 블록으로 감싸고 날짜를 진초록 원으로 강조한다.
 */
@Composable
private fun HistoryCalendarDayCell(
    day: HistoryCalendarDayUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val hasRecord = day.foodIconIds.isNotEmpty()
    val dateColor = when {
        !day.inCurrentMonth -> colors.contentCalendarAdjacentMonth
        day.weekday == NyummyCalendarWeekday.Sunday -> colors.contentCalendarSunday
        day.weekday == NyummyCalendarWeekday.Saturday -> colors.contentCalendarSaturday
        else -> colors.contentCalendarDate
    }

    Column(
        modifier = modifier
            .height(CalendarCellHeight)
            .padding(CalendarCellPadding)
            .clip(RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius12))
            .background(if (selected) colors.bgCalendarSelected else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CalendarCellTopGap))
        Box(
            modifier = Modifier
                .size(CalendarDateCircleSize)
                .clip(CircleShape)
                .background(if (selected) colors.dataCalendarToday else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = day.dayLabel,
                color = if (selected) colors.contentInverseDefault else dateColor,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
        }
        if (hasRecord) {
            Spacer(Modifier.height(CalendarIconTopGap))
            Row(horizontalArrangement = Arrangement.spacedBy(CalendarIconGap)) {
                day.foodIconIds.forEach { iconId ->
                    Box(
                        modifier = Modifier.size(CalendarFoodIconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        HistoryFoodIcon(iconId, sizeFraction = CalendarFoodIconFraction)
                    }
                }
            }
            Spacer(Modifier.height(CalendarDotTopGap))
            Box(
                modifier = Modifier
                    .size(CalendarRecordDotSize)
                    .background(colors.dataCalendarRecorded, CircleShape),
            )
        }
    }
}

private const val GRID_COLUMN_COUNT = 7
private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

private val CalendarHeaderBottomGap = 16.dp
private val CalendarWeekdayBottomGap = 8.dp
private val CalendarDividerHeight = 1.dp
private val CalendarGridTopGap = 4.dp
private val CalendarCellHeight = 74.dp
private val CalendarCellPadding = 2.dp
private val CalendarCellTopGap = 4.dp
private val CalendarDateCircleSize = 20.dp
private val CalendarIconTopGap = 1.dp
private val CalendarIconGap = 1.dp
private val CalendarFoodIconSize = 24.dp
private const val CalendarFoodIconFraction = 0.95f
private val CalendarDotTopGap = 2.dp
private val CalendarRecordDotSize = 5.dp
private val CalendarCaptionTopGap = 8.dp
private val CaptionIconSize = 18.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryCalendarCardPreview() {
    DesignSystemTheme {
        HistoryCalendarCard(
            monthLabel = "2026년 7월",
            days = buildCalendarDayUiModels(2026, 7, emptyMap()),
            selectedDate = HistoryDateVO(2026, 7, 18),
            onClickPreviousMonth = {},
            onClickNextMonth = {},
            onClickToday = {},
            onSelectDate = {},
        )
    }
}
