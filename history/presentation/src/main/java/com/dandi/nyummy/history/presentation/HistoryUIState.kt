package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.DailyNutritionStatus
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
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
     * [mealId] 식사의 이름 수정 저장 결과를 반영합니다.
     * 목록에서는 해당 식사만 rename 하고, 상세 오버레이는 같은 식사가 열려 있을 때만 갱신합니다 —
     * 응답 도착 시점에 다른 식사가 열려 있어도 그 식사의 draft 를 건드리지 않습니다.
     */
    fun commitMealNameEdit(mealId: String, newName: String): HistoryUIState {
        if (newName.isEmpty()) return this
        val detail = mealDetail
        return copy(
            selectedDayMeals = selectedDayMeals
                .map { meal -> if (meal.id == mealId) meal.copy(name = newName) else meal }
                .toImmutableList(),
            mealDetail = if (detail != null && detail.meal.id == mealId) {
                detail.copy(
                    meal = detail.meal.copy(name = newName),
                    mode = HistoryMealDetailMode.Viewing,
                    nameDraft = "",
                    isActionInFlight = false,
                )
            } else {
                detail
            },
        )
    }

    /**
     * [mealId] 식사의 삭제 완료를 반영합니다.
     * 남은 식사로 하루 영양 합계를 다시 계산하고, 해당 날짜의 캘린더 셀 표시도 갱신합니다.
     * 상세 오버레이는 같은 식사가 열려 있을 때만 닫습니다.
     */
    fun deleteDetailMeal(mealId: String): HistoryUIState {
        val closedDetail = if (mealDetail?.meal?.id == mealId) null else mealDetail
        if (selectedDayMeals.none { it.id == mealId }) return copy(mealDetail = closedDetail)
        val remaining = selectedDayMeals
            .filterNot { it.id == mealId }
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
            mealDetail = closedDetail,
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

        /**
         * 데이터 로드 전에도 캘린더 그리드와 월 이동이 유효한 연/월로 동작하도록
         * 오늘 날짜 기준으로 초기화한 상태를 만듭니다.
         */
        fun initial(today: HistoryDateVO): HistoryUIState = HistoryUIState(
            displayedYear = today.year,
            displayedMonth = today.month,
            today = today,
            selectedDate = today,
            calendarDays = buildCalendarDayUiModels(
                year = today.year,
                month = today.month,
                records = emptyMap(),
            ),
            isLoading = true,
        )
    }
}

/**
 * 식사 상세 오버레이의 상태입니다.
 *
 * @property nameDraft 이름 수정 다이얼로그의 입력값. [HistoryMealDetailMode.EditingName]에서만 의미가 있습니다.
 * @property isActionInFlight 수정/삭제 요청이 진행 중인지 여부. true 인 동안 확정/취소 조작을 무시합니다.
 */
data class HistoryMealDetailUiState(
    val meal: MealHistoryVO = MealHistoryVO.empty,
    val mode: HistoryMealDetailMode = HistoryMealDetailMode.Viewing,
    val nameDraft: String = "",
    val isActionInFlight: Boolean = false,
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
