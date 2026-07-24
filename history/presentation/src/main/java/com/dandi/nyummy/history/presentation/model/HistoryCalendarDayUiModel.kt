package com.dandi.nyummy.history.presentation.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.component.NyummyCalendarNutritionStatus
import com.dandi.nyummy.common.presentation.component.NyummyCalendarWeekday
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.util.buildCalendarCells
import com.dandi.nyummy.history.presentation.util.columnOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 캘린더 날짜 셀 하나를 그리는 데 필요한 표시 정보입니다.
 *
 * 인접 월 칸은 날짜만 보여주므로 상태 마커와 음식 아이콘을 갖지 않습니다.
 */
@Immutable
data class HistoryCalendarDayUiModel(
    val date: HistoryDateVO,
    val dayLabel: String,
    val inCurrentMonth: Boolean,
    val weekday: NyummyCalendarWeekday,
    val nutritionStatus: NyummyCalendarNutritionStatus,
    val foodIconIds: ImmutableList<String>,
)

/** 표시 월의 42칸 그리드를 기록 데이터와 합쳐 셀 표시 모델로 변환합니다. */
fun buildCalendarDayUiModels(
    year: Int,
    month: Int,
    records: Map<HistoryDateVO, HistoryCalendarDayVO>,
): ImmutableList<HistoryCalendarDayUiModel> =
    buildCalendarCells(year, month).mapIndexed { index, cell ->
        val record = if (cell.inCurrentMonth) records[cell.date] else null
        HistoryCalendarDayUiModel(
            date = cell.date,
            dayLabel = cell.date.day.toString(),
            inCurrentMonth = cell.inCurrentMonth,
            weekday = when (columnOf(index)) {
                0 -> NyummyCalendarWeekday.Sunday
                6 -> NyummyCalendarWeekday.Saturday
                else -> NyummyCalendarWeekday.Weekday
            },
            nutritionStatus = record?.status?.toCalendarNutritionStatus()
                ?: NyummyCalendarNutritionStatus.None,
            foodIconIds = (record?.foodIconIds ?: emptyList()).toImmutableList(),
        )
    }.toImmutableList()

/** 하루 영양 상태 VO 를 캘린더 셀 마커 상태로 매핑합니다. */
fun DailyNutritionStatus.toCalendarNutritionStatus(): NyummyCalendarNutritionStatus = when (this) {
    DailyNutritionStatus.IN_RANGE -> NyummyCalendarNutritionStatus.Positive
    DailyNutritionStatus.OUT_OF_RANGE -> NyummyCalendarNutritionStatus.OutOfRange
    DailyNutritionStatus.NOT_RECORDED -> NyummyCalendarNutritionStatus.NoRecord
    DailyNutritionStatus.NONE -> NyummyCalendarNutritionStatus.None
}

/** 음식 아이콘 식별자를 공용 픽셀 아이콘 리소스로 매핑합니다. */
@DrawableRes
fun foodIconResOf(foodIconId: String): Int = when (foodIconId) {
    "rice" -> R.drawable.nyummy_food_rice
    "pasta" -> R.drawable.nyummy_food_pasta
    else -> R.drawable.nyummy_food_salad
}

/** "2026년 7월" 형태의 월 라벨입니다. */
fun monthLabelOf(year: Int, month: Int): String = "${year}년 ${month}월"

/** "7월 18일" 형태의 날짜 라벨입니다. */
fun dayLabelOf(date: HistoryDateVO): String = "${date.month}월 ${date.day}일"

/** 촬영 시각 epoch millis 를 "08:10" 형태로 바꿉니다. */
fun timeLabelOf(recordedAtMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(recordedAtMillis))

/**
 * 하루 안의 순서 라벨입니다. 디자인 시안의 표기(첫 끼 → n 번째 끼니 → 마지막 끼니)를 따릅니다.
 */
fun mealOrderLabelOf(orderIndex: Int, mealCount: Int): String = when {
    orderIndex <= 1 -> "첫 끼"
    orderIndex >= mealCount -> "마지막 끼니"
    else -> "${koreanOrdinalOf(orderIndex)} 번째 끼니"
}

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

/** "5회 기록" 형태의 기록 횟수 라벨입니다. */
fun mealCountLabelOf(count: Int): String = "${count}회 기록"

/** 식사 행의 보조 정보("08:10 · 사진 기록") 라벨입니다. */
fun mealRowMetaOf(meal: MealHistoryVO): String = "${timeLabelOf(meal.recordedAtMillis)} · 사진 기록"
