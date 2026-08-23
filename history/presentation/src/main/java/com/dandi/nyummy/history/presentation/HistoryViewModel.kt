package com.dandi.nyummy.history.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.HistoryErrorType
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.util.lastDayOf
import com.dandi.nyummy.history.presentation.util.nextMonthOf
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import com.dandi.nyummy.history.presentation.util.todayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyMeals: GetMonthlyMealsUseCase,
    private val getDailyMeals: GetDailyMealsUseCase,
    private val getMealDetail: GetMealDetailUseCase,
    private val updateMealName: UpdateMealNameUseCase,
    private val deleteMeal: DeleteMealUseCase,
) : MviViewModel<HistoryIntent, HistoryUIState, HistoryReducerEvent>(HistoryUIState.empty) {

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

            is HistoryIntent.ClickMeal -> openMealDetail(intent.mealId)

            HistoryIntent.DismissMealDetail ->
                dispatch(HistoryReducerEvent.MealDetailDismissed)

            HistoryIntent.ClickEditMealName ->
                dispatch(HistoryReducerEvent.MealNameEditStarted)

            is HistoryIntent.ChangeMealNameDraft ->
                dispatch(HistoryReducerEvent.MealNameDraftChanged(intent.text))

            HistoryIntent.ConfirmEditMealName -> confirmEditMealName()

            HistoryIntent.CancelEditMealName ->
                dispatch(HistoryReducerEvent.MealNameEditCanceled)

            HistoryIntent.ClickDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleteRequested)

            HistoryIntent.ConfirmDeleteMeal -> confirmDeleteMeal()

            HistoryIntent.CancelDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleteCanceled)
        }
    }

    override fun reduce(state: HistoryUIState, event: HistoryReducerEvent): HistoryUIState =
        when (event) {
            HistoryReducerEvent.LoadStarted -> state.copy(isLoading = true, errorType = null)

            HistoryReducerEvent.LoadFailed ->
                state.copy(isLoading = false, errorType = HistoryErrorType.LOAD_FAILED)

            is HistoryReducerEvent.MealActionFailed -> state
                .copy(errorType = event.errorType)
                .withMealDetail { it.copy(mode = HistoryMealDetailMode.Viewing) }

            is HistoryReducerEvent.MealDetailPhotoLoaded -> state.withMealDetail { detail ->
                detail.copy(meal = detail.meal.copy(photoUrl = event.photoUrl))
            }

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
                errorType = null,
                mealDetail = null,
            )

            is HistoryReducerEvent.DaySelected -> state.copy(
                selectedDate = event.date,
                selectedDayMeals = event.dailyDetail.meals.toImmutableList(),
                dailyNutrition = event.dailyDetail.nutrition,
                errorType = null,
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

    /** 월 캘린더와 선택 날짜의 일별 기록을 함께 조회한다. */
    private fun loadMonth(year: Int, month: Int, selectedDate: HistoryDateVO) {
        viewModelScope.launch {
            dispatch(HistoryReducerEvent.LoadStarted)
            val today = todayDate()
            val calendar = getMonthlyMeals(year, month).getOrNull()
            val dailyDetail = getDailyMeals(
                selectedDate.year,
                selectedDate.month,
                selectedDate.day,
            ).getOrNull()
            if (calendar == null || dailyDetail == null) {
                dispatch(HistoryReducerEvent.LoadFailed)
                return@launch
            }
            dispatch(
                HistoryReducerEvent.MonthLoaded(
                    today = today,
                    calendar = calendar,
                    selectedDate = selectedDate,
                    dailyDetail = dailyDetail,
                ),
            )
        }
    }

    private fun selectDate(date: HistoryDateVO) {
        if (date == currentState.selectedDate) return
        // 인접 월 날짜를 선택하면 해당 월로 이동하면서 그 날짜를 선택한다.
        if (date.year != currentState.displayedYear || date.month != currentState.displayedMonth) {
            loadMonth(year = date.year, month = date.month, selectedDate = date)
            return
        }
        viewModelScope.launch {
            getDailyMeals(date.year, date.month, date.day)
                .onSuccess { dispatch(HistoryReducerEvent.DaySelected(date = date, dailyDetail = it)) }
                .onFailure { dispatch(HistoryReducerEvent.LoadFailed) }
        }
    }

    /** 상세 오버레이를 즉시 열고, 사진 URL 이 포함된 단건 상세를 이어서 받아 갱신한다. */
    private fun openMealDetail(mealId: String) {
        val meal = currentState.selectedDayMeals.firstOrNull { it.id == mealId } ?: return
        dispatch(HistoryReducerEvent.MealDetailOpened(meal))
        val id = mealId.toLongOrNull() ?: return
        viewModelScope.launch {
            getMealDetail(id).onSuccess { detail ->
                if (detail.photoUrl.isNotBlank()) {
                    dispatch(HistoryReducerEvent.MealDetailPhotoLoaded(detail.photoUrl))
                }
            }
        }
    }

    private fun confirmEditMealName() {
        val detail = currentState.mealDetail ?: return
        val newName = detail.nameDraft.trim()
        if (newName.isEmpty()) return
        val id = detail.meal.id.toLongOrNull() ?: return
        viewModelScope.launch {
            updateMealName(id, newName)
                .onSuccess { dispatch(HistoryReducerEvent.MealNameEditCommitted) }
                .onFailure {
                    dispatch(HistoryReducerEvent.MealActionFailed(HistoryErrorType.EDIT_SAVE_FAILED))
                }
        }
    }

    private fun confirmDeleteMeal() {
        val detail = currentState.mealDetail ?: return
        val id = detail.meal.id.toLongOrNull() ?: return
        viewModelScope.launch {
            deleteMeal(id)
                .onSuccess { dispatch(HistoryReducerEvent.MealDeleted) }
                .onFailure {
                    dispatch(HistoryReducerEvent.MealActionFailed(HistoryErrorType.DELETE_FAILED))
                }
        }
    }

    /** 월 이동 시 선택 일(day)은 유지하되 대상 달의 말일을 넘지 않게 보정한다. */
    private fun moveMonth(target: Pair<Int, Int>) {
        val (year, month) = target
        val day = currentState.selectedDate.day.coerceIn(1, lastDayOf(year, month))
        loadMonth(year = year, month = month, selectedDate = HistoryDateVO(year, month, day))
    }
}
