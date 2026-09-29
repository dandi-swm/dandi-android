package com.dandi.nyummy.history.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.analysis.MealAnalysisEvent
import com.dandi.nyummy.common.domain.helper.MealAnalysisEventHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.ReanalyzeMealUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import com.dandi.nyummy.history.presentation.model.isoDateOf
import com.dandi.nyummy.history.presentation.util.lastDayOf
import com.dandi.nyummy.history.presentation.util.nextMonthOf
import com.dandi.nyummy.history.presentation.util.previousMonthOf
import com.dandi.nyummy.history.presentation.util.todayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyMeals: GetMonthlyMealsUseCase,
    private val getDailyMeals: GetDailyMealsUseCase,
    private val getMealDetail: GetMealDetailUseCase,
    private val updateMealName: UpdateMealNameUseCase,
    private val deleteMeal: DeleteMealUseCase,
    private val reanalyzeMeal: ReanalyzeMealUseCase,
    private val mealAnalysisEventHelper: MealAnalysisEventHelper,
) : MviViewModel<HistoryIntent, HistoryUIState, HistoryReducerEvent>(
    HistoryUIState.initial(todayDate()),
) {

    /**
     * 월/일 조회 job. 새 조회가 이전 조회를 취소해 늦게 도착한 과거 응답이
     * 최신 화면을 덮어쓰지 않게 한다(최신 요청만 dispatch).
     */
    private var loadJob: Job? = null

    /**
     * 분석 완료 알림으로 인한 조용한 재조회 job. 사용자의 월/일 조회([loadJob])와 분리해,
     * 알림이 도착해도 진행 중인 사용자 조작이 취소되지 않게 한다.
     */
    private var refreshJob: Job? = null

    private var mealDetailJob: Job? = null

    init {
        val today = currentState.selectedDate
        loadMonth(year = today.year, month = today.month, selectedDate = today)
        observeAnalysisEvents()
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

            // 오늘 날짜 선택과 동일한 경로 — 다른 달이면 그 달을 로드하며 선택한다.
            HistoryIntent.ClickToday -> selectDate(todayDate())

            HistoryIntent.ToggleNutritionSummary ->
                dispatch(HistoryReducerEvent.NutritionSummaryToggled)

            is HistoryIntent.ClickMeal -> openMealDetail(intent.mealId)

            is HistoryIntent.ClickRetryAnalysis -> requestReanalysis(intent.mealId)

            is HistoryIntent.ClickDeleteFailedMeal -> requestDeleteFailedMeal(intent.mealId)

            HistoryIntent.DismissMealDetail -> dismissMealDetail()

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
            HistoryReducerEvent.LoadStarted -> state.copy(isLoading = true)

            HistoryReducerEvent.LoadFailed -> state.copy(isLoading = false)

            HistoryReducerEvent.MealActionFailed -> state.withMealDetail {
                // 에러 안내는 UseCase 의 스낵바가 담당한다. 다이얼로그는 열어 둔 채 재시도만 허용한다.
                it.copy(isActionInFlight = false)
            }

            HistoryReducerEvent.MealActionStarted -> state.withMealDetail {
                it.copy(isActionInFlight = true)
            }

            is HistoryReducerEvent.MealDetailLoaded -> state.withMealDetail { detail ->
                // 늦게 도착한 다른 식사의 응답이 현재 열린 상세를 덮어쓰지 않게 한다.
                if (detail.meal.id == event.mealId) {
                    detail.copy(
                        meal = detail.meal.copy(
                            // 목록에서 가져온 값이 있으면 빈 응답으로 지우지 않는다.
                            photoUrl = event.photoUrl.ifBlank { detail.meal.photoUrl },
                            foodIconId = event.foodIconId.ifBlank { detail.meal.foodIconId },
                            catComment = event.catComment,
                        ),
                    )
                } else {
                    detail
                }
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
                selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                dailyNutrition = event.dailyDetail.nutrition,
                isLoading = false,
                reanalyzingMealIds = persistentSetOf(),
                mealDetail = null,
            )

            is HistoryReducerEvent.DaySelected -> state.copy(
                selectedDate = event.date,
                selectedDayMeals = event.dailyDetail.meals.withCompletedMealOrder(),
                dailyNutrition = event.dailyDetail.nutrition,
                reanalyzingMealIds = persistentSetOf(),
                mealDetail = null,
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

            is HistoryReducerEvent.MealReanalyzeStarted ->
                state.withReanalyzing(mealId = event.mealId, inFlight = true)

            is HistoryReducerEvent.MealReanalyzeSucceeded ->
                state.withMealStatus(mealId = event.mealId, status = event.status)

            is HistoryReducerEvent.MealReanalyzeFailed ->
                state.withReanalyzing(mealId = event.mealId, inFlight = false)

            is HistoryReducerEvent.MealDeleteRequestedFor -> state.copy(
                mealDetail = HistoryMealDetailUiState(
                    meal = event.meal,
                    mode = HistoryMealDetailMode.ConfirmingDelete,
                ),
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

            is HistoryReducerEvent.MealNameEditCommitted ->
                state.commitMealNameEdit(mealId = event.mealId, newName = event.newName)

            HistoryReducerEvent.MealNameEditCanceled -> state.withMealDetail { detail ->
                // 저장 요청이 진행 중이면 취소를 무시해 결과 반영과의 경쟁을 막는다.
                if (detail.isActionInFlight) {
                    detail
                } else {
                    detail.copy(mode = HistoryMealDetailMode.Viewing, nameDraft = "")
                }
            }

            HistoryReducerEvent.MealDeleteRequested -> state.withMealDetail { detail ->
                detail.copy(mode = HistoryMealDetailMode.ConfirmingDelete)
            }

            HistoryReducerEvent.MealDeleteCanceled -> {
                val detail = state.mealDetail
                when {
                    detail == null || detail.isActionInFlight -> state

                    // 분석 실패 카드에서 연 삭제 확인이면 돌아갈 상세 화면이 없으므로
                    // 오버레이를 닫고 목록의 실패 카드로 되돌아간다.
                    !detail.meal.isAnalysisCompleted -> state.copy(mealDetail = null)

                    else -> state.copy(
                        mealDetail = detail.copy(mode = HistoryMealDetailMode.Viewing),
                    )
                }
            }

            is HistoryReducerEvent.MealDeleted -> state.deleteDetailMeal(event.mealId)
        }

    /** 월 캘린더와 선택 날짜의 일별 기록을 함께 조회한다. 진행 중인 조회는 취소한다. */
    private fun loadMonth(year: Int, month: Int, selectedDate: HistoryDateVO) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            dispatch(HistoryReducerEvent.LoadStarted)
            val today = todayDate()
            // 월간 조회가 실패하면 일간 조회를 건너뛴다 — 두 UseCase 가 같은 오류
            // 스낵바를 각각 발행해 중복 노출되는 것을 막는다.
            val calendar = getMonthlyMeals(year, month).getOrNull() ?: run {
                dispatch(HistoryReducerEvent.LoadFailed)
                return@launch
            }
            val dailyDetail = getDailyMeals(
                selectedDate.year,
                selectedDate.month,
                selectedDate.day,
            ).getOrNull() ?: run {
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
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getDailyMeals(date.year, date.month, date.day)
                .onSuccess { dispatch(HistoryReducerEvent.DaySelected(date = date, dailyDetail = it)) }
                .onFailure { dispatch(HistoryReducerEvent.LoadFailed) }
        }
    }

    /** 상세 오버레이를 즉시 열고, 사진 URL·냐미 한마디가 포함된 단건 상세를 이어서 받아 갱신한다. */
    private fun openMealDetail(mealId: String) {
        val meal = currentState.selectedDayMeals.firstOrNull { it.id == mealId } ?: return
        // 실패/분석 중 기록은 보여줄 이름·영양 정보가 없어 카드 안의 액션만 제공한다.
        if (!meal.isAnalysisCompleted) return
        mealDetailJob?.cancel()
        dispatch(HistoryReducerEvent.MealDetailOpened(meal))
        val id = mealId.toLongOrNull() ?: return
        mealDetailJob = viewModelScope.launch {
            getMealDetail(id).onSuccess { detail ->
                dispatch(
                    HistoryReducerEvent.MealDetailLoaded(
                        mealId = mealId,
                        photoUrl = detail.photoUrl,
                        foodIconId = detail.foodIconId,
                        catComment = detail.catComment,
                    ),
                )
            }
        }
    }

    private fun dismissMealDetail() {
        mealDetailJob?.cancel()
        mealDetailJob = null
        dispatch(HistoryReducerEvent.MealDetailDismissed)
    }

    /**
     * 분석 완료 알림을 구독한다. 이벤트에 날짜가 실려 있고 그 날짜가 지금 보고 있는 날이 아니면
     * 화면에 보이지 않는 갱신이라 흘려보낸다(다시 그 날짜를 선택할 때 새로 조회된다).
     */
    private fun observeAnalysisEvents() {
        viewModelScope.launch {
            mealAnalysisEventHelper.events.collect { event ->
                if (isEventForSelectedDate(event)) refreshSelectedDayQuietly()
            }
        }
    }

    private fun isEventForSelectedDate(event: MealAnalysisEvent): Boolean =
        event.date.isBlank() || event.date == isoDateOf(currentState.selectedDate)

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
                .onSuccess { meal ->
                    dispatch(
                        HistoryReducerEvent.MealReanalyzeSucceeded(
                            mealId = mealId,
                            status = meal.status,
                        ),
                    )
                }
                .onFailure { dispatch(HistoryReducerEvent.MealReanalyzeFailed(mealId)) }
        }
    }

    /** 분석 실패 카드에서 삭제를 누르면 기존 삭제 확인 다이얼로그를 그 식사로 연다. */
    private fun requestDeleteFailedMeal(mealId: String) {
        val meal = currentState.selectedDayMeals.firstOrNull { it.id == mealId } ?: return
        dispatch(HistoryReducerEvent.MealDeleteRequestedFor(meal))
    }

    private fun confirmEditMealName() {
        val detail = currentState.mealDetail ?: return
        if (detail.isActionInFlight) return
        val newName = detail.nameDraft.trim()
        if (newName.isEmpty()) return
        // 요청 시점의 대상을 캡처해, 응답이 늦게 와도 이 식사에만 반영한다.
        val targetMealId = detail.meal.id
        val id = targetMealId.toLongOrNull() ?: return
        dispatch(HistoryReducerEvent.MealActionStarted)
        // 실패 시 에러 안내는 UseCase 의 스낵바가 담당하고, 수정 다이얼로그는 열어 둔 채 재시도를 허용한다.
        viewModelScope.launch {
            updateMealName(id, newName)
                .onSuccess {
                    dispatch(
                        HistoryReducerEvent.MealNameEditCommitted(
                            mealId = targetMealId,
                            newName = newName,
                        ),
                    )
                }
                .onFailure { dispatch(HistoryReducerEvent.MealActionFailed) }
        }
    }

    private fun confirmDeleteMeal() {
        val detail = currentState.mealDetail ?: return
        if (detail.isActionInFlight) return
        val targetMealId = detail.meal.id
        val id = targetMealId.toLongOrNull() ?: return
        dispatch(HistoryReducerEvent.MealActionStarted)
        // 실패 시 에러 안내는 UseCase 의 스낵바가 담당하고, 삭제 확인 다이얼로그는 열어 둔 채 재시도를 허용한다.
        viewModelScope.launch {
            deleteMeal(id)
                .onSuccess { dispatch(HistoryReducerEvent.MealDeleted(mealId = targetMealId)) }
                .onFailure { dispatch(HistoryReducerEvent.MealActionFailed) }
        }
    }

    /** 월 이동 시 선택 일(day)은 유지하되 대상 달의 말일을 넘지 않게 보정한다. */
    private fun moveMonth(target: Pair<Int, Int>) {
        val (year, month) = target
        val day = currentState.selectedDate.day.coerceIn(1, lastDayOf(year, month))
        loadMonth(year = year, month = month, selectedDate = HistoryDateVO(year, month, day))
    }
}
