package com.dandi.nyummy.history.presentation.model

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.util.buildCalendarCells
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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

/** "2026년 7월" 형태의 월 라벨입니다. */
fun monthLabelOf(year: Int, month: Int): String = "${year}년 ${month}월"

/** 서버와 주고받는 "yyyy-MM-dd" 형태의 날짜 문자열입니다(푸시 payload 비교용). */
fun isoDateOf(date: HistoryDateVO): String =
    String.format(Locale.KOREA, "%04d-%02d-%02d", date.year, date.month, date.day)

/** "7월 18일" 형태의 날짜 라벨입니다. */
fun dayLabelOf(date: HistoryDateVO): String = "${date.month}월 ${date.day}일"

/** "9월 8일 화요일" 형태로 요일까지 포함한 선택일 제목입니다. */
fun dayTitleOf(date: HistoryDateVO): String {
    // 기기 타임존에 의존하지 않는 순수 요일 계산(0 = 일요일).
    val weekday = WEEKDAY_TITLE_LABELS[
        KstTime.sundayBasedWeekdayOf(date.year, date.month, date.day),
    ]
    return "${dayLabelOf(date)} $weekday"
}

private val WEEKDAY_TITLE_LABELS =
    listOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")

/** "12:30" 시각을 "오후 12:30" 형태로 바꿉니다. 형식이 다르면 원문을 그대로 돌려줍니다. */
fun meridiemTimeOf(time: String): String {
    val hour = time.substringBefore(':').toIntOrNull() ?: return time
    val minute = time.substringAfter(':', missingDelimiterValue = "").ifBlank { return time }
    return when {
        hour == 0 -> "오전 12:$minute"
        hour < 12 -> "오전 $hour:$minute"
        hour == 12 -> "오후 12:$minute"
        else -> "오후 ${hour - 12}:$minute"
    }
}

/** 하루 안의 순서 라벨입니다. Figma 표기를 따라 첫 끼, 두 번째 끼니, 세 번째 끼니로 씁니다. */
fun mealOrderLabelOf(orderIndex: Int): String =
    if (orderIndex <= 1) "첫 끼" else "${koreanOrdinalOf(orderIndex)} 번째 끼니"

private fun koreanOrdinalOf(index: Int): String = when (index) {
    2 -> "두"
    3 -> "세"
    4 -> "네"
    5 -> "다섯"
    else -> "$index"
}

/** 목표 대비 백분율 정수를 계산합니다. 목표가 0이면 0을 돌려줍니다. */
fun percentOf(current: Int, goal: Int): Int =
    if (goal <= 0) 0 else current * 100 / goal

/** 진행 바에 쓰는 0f..1f 비율입니다. */
fun progressOf(current: Int, goal: Int): Float =
    if (goal <= 0) 0f else current.toFloat() / goal

/** 천 단위 구분 기호가 들어간 숫자 라벨("2,129")입니다. */
fun numberLabelOf(value: Int): String = String.format(Locale.KOREA, "%,d", value)
