package com.dandi.nyummy.history.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.util.DAYS_IN_WEEK
import com.dandi.nyummy.history.presentation.util.HISTORY_MONTH_PAGE_COUNT
import com.dandi.nyummy.history.presentation.util.historyMonthAt
import com.dandi.nyummy.history.presentation.util.historyPageOf
import com.dandi.nyummy.history.presentation.util.isAfter
import com.dandi.nyummy.history.presentation.util.weekCountOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 좌우로 넘겨 달을 바꾸는 월 캘린더. 월 이름과 요일 줄은 고정하고 날짜 6줄만 넘어간다.
 *
 * - 이번 달이 마지막 장이라 미래 달로는 넘어가지 않는다.
 * - 오늘이 아닌 날을 고르고 있거나 다른 달을 보고 있으면 "오늘로 이동" 버튼이 보인다. 누르면 이번 달 오늘로 돌아간다.
 *   버튼을 "오늘"이라고만 쓰면 지금 고른 날이 오늘이라는 표시로 읽힐 수 있어 이동한다는 말을 붙였다.
 * - 넘기다 멈추면 [onChangeMonth]로 그 달을 알린다. 보고 있는 달은 [displayedMonth]가 정하고,
 *   바깥에서 바뀌면(오늘 버튼, 다른 달 날짜 선택) 그 달로 넘겨 맞춘다.
 * - 그 달 날짜가 있는 주만 그린다(4~6주). 주 수가 다른 달로 넘기면 캘린더 높이가 부드럽게 바뀐다.
 *
 * @param calendarMonths 받아 둔 달의 칸. 아직 없는 달은 날짜만 그린다.
 */
@Composable
internal fun HistoryCalendar(
    displayedMonth: HistoryMonth,
    today: HistoryDateVO,
    selectedDate: HistoryDateVO,
    calendarMonths: ImmutableMap<HistoryMonth, ImmutableList<HistoryCalendarDayUiModel>>,
    onChangeMonth: (HistoryMonth) -> Unit,
    onClickToday: () -> Unit,
    onSelectDate: (HistoryDateVO) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentMonth = HistoryMonth.of(today)
    val pagerState = rememberPagerState(initialPage = historyPageOf(displayedMonth, currentMonth)) {
        HISTORY_MONTH_PAGE_COUNT
    }
    val visibleMonth = historyMonthAt(pagerState.currentPage, currentMonth)
    val latestOnChangeMonth by rememberUpdatedState(onChangeMonth)
    val latestCurrentMonth by rememberUpdatedState(currentMonth)
    val latestDisplayedMonth by rememberUpdatedState(displayedMonth)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            // 보고 있는 달과 같은 장에 멈춘 것(처음 그릴 때, 바깥에서 넘겨 맞춘 뒤)은 알리지 않는다.
            val month = historyMonthAt(page, latestCurrentMonth)
            if (month != latestDisplayedMonth) latestOnChangeMonth(month)
        }
    }
    LaunchedEffect(displayedMonth, currentMonth) {
        val target = historyPageOf(displayedMonth, currentMonth)
        if (pagerState.settledPage != target) pagerState.animateScrollToPage(target)
    }

    val scope = rememberCoroutineScope()
    val calendarDescription = stringResource(R.string.history_calendar_description, visibleMonth.label)
    val previousLabel = stringResource(R.string.history_calendar_previous_month)
    val nextLabel = stringResource(R.string.history_calendar_next_month)

    Column(modifier = modifier) {
        CalendarHeader(
            month = visibleMonth,
            showToday = visibleMonth != currentMonth || selectedDate != today,
            onClickToday = onClickToday,
        )
        Spacer(Modifier.height(HeaderBottomGap))
        WeekdayRow()
        Box(
            Modifier
                .fillMaxWidth()
                .height(NyummyTheme.borderWidth.hairline)
                .background(NyummyTheme.colors.border.subtle),
        )
        Spacer(Modifier.height(GridTopGap))
        val weeks = weekCountOf(visibleMonth.year, visibleMonth.month)
        val gridHeight by animateDpAsState(
            targetValue = DayCellHeight * weeks + WeekGap * (weeks - 1),
            label = "calendarHeight",
        )
        HorizontalPager(
            state = pagerState,
            key = { it },
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .semantics {
                    contentDescription = calendarDescription
                    customActions = buildList {
                        if (pagerState.currentPage > 0) {
                            add(
                                CustomAccessibilityAction(previousLabel) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    true
                                },
                            )
                        }
                        if (pagerState.currentPage < HISTORY_MONTH_PAGE_COUNT - 1) {
                            add(
                                CustomAccessibilityAction(nextLabel) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    true
                                },
                            )
                        }
                    }
                },
        ) { page ->
            val month = historyMonthAt(page, currentMonth)
            val datesOnly = remember(month) { buildCalendarDayUiModels(month.year, month.month, emptyMap()) }
            MonthGrid(
                days = calendarMonths[month] ?: datesOnly,
                today = today,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
            )
        }
        Spacer(Modifier.height(CaptionTopGap))
        RecordCaption(month = visibleMonth, currentMonth = currentMonth, days = calendarMonths[visibleMonth])
    }
}

/** 월 이름과, 오늘을 보고 있지 않을 때 나타나는 "오늘로 이동" 버튼. */
@Composable
private fun CalendarHeader(
    month: HistoryMonth,
    showToday: Boolean,
    onClickToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NyummyTheme.size.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = month.label,
            style = NyummyTheme.typography.titleL,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(visible = showToday, enter = fadeIn(), exit = fadeOut()) {
            NyummyTextButton(
                text = stringResource(R.string.history_go_today),
                onClick = onClickToday,
                tone = NyummyTextButtonTone.Neutral,
            )
        }
    }
}

/** 일요일은 빨강, 토요일은 중립 회색(info), 평일은 옅은 글자로 쓴 요일 줄. */
@Composable
private fun WeekdayRow() {
    val labels = stringArrayResource(R.array.history_weekdays)
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(WeekdayRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(
                    text = label,
                    style = NyummyTheme.typography.labelS,
                    color = when (index) {
                        0 -> NyummyTheme.colors.content.danger
                        6 -> NyummyTheme.colors.content.info
                        else -> NyummyTheme.colors.content.tertiary
                    },
                )
            }
        }
    }
}

/** 6주 날짜 칸. */
@Composable
private fun MonthGrid(
    days: ImmutableList<HistoryCalendarDayUiModel>,
    today: HistoryDateVO,
    selectedDate: HistoryDateVO,
    onSelectDate: (HistoryDateVO) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WeekGap)) {
        days.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDay(
                        day = day,
                        isToday = day.date == today,
                        isSelected = day.date == selectedDate,
                        isFuture = day.date.isAfter(today),
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = DayCellInset),
                    )
                }
            }
        }
    }
}

/**
 * 날짜 칸 하나. 오늘은 그린 원, 고른 날은 연민트 칸으로 따로 표시하고, 오늘을 고르면 둘 다 보인다.
 * 다른 달 날짜와 미래 날짜는 흐리게 그리며, 미래 날짜는 누를 수 없다.
 * 기록이 있는 날은 음식 아이콘(최대 2개)과 기록 점을 붙인다.
 */
@Composable
private fun CalendarDay(
    day: HistoryCalendarDayUiModel,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NyummyTheme.colors
    val numberColor = when {
        isToday -> colors.content.onAction
        !day.inCurrentMonth -> colors.content.disabled
        isFuture -> colors.content.tertiary
        else -> colors.content.primary
    }
    val showRecord = day.inCurrentMonth && day.hasRecord
    val description = buildString {
        append(dayLabelOf(day.date))
        if (isToday) append(", ").append(stringResource(R.string.history_calendar_day_today))
        if (showRecord) append(", ").append(stringResource(R.string.history_calendar_day_recorded))
    }
    Column(
        modifier = modifier
            .height(DayCellHeight)
            .clip(RoundedCornerShape(NyummyTheme.radius.s))
            .background(if (isSelected) colors.bg.selected else Color.Transparent)
            .nyummyClickable(onClick = onClick, enabled = !isFuture)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            }
            .padding(top = DayCellTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DayCellGap),
    ) {
        Box(
            modifier = Modifier
                .size(DayNumberSize)
                .clip(CircleShape)
                .background(if (isToday) colors.bg.actionPrimary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            NyummyText(text = day.dayLabel, style = NyummyTheme.typography.numberXs, color = numberColor)
        }
        if (showRecord) {
            Row(horizontalArrangement = Arrangement.spacedBy(FoodIconOverlap)) {
                day.foodIconIds.forEach { iconId ->
                    Box(modifier = Modifier.size(FoodIconSize), contentAlignment = Alignment.Center) {
                        HistoryFoodIcon(foodIconId = iconId, sizeFraction = 1f)
                    }
                }
            }
            Box(
                Modifier
                    .size(RecordDotSize)
                    .background(colors.data.recordMarker, CircleShape),
            )
        }
    }
}

/** 캘린더 아래 "이번 달 N일 기록했어요". 아직 받지 않은 달은 자리만 둔다. */
@Composable
private fun RecordCaption(
    month: HistoryMonth,
    currentMonth: HistoryMonth,
    days: ImmutableList<HistoryCalendarDayUiModel>?,
) {
    Row(
        modifier = Modifier.height(CaptionHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
    ) {
        if (days == null) return@Row
        val recorded = days.count { it.inCurrentMonth && it.hasRecord }
        val text = when {
            month == currentMonth && recorded > 0 -> stringResource(R.string.history_calendar_recorded_this_month, recorded)
            month == currentMonth -> stringResource(R.string.history_calendar_no_record_this_month)
            recorded > 0 -> stringResource(R.string.history_calendar_recorded_month, month.month, recorded)
            else -> stringResource(R.string.history_calendar_no_record_month, month.month)
        }
        Image(
            painter = painterResource(CommonR.drawable.nyummy_ic_calendar_days),
            contentDescription = null,
            colorFilter = ColorFilter.tint(NyummyTheme.colors.content.tertiary),
            modifier = Modifier.size(NyummyTheme.size.iconS),
        )
        NyummyText(text = text, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
    }
}

// "오늘로 이동" 버튼(터치 영역 44)이 있어 머리줄이 Figma(30)보다 높다. 그만큼 아래 간격을 줄여 요일 줄 위치를 맞춘다.
private val HeaderBottomGap = 5.dp
private val WeekdayRowHeight = 28.dp
private val GridTopGap = 6.dp
private val WeekGap = 2.dp
private val DayCellHeight = 72.dp
private val DayCellInset = 1.dp
private val DayCellTopPadding = 6.dp
private val DayCellGap = 4.dp
private val DayNumberSize = 26.dp
private val FoodIconSize = 22.dp

// 아이콘 둘을 살짝 겹쳐 칸 폭(48)에 맞춘다.
private val FoodIconOverlap = (-4).dp
private val RecordDotSize = 5.dp
private val CaptionTopGap = 16.dp
private val CaptionHeight = 18.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryCalendarPreview() {
    val today = HistoryDateVO(2026, 10, 5)
    val month = HistoryMonth.of(today)
    val records = listOf(1, 2, 3, 5).associate { day ->
        val date = HistoryDateVO(2026, 10, day)
        date to HistoryCalendarDayVO(date = date, foodIconIds = listOf("1", "2"), mealCount = 2)
    }
    NyummyTheme {
        HistoryCalendar(
            displayedMonth = month,
            today = today,
            selectedDate = today,
            calendarMonths = persistentMapOf(month to buildCalendarDayUiModels(2026, 10, records)),
            onChangeMonth = {},
            onClickToday = {},
            onSelectDate = {},
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
