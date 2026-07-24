package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.domain.HistoryErrorType
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryCalendarDayUiModel
import com.dandi.nyummy.history.presentation.model.monthLabelOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
