package com.dandi.nyummy.history.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.util.buildCalendarCells
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 캘린더 날짜 칸 하나를 그리는 데 필요한 표시 정보입니다.
 *
 * 다른 달 칸은 날짜만 보여 주므로 기록 표시와 음식 아이콘을 갖지 않습니다.
 *
 * @property hasRecord 그날 식사를 한 번이라도 기록했다(칸 아래 기록 점)
 * @property foodIconIds 칸에 보여 줄 음식 아이콘, 최대 2개
 */
@Immutable
data class HistoryCalendarDayUiModel(
    val date: HistoryDateVO,
    val dayLabel: String,
    val inCurrentMonth: Boolean,
    val hasRecord: Boolean,
    val foodIconIds: ImmutableList<String>,
)

/** 표시 월의 캘린더 칸(그 달 날짜가 있는 주만)을 기록 데이터와 합쳐 칸 표시 모델로 바꿉니다. */
fun buildCalendarDayUiModels(
    year: Int,
    month: Int,
    records: Map<HistoryDateVO, HistoryCalendarDayVO>,
): ImmutableList<HistoryCalendarDayUiModel> =
    buildCalendarCells(year, month).map { cell ->
        val record = if (cell.inCurrentMonth) records[cell.date] else null
        val icons = record?.foodIconIds.orEmpty()
        HistoryCalendarDayUiModel(
            date = cell.date,
            dayLabel = cell.date.day.toString(),
            inCurrentMonth = cell.inCurrentMonth,
            // 월간 데이터는 기록이 있는 날만 담는다. 기록 판정은 기록 행위 기준이다.
            hasRecord = record != null,
            foodIconIds = icons.toImmutableList(),
        )
    }.toImmutableList()

/** 기기 언어의 월 라벨입니다(한국어 "2026년 7월"). */
@Composable
fun monthLabelOf(month: HistoryMonth): String =
    formatDateLabel(LocalDate.of(month.year, month.month, 1), stringResource(R.string.history_month_label_pattern), currentLocale())

/** 기기 언어의 날짜 라벨입니다(한국어 "7월 18일"). */
@Composable
fun dayLabelOf(date: HistoryDateVO): String =
    formatDateLabel(date.toLocalDate(), stringResource(R.string.history_day_label_pattern), currentLocale())

/** 요일까지 포함한 선택일 제목입니다(한국어 "9월 8일 화요일"). */
@Composable
fun dayTitleOf(date: HistoryDateVO): String =
    formatDateLabel(date.toLocalDate(), stringResource(R.string.history_day_title_pattern), currentLocale())

/** 짧은 선택일 제목입니다(한국어 "10. 5 (월)"). */
@Composable
fun dayShortTitleOf(date: HistoryDateVO): String =
    formatDateLabel(date.toLocalDate(), stringResource(R.string.history_day_short_title_pattern), currentLocale())

/** "12:30" 시각을 기기 언어의 시각 라벨로 바꿉니다(한국어 "오후 12:30"). 형식이 다르면 원문을 그대로 돌려줍니다. */
@Composable
fun meridiemTimeOf(time: String): String =
    formatMealTime(time, stringResource(R.string.history_meal_time_pattern), currentLocale())

/**
 * 하루 안의 순서 라벨입니다. Figma 표기를 따라 첫 끼, 두 번째 끼니, 세 번째 끼니로 쓰고,
 * 준비한 서수 라벨을 넘어가면 숫자를 붙여 "11번째 끼니"로 씁니다.
 */
@Composable
fun mealOrderLabelOf(orderIndex: Int): String = mealOrderLabel(
    orderIndex = orderIndex,
    ordinalLabels = stringArrayResource(R.array.history_meal_order_labels).asList(),
    numberedLabel = stringResource(R.string.history_meal_order_numbered, orderIndex),
)

/** [pattern](java.time 패턴)으로 [date]를 [locale] 언어로 씁니다. 요일·월 이름이 [locale]을 따릅니다. */
fun formatDateLabel(date: LocalDate, pattern: String, locale: Locale): String =
    DateTimeFormatter.ofPattern(pattern, locale).format(date)

/** "12:30" 시각을 [pattern]으로 다시 씁니다. 형식이 다르면 원문을 그대로 돌려줍니다. */
fun formatMealTime(time: String, pattern: String, locale: Locale): String {
    val hour = time.substringBefore(':').toIntOrNull() ?: return time
    val minute = time.substringAfter(':', missingDelimiterValue = "").toIntOrNull() ?: return time
    if (hour !in 0..23 || minute !in 0..59) return time
    return DateTimeFormatter.ofPattern(pattern, locale).format(LocalTime.of(hour, minute))
}

/** 순번이 없거나 1이면 첫 라벨, [ordinalLabels] 안이면 그 라벨, 넘어가면 [numberedLabel]입니다. */
fun mealOrderLabel(orderIndex: Int, ordinalLabels: List<String>, numberedLabel: String): String = when {
    ordinalLabels.isEmpty() -> numberedLabel
    orderIndex <= 1 -> ordinalLabels.first()
    orderIndex <= ordinalLabels.size -> ordinalLabels[orderIndex - 1]
    else -> numberedLabel
}

private fun HistoryDateVO.toLocalDate(): LocalDate = LocalDate.of(year, month, day)

/** 문자열 리소스를 고른 것과 같은 기기 언어. 요일·월 이름·오전/오후를 이 언어로 쓴다. */
@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

/** 천 단위 구분 기호가 들어간 숫자 라벨("2,129")입니다. */
fun numberLabelOf(value: Int): String = String.format(Locale.KOREA, "%,d", value)
