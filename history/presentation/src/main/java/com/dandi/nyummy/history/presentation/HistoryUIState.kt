package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.domain.HistoryErrorType
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.monthLabelOf
import com.dandi.nyummy.history.presentation.model.toCalendarNutritionStatus
import com.dandi.nyummy.history.presentation.util.isAfter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 히스토리 화면의 UI 상태입니다.
 *
 * 캘린더는 [calendarDays] 42칸을 그대로 그리며, 날짜 선택 시 [selectedDayMeals]와
 * [dailyNutrition]이 함께 바뀝니다. [mealDetail]이 null 이 아니면 식사 상세 오버레이가 열립니다.
 */
data class HistoryUIState(
    val displayedYear: Int = 0,
    val displayedMonth: Int = 0,
    val today: HistoryDateVO = HistoryDateVO.empty,
    val selectedDate: HistoryDateVO = HistoryDateVO.empty,
    val calendarDays: ImmutableList<HistoryCalendarDayUiModel> = persistentListOf(),
    val selectedDayMeals: ImmutableList<MealHistoryVO> = persistentListOf(),
    val dailyNutrition: DailyNutritionVO = DailyNutritionVO.empty,
    val isNutritionExpanded: Boolean = true,
    val isLoading: Boolean = false,
    val errorType: HistoryErrorType? = null,
    val mealDetail: HistoryMealDetailUiState? = null,
) : UiState {

    val monthLabel: String
        get() = monthLabelOf(displayedYear, displayedMonth)

    val hasNoMeals: Boolean
        get() = !isLoading && selectedDayMeals.isEmpty()

    /** 상세 오버레이가 열려 있을 때만 [transform]을 적용합니다. */
    fun withMealDetail(
        transform: (HistoryMealDetailUiState) -> HistoryMealDetailUiState,
    ): HistoryUIState = mealDetail?.let { copy(mealDetail = transform(it)) } ?: this

    /**
     * 이름 수정 입력값을 확정합니다.
     * 공백이면 상태를 바꾸지 않고, 성공 시 목록/상세에 새 이름을 반영하고 보기 모드로 돌아갑니다.
     */
    fun commitMealNameEdit(): HistoryUIState {
        val detail = mealDetail ?: return this
        val newName = detail.nameDraft.trim()
        if (newName.isEmpty()) return this
        val renamed = detail.meal.copy(name = newName)
        return copy(
            selectedDayMeals = selectedDayMeals
                .map { meal -> if (meal.id == renamed.id) renamed else meal }
                .toImmutableList(),
            mealDetail = detail.copy(
                meal = renamed,
                mode = HistoryMealDetailMode.Viewing,
                nameDraft = "",
            ),
        )
    }

    /**
     * 상세 오버레이에 열린 식사를 삭제합니다.
     * 남은 식사로 하루 영양 합계를 다시 계산하고, 해당 날짜의 캘린더 셀 표시도 갱신합니다.
     */
    fun deleteDetailMeal(): HistoryUIState {
        val target = mealDetail?.meal ?: return this
        val remaining = selectedDayMeals
            .filterNot { it.id == target.id }
            .mapIndexed { index, meal -> meal.copy(orderIndex = index + 1) }
        val totalCalorie = remaining.sumOf { it.calorieKcal }
        val hasRecord = remaining.isNotEmpty() && !selectedDate.isAfter(today)
        val cellStatus = DailyNutritionStatus.of(
            totalCalorieKcal = totalCalorie,
            targetCalorieKcal = dailyNutrition.targetCalorieKcal,
            hasRecord = hasRecord,
        ).toCalendarNutritionStatus()
        val cellIcons = remaining.take(2).map { it.foodIconId }.toImmutableList()
        return copy(
            selectedDayMeals = remaining.toImmutableList(),
            dailyNutrition = dailyNutrition.copy(
                currentCalorieKcal = totalCalorie,
                carbohydrate = dailyNutrition.carbohydrate.copy(
                    dailyGram = remaining.sumOf { it.carbohydrateGram },
                ),
                protein = dailyNutrition.protein.copy(dailyGram = remaining.sumOf { it.proteinGram }),
                fat = dailyNutrition.fat.copy(dailyGram = remaining.sumOf { it.fatGram }),
            ),
            mealDetail = null,
            calendarDays = calendarDays.map { cell ->
                if (cell.inCurrentMonth && cell.date == selectedDate) {
                    cell.copy(nutritionStatus = cellStatus, foodIconIds = cellIcons)
                } else {
                    cell
                }
            }.toImmutableList(),
        )
    }

    companion object {
        val empty = HistoryUIState()
    }
}

/**
 * 식사 상세 오버레이의 상태입니다.
 *
 * @property nameDraft 이름 수정 다이얼로그의 입력값. [HistoryMealDetailMode.EditingName]에서만 의미가 있습니다.
 */
data class HistoryMealDetailUiState(
    val meal: MealHistoryVO = MealHistoryVO.empty,
    val mode: HistoryMealDetailMode = HistoryMealDetailMode.Viewing,
    val nameDraft: String = "",
)

/** 식사 상세 오버레이가 보여주는 단계입니다. */
enum class HistoryMealDetailMode {
    /** 상세 카드 보기 */
    Viewing,

    /** 이름 수정 다이얼로그 */
    EditingName,

    /** 삭제 확인 다이얼로그 */
    ConfirmingDelete,
}
