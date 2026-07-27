package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.mock.HistoryMockData
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.util.lastDayOf
import com.dandi.nyummy.history.presentation.util.nextMonthOf
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import com.dandi.nyummy.history.presentation.util.todayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor() :
    MviViewModel<HistoryIntent, HistoryUIState, HistoryReducerEvent>(HistoryUIState.empty) {

    init {
        val today = todayDate()
        loadMonth(year = today.year, month = today.month, selectedDate = today)
    }

    override fun onIntent(intent: HistoryIntent) {
        when (intent) {
            HistoryIntent.ClickPreviousMonth -> moveMonth(
                previousMonthOf(currentState.displayedYear, currentState.displayedMonth),
            )

            HistoryIntent.ClickNextMonth -> moveMonth(
                nextMonthOf(currentState.displayedYear, currentState.displayedMonth),
            )

            is HistoryIntent.SelectDate -> selectDate(intent.date)

            HistoryIntent.ToggleNutritionSummary ->
                dispatch(HistoryReducerEvent.NutritionSummaryToggled)

            is HistoryIntent.ClickMeal -> {
                val meal = currentState.selectedDayMeals.firstOrNull { it.id == intent.mealId }
                if (meal != null) dispatch(HistoryReducerEvent.MealDetailOpened(meal))
            }

            HistoryIntent.DismissMealDetail ->
                dispatch(HistoryReducerEvent.MealDetailDismissed)

            HistoryIntent.ClickEditMealName ->
                dispatch(HistoryReducerEvent.MealNameEditStarted)

            is HistoryIntent.ChangeMealNameDraft ->
                dispatch(HistoryReducerEvent.MealNameDraftChanged(intent.text))

            // TODO: 식사 이름 수정 API 연동 (백엔드 미구현) — 현재는 화면 상태만 갱신
            HistoryIntent.ConfirmEditMealName ->
                dispatch(HistoryReducerEvent.MealNameEditCommitted)

            HistoryIntent.CancelEditMealName ->
                dispatch(HistoryReducerEvent.MealNameEditCanceled)

            HistoryIntent.ClickDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleteRequested)

            // TODO: 식사 기록 삭제 API 연동 (백엔드 미구현) — 현재는 화면 상태만 갱신
            HistoryIntent.ConfirmDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleted)

            HistoryIntent.CancelDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleteCanceled)
        }
    }

    override fun reduce(state: HistoryUIState, event: HistoryReducerEvent): HistoryUIState =
        when (event) {
            is HistoryReducerEvent.MonthLoaded -> state.copy(
                displayedYear = event.calendar.year,
                displayedMonth = event.calendar.month,
                today = event.today,
                selectedDate = event.selectedDate,
                calendarDays = buildCalendarDayUiModels(
                    year = event.calendar.year,
                    month = event.calendar.month,
                    records = event.calendar.days.associateBy { it.date },
                ),
                selectedDayMeals = event.dailyDetail.meals.toImmutableList(),
                dailyNutrition = event.dailyDetail.nutrition,
                isLoading = false,
                mealDetail = null,
            )

            is HistoryReducerEvent.DaySelected -> state.copy(
                selectedDate = event.date,
                selectedDayMeals = event.dailyDetail.meals.toImmutableList(),
                dailyNutrition = event.dailyDetail.nutrition,
                mealDetail = null,
            )

            HistoryReducerEvent.NutritionSummaryToggled ->
                state.copy(isNutritionExpanded = !state.isNutritionExpanded)

            is HistoryReducerEvent.MealDetailOpened ->
                state.copy(mealDetail = HistoryMealDetailUiState(meal = event.meal))

            HistoryReducerEvent.MealDetailDismissed ->
                state.copy(mealDetail = null)

            HistoryReducerEvent.MealNameEditStarted -> state.withMealDetail { detail ->
                detail.copy(mode = HistoryMealDetailMode.EditingName, nameDraft = detail.meal.name)
            }

            is HistoryReducerEvent.MealNameDraftChanged -> state.withMealDetail { detail ->
                detail.copy(nameDraft = event.text)
            }

            HistoryReducerEvent.MealNameEditCommitted -> state.commitMealNameEdit()

            HistoryReducerEvent.MealNameEditCanceled -> state.withMealDetail { detail ->
                detail.copy(mode = HistoryMealDetailMode.Viewing, nameDraft = "")
            }

            HistoryReducerEvent.MealDeleteRequested -> state.withMealDetail { detail ->
                detail.copy(mode = HistoryMealDetailMode.ConfirmingDelete)
            }

            HistoryReducerEvent.MealDeleteCanceled -> state.withMealDetail { detail ->
                detail.copy(mode = HistoryMealDetailMode.Viewing)
            }

            HistoryReducerEvent.MealDeleted -> state.deleteDetailMeal()
        }

    // TODO: 월 캘린더/일별 기록 조회 API 연동 시 아래 두 함수 본문을 UseCase 호출로 대체 (백엔드 미구현)
    private fun loadMonth(year: Int, month: Int, selectedDate: HistoryDateVO) {
        val today = todayDate()
        dispatch(
            HistoryReducerEvent.MonthLoaded(
                today = today,
                calendar = HistoryMockData.monthOf(year = year, month = month, today = today),
                selectedDate = selectedDate,
                dailyDetail = HistoryMockData.dayOf(date = selectedDate, today = today),
            ),
        )
    }

    private fun selectDate(date: HistoryDateVO) {
        if (date == currentState.selectedDate) return
        // 인접 월 날짜를 선택하면 해당 월로 이동하면서 그 날짜를 선택한다.
        if (date.year != currentState.displayedYear || date.month != currentState.displayedMonth) {
            loadMonth(year = date.year, month = date.month, selectedDate = date)
            return
        }
        dispatch(
            HistoryReducerEvent.DaySelected(
                date = date,
                dailyDetail = HistoryMockData.dayOf(date = date, today = currentState.today),
            ),
        )
    }

    /** 월 이동 시 선택 일(day)은 유지하되 대상 달의 말일을 넘지 않게 보정합니다. */
    private fun moveMonth(target: Pair<Int, Int>) {
        val (year, month) = target
        val day = currentState.selectedDate.day.coerceIn(1, lastDayOf(year, month))
        loadMonth(year = year, month = month, selectedDate = HistoryDateVO(year, month, day))
    }
}
