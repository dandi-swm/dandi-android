package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyCalendarDay
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
import kotlinx.collections.immutable.toImmutableList

/**
 * 월 이동 헤더, 요일 헤더, 6주 날짜 그리드, 상태 범례로 이루어진 월간 캘린더 카드입니다.
 */
@Composable
internal fun HistoryCalendarCard(
    monthLabel: String,
    days: ImmutableList<HistoryCalendarDayUiModel>,
    selectedDate: HistoryDateVO,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    onSelectDate: (HistoryDateVO) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier.width(CalendarCardWidth),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20),
        color = colors.bgSurfaceCardSubtle,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(CalendarCardBorderWidth, colors.borderCalendarOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = CalendarCardInnerInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CalendarMonthHeader(
                monthLabel = monthLabel,
                onClickPreviousMonth = onClickPreviousMonth,
                onClickNextMonth = onClickNextMonth,
            )
            CalendarWeekdayHeader()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CalendarDividerHeight)
                    .background(colors.borderCalendarGrid),
            )
            days.chunked(GRID_COLUMN_COUNT).forEach { week ->
                Row {
                    week.forEach { day ->
                        NyummyCalendarDay(
                            day = day.dayLabel,
                            onClick = { onSelectDate(day.date) },
                            selected = day.inCurrentMonth && day.date == selectedDate,
                            inCurrentMonth = day.inCurrentMonth,
                            weekday = day.weekday,
                            nutritionStatus = day.nutritionStatus,
                            firstFoodIcon = day.foodIconIds.getOrNull(0)?.let { iconId ->
                                { HistoryFoodIcon(iconId) }
                            },
                            secondFoodIcon = day.foodIconIds.getOrNull(1)?.let { iconId ->
                                { HistoryFoodIcon(iconId) }
                            },
                        )
                    }
                }
            }
            CalendarLegend(
                modifier = Modifier.padding(
                    top = CalendarLegendTopGap,
                    bottom = CalendarLegendBottomGap,
                ),
            )
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthLabel: String,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CalendarHeaderVerticalGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyCalendarHeaderAction(
            direction = NyummyCalendarHeaderDirection.Previous,
            onClick = onClickPreviousMonth,
        )
        DandiText(
            text = monthLabel,
            modifier = Modifier.weight(1f),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularM,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Clip,
        )
        NyummyCalendarHeaderAction(
            direction = NyummyCalendarHeaderDirection.Next,
            onClick = onClickNextMonth,
        )
    }
}

@Composable
private fun CalendarWeekdayHeader() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(modifier = Modifier.padding(bottom = CalendarWeekdayBottomGap)) {
        WEEKDAY_LABELS.forEachIndexed { index, label ->
            DandiText(
                text = label,
                modifier = Modifier.width(CalendarCellWidth),
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

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CalendarLegendItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendItem(
            color = colors.dataEvaluationPositive,
            label = stringResource(R.string.history_legend_in_range),
        )
        CalendarLegendItem(
            color = colors.dataEvaluationNegative,
            label = stringResource(R.string.history_legend_out_of_range),
        )
        CalendarLegendItem(
            color = colors.dataEvaluationUnrecorded,
            label = stringResource(R.string.history_legend_no_record),
        )
    }
}

@Composable
private fun CalendarLegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(CalendarLegendDotSize)
                .background(color, DesignSystemThemeImpl.designSystemShape.pill),
        )
        Spacer(Modifier.width(CalendarLegendDotGap))
        DandiText(
            text = label,
            color = DesignSystemThemeImpl.designSystemColor.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
    }
}

private const val GRID_COLUMN_COUNT = 7
private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

private val CalendarCardWidth = 366.dp
private val CalendarCardBorderWidth = 1.dp
private val CalendarCardInnerInset = 8.dp
private val CalendarHeaderVerticalGap = 8.dp
private val CalendarCellWidth = 50.dp
private val CalendarWeekdayBottomGap = 6.dp
private val CalendarDividerHeight = 1.dp
private val CalendarLegendTopGap = 17.dp
private val CalendarLegendBottomGap = 14.dp
private val CalendarLegendItemGap = 18.dp
private val CalendarLegendDotSize = 6.dp
private val CalendarLegendDotGap = 6.dp

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
            onSelectDate = {},
        )
    }
}
