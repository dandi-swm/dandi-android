package com.dandi.nyummy.history.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetDailyMealsUseCase
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.GetMonthlyMealsUseCase
import com.dandi.nyummy.history.domain.OutdatedMealException
import com.dandi.nyummy.history.domain.ReanalyzeMealUseCase
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
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
import kotlinx.coroutines.delay
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
    private val submitAnalysisFeedback: SubmitAnalysisFeedbackUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<HistoryIntent, HistoryUIState, HistoryReducerEvent>(
    HistoryUIState.initial(todayDate()),
) {

    /**
     * 월/일 조회 job. 새 조회가 이전 조회를 취소해 늦게 도착한 과거 응답이
     * 최신 화면을 덮어쓰지 않게 한다(최신 요청만 dispatch).
     */
    private var loadJob: Job? = null

    /**
     * 알림 링크로 식사를 열 때의 조용한 재조회 job. 사용자의 월/일 조회([loadJob])와 분리해,
     * 진행 중인 사용자 조작이 취소되지 않게 한다.
     */
    private var refreshJob: Job? = null

    private var mealDetailJob: Job? = null

    /** 지난달 캘린더를 미리 받는 job. 사용자의 조회([loadJob])와 따로 두어 서로 취소하지 않는다. */
    private var prefetchJob: Job? = null

    /** 알림의 식사 상세 링크로 열어 달라고 받은 식사. 목록을 새로 받으면 [openPendingMealDetail]이 연다. */
    private var pendingMealId: String? = null

    /**
     * 이 화면에서 이미 처리한 링크의 알림. 탭을 오가며 화면이 다시 그려질 때 같은 인자로 다시 오므로 한 번만 연다.
     * 다른 알림의 링크는 라우트 키가 달라 새 화면(새 ViewModel)으로 열린다.
     */
    private var handledLinkEventId: String? = null

    /** 링크로 연 식사의 날짜를 찾는 job. 새 링크가 오면 이전 것을 취소한다. */
    private var linkJob: Job? = null

    init {
        val today = currentState.selectedDate
        loadMonth(month = HistoryMonth.of(today), selectedDate = today)
    }

    override fun onIntent(intent: HistoryIntent) {
        when (intent) {
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

            is HistoryIntent.OpenMealFromLink -> openMealFromLink(intent.mealId, intent.eventId)

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

            is HistoryIntent.ChangeFeedbackRating ->
                dispatch(HistoryReducerEvent.FeedbackRatingChanged(intent.halfSteps))

            is HistoryIntent.SelectFeedbackReason ->
                dispatch(HistoryReducerEvent.FeedbackReasonSelected(intent.reason))

            is HistoryIntent.ChangeFeedbackComment ->
                dispatch(HistoryReducerEvent.FeedbackCommentChanged(intent.text))

            HistoryIntent.SubmitFeedback -> submitFeedback()

            HistoryIntent.DismissFeedback -> currentState.mealDetail?.let {
                dispatch(HistoryReducerEvent.FeedbackDismissed(it.meal.id))
            }
        }
    }

    override fun reduce(state: HistoryUIState, event: HistoryReducerEvent): HistoryUIState =
        when (event) {
            HistoryReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)

            HistoryReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)

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

            is HistoryReducerEvent.MonthChanged -> state.copy(
                displayedYear = event.month.year,
                displayedMonth = event.month.month,
                selectedDate = event.selectedDate,
                selectedDayMeals = persistentListOf(),
                // 새 날짜의 기록을 받기 전이나 받지 못했을 때 이전 날짜의 영양이 남아 보이지 않게 비운다.
                dailyNutrition = DailyNutritionVO.empty,
                reanalyzingMealIds = persistentSetOf(),
                mealDetail = null,
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
                mealDetail = null,
            ).withCalendarMonth(monthOf(event.calendar), event.calendar.toCalendarDays())

            is HistoryReducerEvent.DaySelectionStarted -> state.copy(
                selectedDate = event.date,
                selectedDayMeals = persistentListOf(),
                dailyNutrition = DailyNutritionVO.empty,
                isLoading = true,
                isLoadFailed = false,
                reanalyzingMealIds = persistentSetOf(),
                mealDetail = null,
            )

            is HistoryReducerEvent.DaySelected -> state.copy(
                selectedDate = event.date,
                isLoading = false,
                isLoadFailed = false,
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

            is HistoryReducerEvent.FeedbackPromptShown -> state.withMealDetail { detail ->
                if (detail.meal.id == event.mealId) detail.copy(feedback = AnalysisFeedbackUiState()) else detail
            }

            is HistoryReducerEvent.FeedbackRatingChanged -> state.withMealDetail { detail ->
                detail.withFeedback { it.copy(ratingHalfSteps = event.halfSteps) }
            }

            is HistoryReducerEvent.FeedbackReasonSelected -> state.withMealDetail { detail ->
                detail.withFeedback { feedback ->
                    feedback.copy(selectedReason = event.reason.takeIf { it != feedback.selectedReason })
                }
            }

            is HistoryReducerEvent.FeedbackCommentChanged -> state.withMealDetail { detail ->
                detail.withFeedback { it.copy(comment = event.text) }
            }

            is HistoryReducerEvent.FeedbackSubmitStarted -> state.withFeedbackOf(event.mealId) {
                it.copy(isSubmitting = true)
            }

            is HistoryReducerEvent.FeedbackSubmitFailed -> state.withFeedbackOf(event.mealId) {
                it.copy(isSubmitting = false)
            }

            is HistoryReducerEvent.FeedbackDismissed -> state.withMealDetail { detail ->
                if (detail.meal.id == event.mealId) detail.copy(feedback = null) else detail
            }

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
            // 월간 조회가 실패하면 일간 조회를 건너뛰고 화면에 오류를 보여 준다.
            val calendar = getMonthlyMeals(year, monthValue).getOrNull() ?: run {
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
            openPendingMealDetail()
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
                    openPendingMealDetail()
                }
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
                // 평가 대상이면 상세를 잠깐 본 뒤 평가 카드를 띄운다. 그 전에 상세를 닫으면 이 job 과 함께 취소된다.
                if (detail.canCreateAnalysisFeedback) {
                    delay(FEEDBACK_PROMPT_DELAY_MILLIS)
                    dispatch(HistoryReducerEvent.FeedbackPromptShown(mealId))
                }
            }
        }
    }

    /**
     * 평가를 보낸다. 상세를 닫아도 보내던 평가는 끝까지 보낸다(결과는 같은 식사가 열려 있을 때만 화면에 반영).
     * 보내기에 성공하면 상세 시트를 닫는다. 고맙다는 안내는 UseCase 의 스낵바가 맡는데, 시트가 별도 창이라
     * 열려 있으면 스낵바가 그 아래에 가려지기 때문이다.
     */
    private fun submitFeedback() {
        val detail = currentState.mealDetail ?: return
        val feedback = detail.feedback ?: return
        if (!feedback.canSubmit) return
        val mealId = detail.meal.id
        val id = mealId.toLongOrNull() ?: return
        dispatch(HistoryReducerEvent.FeedbackSubmitStarted(mealId))
        viewModelScope.launch {
            submitAnalysisFeedback(id, feedback.ratingHalfSteps, feedback.selectedReason, feedback.comment)
                .onSuccess { if (currentState.mealDetail?.meal?.id == mealId) dismissMealDetail() }
                .onFailure { dispatch(HistoryReducerEvent.FeedbackSubmitFailed(mealId)) }
        }
    }

    private fun dismissMealDetail() {
        mealDetailJob?.cancel()
        mealDetailJob = null
        dispatch(HistoryReducerEvent.MealDetailDismissed)
    }

    /** 로딩 표시 없이 선택 날짜의 식사 목록만 다시 불러온다. 실패하면 화면을 그대로 둔다. */
    private fun refreshSelectedDayQuietly() {
        val date = currentState.selectedDate
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            getDailyMeals(date.year, date.month, date.day).onSuccess {
                dispatch(HistoryReducerEvent.DayRefreshed(date = date, dailyDetail = it))
                openPendingMealDetail()
            }
        }
    }

    /**
     * 알림의 식사 상세 링크(`/meals/{mealId}`)로 열렸다. 과거 식사의 재분석 알림이나 자정을 넘겨 누른 알림도 있어서,
     * 식사를 먼저 조회해 그 날짜를 연다(날짜를 모르면 오늘). 받은 시점의 목록은 분석 전 상태일 수 있어 그날 목록을 새로 받은 뒤 연다.
     */
    private fun openMealFromLink(mealId: Long, eventId: String) {
        if (eventId == handledLinkEventId) return
        handledLinkEventId = eventId
        linkJob?.cancel()
        linkJob = viewModelScope.launch {
            val date = getMealDetail(mealId).getOrNull()?.date?.takeUnless { it.isEmpty } ?: todayDate()
            // 화면을 처음 열 때의 조회가 끝난 뒤에 날짜를 옮긴다. 중간에 끊으면 캘린더가 빈 채로 남는다.
            loadJob?.join()
            pendingMealId = mealId.toString()
            if (date != currentState.selectedDate) selectDate(date) else refreshSelectedDayQuietly()
        }
    }

    /** 기다리던 식사가 지금 목록에 있으면 상세를 연다. 없거나 분석이 끝나지 않았으면 목록만 둔다([openMealDetail]이 거른다). */
    private fun openPendingMealDetail() {
        val mealId = pendingMealId ?: return
        pendingMealId = null
        openMealDetail(mealId)
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
}

/** 식사 [mealId]의 상세가 열려 있고 평가 카드가 떠 있을 때만 [transform]을 적용한다. */
private fun HistoryUIState.withFeedbackOf(
    mealId: String,
    transform: (AnalysisFeedbackUiState) -> AnalysisFeedbackUiState,
): HistoryUIState = withMealDetail { detail ->
    if (detail.meal.id == mealId) detail.withFeedback(transform) else detail
}

/** 상세가 열리고 평가 카드를 띄우기까지 기다리는 시간. 상세를 먼저 보게 한 뒤 묻는다. */
private const val FEEDBACK_PROMPT_DELAY_MILLIS = 1_500L

private fun monthOf(calendar: HistoryCalendarVO) = HistoryMonth(year = calendar.year, month = calendar.month)

private fun HistoryCalendarVO.toCalendarDays() =
    buildCalendarDayUiModels(year = year, month = month, records = days.associateBy { it.date })
