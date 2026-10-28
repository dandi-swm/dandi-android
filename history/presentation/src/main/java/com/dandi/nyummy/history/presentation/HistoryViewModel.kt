package com.dandi.nyummy.history.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.GetMonthWithDayMealsUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.OutdatedMealException
import com.dandi.nyummy.history.domain.ReanalyzeMealUseCase
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.domain.MealDetailPage
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.util.isAfter
import com.dandi.nyummy.history.presentation.util.lastDayOf
import com.dandi.nyummy.history.presentation.util.todayDate
import com.dandi.nyummy.meal.domain.MealRecordPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyMeals: GetMonthlyMealsUseCase,
    private val getDailyMeals: GetDailyMealsUseCase,
    private val getMonthWithDayMeals: GetMonthWithDayMealsUseCase,
    private val deleteMeal: DeleteMealUseCase,
    private val reanalyzeMeal: ReanalyzeMealUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<HistoryIntent, HistoryUIState, HistoryReducerEvent>(
    HistoryUIState.initial(todayDate()),
) {

    /**
     * 월/일 조회 job. 새 조회가 이전 조회를 취소해 늦게 도착한 과거 응답이
     * 최신 화면을 덮어쓰지 않게 한다(최신 요청만 dispatch).
     */
    private var loadJob: Job? = null

    /** 낡은 실패 카드를 고치려는 조용한 재조회 job. 사용자의 월/일 조회([loadJob])와 따로 둔다. */
    private var refreshJob: Job? = null

    /** 지난달 캘린더를 미리 받는 job. 사용자의 조회([loadJob])와 따로 두어 서로 취소하지 않는다. */
    private var prefetchJob: Job? = null

    /** 화면에 다시 들어왔을 때의 조용한 재조회 job. */
    private var resumeRefreshJob: Job? = null

    override fun onIntent(intent: HistoryIntent) {
        when (intent) {
            HistoryIntent.ScreenResumed -> onScreenResumed()

            is HistoryIntent.ChangeMonth -> changeMonth(intent.month)

            HistoryIntent.RetryLoad ->
                loadMonth(month = currentState.displayedHistoryMonth, selectedDate = currentState.selectedDate)

            HistoryIntent.ClickRecordMeal -> navigationHelper.navigateTo(MealRecordPage)

            is HistoryIntent.SelectDate -> selectDate(intent.date)

            // 오늘 날짜 선택과 동일한 경로 — 다른 달이면 그 달을 로드하며 선택한다.
            HistoryIntent.ClickToday -> selectDate(todayDate())

            HistoryIntent.ToggleNutritionSummary ->
                dispatch(HistoryReducerEvent.NutritionSummaryToggled)

            is HistoryIntent.ClickMeal -> openMealDetail(intent.mealId)

            is HistoryIntent.ClickRetryAnalysis -> requestReanalysis(intent.mealId)

            is HistoryIntent.ClickDeleteFailedMeal -> requestDeleteFailedMeal(intent.mealId)

            HistoryIntent.ConfirmDeleteMeal -> confirmDeleteMeal()

            HistoryIntent.CancelDeleteMeal ->
                dispatch(HistoryReducerEvent.MealDeleteCanceled)
        }
    }


    override fun reduce(state: HistoryUIState, event: HistoryReducerEvent): HistoryUIState =
        when (event) {
            HistoryReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)

            HistoryReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)

            // 에러 안내는 UseCase 의 스낵바가 담당한다. 다이얼로그는 열어 둔 채 재시도만 허용한다.
            HistoryReducerEvent.MealActionFailed -> state.copy(isDeletingMeal = false)

            HistoryReducerEvent.MealActionStarted -> state.copy(isDeletingMeal = true)

            is HistoryReducerEvent.MonthChanged -> state.copy(
                displayedYear = event.month.year,
                displayedMonth = event.month.month,
                selectedDate = event.selectedDate,
                selectedDayMeals = persistentListOf(),
                // 새 날짜의 기록을 받기 전이나 받지 못했을 때 이전 날짜의 영양이 남아 보이지 않게 비운다.
                dailyNutrition = DailyNutritionVO.empty,
                reanalyzingMealIds = persistentSetOf(),
                deletingMeal = null,
                isDeletingMeal = false,
            )

            is HistoryReducerEvent.MonthCalendarLoaded ->
                state.withCalendarMonth(monthOf(event.calendar), event.calendar.toCalendarDays())

            is HistoryReducerEvent.MonthLoaded -> state.copy(
                displayedYear = event.calendar.year,
                displayedMonth = event.calendar.month,
                today = event.today,
                selectedDate = event.selectedDate,
                selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                dailyNutrition = event.dailyDetail.nutrition,
                isLoading = false,
                isLoadFailed = false,
                reanalyzingMealIds = persistentSetOf(),
                deletingMeal = null,
                isDeletingMeal = false,
            ).withCalendarMonth(monthOf(event.calendar), event.calendar.toCalendarDays())

            is HistoryReducerEvent.DaySelectionStarted -> state.copy(
                selectedDate = event.date,
                selectedDayMeals = persistentListOf(),
                dailyNutrition = DailyNutritionVO.empty,
                isLoading = true,
                isLoadFailed = false,
                reanalyzingMealIds = persistentSetOf(),
                deletingMeal = null,
                isDeletingMeal = false,
            )

            is HistoryReducerEvent.DaySelected -> state.copy(
                selectedDate = event.date,
                isLoading = false,
                isLoadFailed = false,
                selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                dailyNutrition = event.dailyDetail.nutrition,
                reanalyzingMealIds = persistentSetOf(),
                deletingMeal = null,
                isDeletingMeal = false,
            )

            is HistoryReducerEvent.DayRefreshed ->
                // 응답이 늦게 도착하는 사이 사용자가 다른 날짜로 옮겼다면 버린다.
                if (event.date != state.selectedDate) {
                    state
                } else {
                    // reanalyzingMealIds 는 건드리지 않는다 — 다른 식사의 분석 완료 푸시로 새로고침되는
                    // 사이 진행 중인 재분석 표시가 지워지면 안 되고, 해제는 Succeeded/Failed 가 책임진다.
                    state.copy(
                        selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                        dailyNutrition = event.dailyDetail.nutrition,
                    )
                }

            is HistoryReducerEvent.ScreenRefreshed -> {
                val refreshed = state.copy(today = event.today)
                    .withCalendarMonth(monthOf(event.calendar), event.calendar.toCalendarDays())
                // reanalyzingMealIds 는 DayRefreshed 와 같은 이유로 건드리지 않는다.
                if (event.date != state.selectedDate) {
                    refreshed
                } else {
                    refreshed.copy(
                        selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                        dailyNutrition = event.dailyDetail.nutrition,
                    )
                }
            }

            is HistoryReducerEvent.MealReanalyzeStarted ->
                state.withReanalyzing(mealId = event.mealId, inFlight = true)

            is HistoryReducerEvent.MealReanalyzeSucceeded ->
                state.withMealStatus(mealId = event.mealId, status = event.status)

            is HistoryReducerEvent.MealReanalyzeFailed ->
                state.withReanalyzing(mealId = event.mealId, inFlight = false)

            is HistoryReducerEvent.MealDeleteRequestedFor ->
                state.copy(deletingMeal = event.meal, isDeletingMeal = false)

            HistoryReducerEvent.NutritionSummaryToggled ->
                state.copy(isNutritionExpanded = !state.isNutritionExpanded)

            HistoryReducerEvent.MealDeleteCanceled ->
                if (state.isDeletingMeal) state else state.copy(deletingMeal = null)

            is HistoryReducerEvent.MealDeleted -> state.deleteMeal(event.mealId)
        }

    /**
     * 캘린더를 넘겨 [target] 달에서 멈췄을 때 그 달을 보여 준다. 이번 달보다 뒤거나 이미 보고 있는 달이면 무시한다.
     * 고른 날은 같은 날짜로 두되 그 달 말일을 넘지 않게 하고, 이번 달이면 오늘을 넘지 않게 한다.
     */
    private fun changeMonth(target: HistoryMonth) {
        val state = currentState
        if (target > state.currentMonth || target == state.displayedHistoryMonth) return
        val lastSelectableDay = if (target == state.currentMonth) state.today.day else lastDayOf(target.year, target.month)
        val day = state.selectedDate.day.coerceIn(1, lastSelectableDay)
        showMonth(target, HistoryDateVO(year = target.year, month = target.month, day = day))
    }

    /** 보고 있는 달을 바로 바꾸고(캘린더가 되돌아가지 않게) 그 달 데이터를 받는다. */
    private fun showMonth(month: HistoryMonth, selectedDate: HistoryDateVO) {
        dispatch(HistoryReducerEvent.MonthChanged(month = month, selectedDate = selectedDate))
        loadMonth(month = month, selectedDate = selectedDate)
    }

    /** 월 캘린더와 선택 날짜의 일별 기록을 함께 조회한다. 진행 중인 조회는 취소한다. */
    private fun loadMonth(month: HistoryMonth, selectedDate: HistoryDateVO) {
        val (year, monthValue) = month
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            dispatch(HistoryReducerEvent.LoadStarted)
            val today = todayDate()
            // 월간과 일간을 함께 받는다. 하나라도 실패하면 화면에 오류를 보여 준다.
            val loaded = getMonthWithDayMeals(year, monthValue, selectedDate).getOrNull() ?: run {
                dispatch(HistoryReducerEvent.LoadFailed)
                return@launch
            }
            dispatch(
                HistoryReducerEvent.MonthLoaded(
                    today = today,
                    calendar = loaded.calendar,
                    selectedDate = selectedDate,
                    dailyDetail = loaded.dailyDetail,
                ),
            )
            prefetchPreviousMonth(month)
        }
    }

    /** 왼쪽으로 넘겼을 때 바로 보이도록 [month] 지난달 캘린더를 조용히 받아 둔다. 이미 받았거나 실패하면 그대로 둔다. */
    private fun prefetchPreviousMonth(month: HistoryMonth) {
        val previous = month.plusMonths(-1)
        if (previous in currentState.calendarMonths) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            getMonthlyMeals(previous.year, previous.month).onSuccess {
                dispatch(HistoryReducerEvent.MonthCalendarLoaded(it))
            }
        }
    }

    private fun selectDate(date: HistoryDateVO) {
        if (date == currentState.selectedDate || date.isAfter(todayDate())) return
        // 인접 월 날짜를 선택하면 해당 월로 이동하면서 그 날짜를 선택한다.
        if (!currentState.displayedHistoryMonth.contains(date)) {
            showMonth(HistoryMonth.of(date), date)
            return
        }
        loadJob?.cancel()
        dispatch(HistoryReducerEvent.DaySelectionStarted(date))
        loadJob = viewModelScope.launch {
            getDailyMeals(date.year, date.month, date.day)
                .onSuccess {
                    dispatch(HistoryReducerEvent.DaySelected(date = date, dailyDetail = it))
                        }
                .onFailure { dispatch(HistoryReducerEvent.LoadFailed) }
        }
    }

    /** 분석이 끝난 식사를 누르면 상세 페이지로 간다. 실패/분석 중 기록은 카드 안의 동작만 있다. */
    private fun openMealDetail(mealId: String) {
        val meal = currentState.selectedDayMeals.firstOrNull { it.id == mealId } ?: return
        if (!meal.isAnalysisCompleted) return
        val id = mealId.toLongOrNull() ?: return
        navigationHelper.navigateTo(MealDetailPage(id))
    }

    /**
     * 화면이 보이게 됐다. 처음 열 때 기록을 불러오는 곳도 여기다.
     * 탭을 오가거나 식사를 기록하고 돌아오는 사이 기록이 바뀌었을 수 있어 돌아올 때마다 다시 읽는다.
     * - 조회가 진행 중이면 건너뛴다.
     * - 오늘을 보고 있었는데 그 사이 날짜가 넘어갔으면 새 오늘로 옮긴다.
     * - 처음 열었거나 불러오지 못한 화면이면 로딩을 보여 주며 불러온다. 그 밖에는 로딩 표시 없이 조용히 바꾼다.
     */
    private fun onScreenResumed() {
        if (loadJob?.isActive == true) return
        val state = currentState
        val today = todayDate()
        val dayPassed = today != state.today && state.selectedDate == state.today
        when {
            dayPassed -> showMonth(HistoryMonth.of(today), today)
            // loadJob 이 없으면 아직 한 번도 불러오지 않은 첫 진입이다.
            loadJob == null || state.isLoadFailed ->
                loadMonth(month = state.displayedHistoryMonth, selectedDate = state.selectedDate)
            else -> refreshScreenQuietly(today)
        }
    }

    /** 로딩 표시 없이 보고 있는 달의 캘린더와 선택 날짜의 기록을 다시 불러온다. 실패하면 화면을 그대로 둔다. */
    private fun refreshScreenQuietly(today: HistoryDateVO) {
        val month = currentState.displayedHistoryMonth
        val date = currentState.selectedDate
        resumeRefreshJob?.cancel()
        resumeRefreshJob = viewModelScope.launch {
            val refreshed = getMonthWithDayMeals(month.year, month.month, date).getOrNull() ?: return@launch
            dispatch(
                HistoryReducerEvent.ScreenRefreshed(
                    today = today,
                    calendar = refreshed.calendar,
                    date = date,
                    dailyDetail = refreshed.dailyDetail,
                ),
            )
        }
    }

    /** 로딩 표시 없이 선택 날짜의 식사 목록만 다시 불러온다. 실패하면 화면을 그대로 둔다. */
    private fun refreshSelectedDayQuietly() {
        val date = currentState.selectedDate
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            getDailyMeals(date.year, date.month, date.day).onSuccess {
                dispatch(HistoryReducerEvent.DayRefreshed(date = date, dailyDetail = it))
                }
        }
    }

    /** 분석에 실패한 식사의 재분석을 요청한다. 같은 식사의 요청이 진행 중이면 무시한다. */
    private fun requestReanalysis(mealId: String) {
        if (currentState.isReanalyzing(mealId)) return
        val id = mealId.toLongOrNull() ?: return
        dispatch(HistoryReducerEvent.MealReanalyzeStarted(mealId))
        // 실패 시 에러 안내는 UseCase 의 스낵바가 담당하고, 카드는 다시 재시도할 수 있는 상태로 되돌린다.
        viewModelScope.launch {
            reanalyzeMeal(id)
                .onSuccess { status ->
                    dispatch(
                        HistoryReducerEvent.MealReanalyzeSucceeded(
                            mealId = mealId,
                            status = status,
                        ),
                    )
                }
                .onFailure { error ->
                    dispatch(HistoryReducerEvent.MealReanalyzeFailed(mealId))
                    // 이미 다시 분석했거나 지운 식사라 실패 카드가 낡았다. 목록을 새로 받아 지금 상태를 보여 준다.
                    if (error is OutdatedMealException) refreshSelectedDayQuietly()
                }
        }
    }

    /** 분석 실패 카드에서 삭제를 누르면 기존 삭제 확인 다이얼로그를 그 식사로 연다. */
    private fun requestDeleteFailedMeal(mealId: String) {
        val meal = currentState.selectedDayMeals.firstOrNull { it.id == mealId } ?: return
        dispatch(HistoryReducerEvent.MealDeleteRequestedFor(meal))
    }

    private fun confirmDeleteMeal() {
        val meal = currentState.deletingMeal ?: return
        if (currentState.isDeletingMeal) return
        val targetMealId = meal.id
        val id = targetMealId.toLongOrNull() ?: return
        dispatch(HistoryReducerEvent.MealActionStarted)
        // 실패 시 에러 안내는 UseCase 의 스낵바가 담당하고, 삭제 확인 다이얼로그는 열어 둔 채 재시도를 허용한다.
        viewModelScope.launch {
            deleteMeal(id)
                .onSuccess { dispatch(HistoryReducerEvent.MealDeleted(mealId = targetMealId)) }
                .onFailure { dispatch(HistoryReducerEvent.MealActionFailed) }
        }
    }
}

private fun monthOf(calendar: HistoryCalendarVO) = HistoryMonth(year = calendar.year, month = calendar.month)

private fun HistoryCalendarVO.toCalendarDays() =
    buildCalendarDayUiModels(year = year, month = month, records = days.associateBy { it.date })
