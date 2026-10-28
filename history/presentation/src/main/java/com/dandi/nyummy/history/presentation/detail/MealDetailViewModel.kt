package com.dandi.nyummy.history.presentation.detail

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.DeleteMealUseCase
import com.dandi.nyummy.history.domain.GetMealDetailUseCase
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 식사 하나의 상세. 조회, 이름 수정, 삭제, AI 분석 평가를 맡는다.
 * 바뀐 내용은 기록 화면이 다시 보일 때 스스로 다시 읽으므로 따로 알리지 않는다.
 */
@HiltViewModel
class MealDetailViewModel @Inject constructor(
    private val getMealDetail: GetMealDetailUseCase,
    private val updateMealName: UpdateMealNameUseCase,
    private val deleteMeal: DeleteMealUseCase,
    private val submitAnalysisFeedback: SubmitAnalysisFeedbackUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealDetailIntent, MealDetailUIState, MealDetailReducerEvent>(MealDetailUIState()) {

    private var mealId: Long? = null
    private var loadJob: Job? = null

    override fun onIntent(intent: MealDetailIntent) {
        when (intent) {
            is MealDetailIntent.Enter -> if (mealId != intent.mealId) {
                mealId = intent.mealId
                load()
            }

            MealDetailIntent.RetryLoad -> load()

            MealDetailIntent.ClickBack -> navigationHelper.navigateToBack()

            MealDetailIntent.ClickEditName -> dispatch(MealDetailReducerEvent.NameEditStarted)

            is MealDetailIntent.ChangeNameDraft -> dispatch(MealDetailReducerEvent.NameDraftChanged(intent.text))

            MealDetailIntent.ConfirmEditName -> confirmEditName()

            MealDetailIntent.CancelEditName -> dispatch(MealDetailReducerEvent.NameEditCanceled)

            MealDetailIntent.ClickDelete -> dispatch(MealDetailReducerEvent.DeleteRequested)

            MealDetailIntent.ConfirmDelete -> confirmDelete()

            MealDetailIntent.CancelDelete -> dispatch(MealDetailReducerEvent.DeleteCanceled)

            is MealDetailIntent.ChangeFeedbackRating ->
                dispatch(MealDetailReducerEvent.FeedbackRatingChanged(intent.halfSteps))

            is MealDetailIntent.SelectFeedbackReason ->
                dispatch(MealDetailReducerEvent.FeedbackReasonSelected(intent.reason))

            is MealDetailIntent.ChangeFeedbackComment ->
                dispatch(MealDetailReducerEvent.FeedbackCommentChanged(intent.text))

            MealDetailIntent.SubmitFeedback -> submitFeedback()

            MealDetailIntent.DismissFeedback -> dispatch(MealDetailReducerEvent.FeedbackDismissed)
        }
    }

    override fun reduce(state: MealDetailUIState, event: MealDetailReducerEvent): MealDetailUIState =
        when (event) {
            MealDetailReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)

            is MealDetailReducerEvent.Loaded -> state.copy(meal = event.meal, isLoading = false, isLoadFailed = false)

            MealDetailReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)

            MealDetailReducerEvent.NameEditStarted -> state.meal?.let {
                state.copy(mode = MealDetailMode.EditingName, nameDraft = it.name)
            } ?: state

            is MealDetailReducerEvent.NameDraftChanged -> state.copy(nameDraft = event.text)

            is MealDetailReducerEvent.NameEditCommitted -> state.copy(
                meal = state.meal?.copy(name = event.newName),
                mode = MealDetailMode.Viewing,
                nameDraft = "",
                isActionInFlight = false,
            )

            // 보내는 중이면 취소를 무시해 결과 반영과의 경쟁을 막는다.
            MealDetailReducerEvent.NameEditCanceled ->
                if (state.isActionInFlight) state else state.copy(mode = MealDetailMode.Viewing, nameDraft = "")

            MealDetailReducerEvent.DeleteRequested ->
                if (state.meal == null) state else state.copy(mode = MealDetailMode.ConfirmingDelete)

            MealDetailReducerEvent.DeleteCanceled ->
                if (state.isActionInFlight) state else state.copy(mode = MealDetailMode.Viewing)

            MealDetailReducerEvent.ActionStarted -> state.copy(isActionInFlight = true)

            // 에러 안내는 UseCase 의 스낵바가 맡는다. 다이얼로그는 열어 둔 채 재시도만 허용한다.
            MealDetailReducerEvent.ActionFailed -> state.copy(isActionInFlight = false)

            MealDetailReducerEvent.FeedbackPromptShown -> state.copy(feedback = AnalysisFeedbackUiState())

            is MealDetailReducerEvent.FeedbackRatingChanged -> state.withEditableFeedback {
                it.copy(ratingHalfSteps = event.halfSteps)
            }

            is MealDetailReducerEvent.FeedbackReasonSelected -> state.withEditableFeedback { feedback ->
                feedback.copy(selectedReason = event.reason.takeIf { it != feedback.selectedReason })
            }

            is MealDetailReducerEvent.FeedbackCommentChanged -> state.withEditableFeedback {
                it.copy(comment = event.text)
            }

            MealDetailReducerEvent.FeedbackSubmitStarted -> state.withFeedback { it.copy(isSubmitting = true) }

            MealDetailReducerEvent.FeedbackSubmitFailed -> state.withFeedback { it.copy(isSubmitting = false) }

            MealDetailReducerEvent.FeedbackSubmitted -> state.copy(feedback = null)

            MealDetailReducerEvent.FeedbackDismissed ->
                if (state.feedback?.isSubmitting == true) state else state.copy(feedback = null)
        }

    /** 식사를 받는다. 평가 대상이면 상세를 잠깐 본 뒤 평가 카드를 띄운다. */
    private fun load() {
        val id = mealId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            dispatch(MealDetailReducerEvent.LoadStarted)
            getMealDetail(id)
                .onSuccess { meal ->
                    dispatch(MealDetailReducerEvent.Loaded(meal))
                    if (meal.canCreateAnalysisFeedback && currentState.feedback == null) {
                        delay(FEEDBACK_PROMPT_DELAY_MILLIS)
                        dispatch(MealDetailReducerEvent.FeedbackPromptShown)
                    }
                }
                .onFailure { dispatch(MealDetailReducerEvent.LoadFailed) }
        }
    }

    private fun confirmEditName() {
        val state = currentState
        if (state.isActionInFlight) return
        val newName = state.nameDraft.trim()
        val id = mealId ?: return
        if (newName.isEmpty()) return
        dispatch(MealDetailReducerEvent.ActionStarted)
        viewModelScope.launch {
            updateMealName(id, newName)
                .onSuccess { dispatch(MealDetailReducerEvent.NameEditCommitted(newName)) }
                .onFailure { dispatch(MealDetailReducerEvent.ActionFailed) }
        }
    }

    /** 지우면 기록 화면으로 돌아간다. 기록 화면은 다시 보일 때 그날 목록을 다시 읽는다. */
    private fun confirmDelete() {
        if (currentState.isActionInFlight) return
        val id = mealId ?: return
        dispatch(MealDetailReducerEvent.ActionStarted)
        viewModelScope.launch {
            deleteMeal(id)
                .onSuccess { navigationHelper.navigateToBack() }
                .onFailure { dispatch(MealDetailReducerEvent.ActionFailed) }
        }
    }

    /** 평가를 보낸다. 보내면 카드를 닫고, 고맙다는 안내는 UseCase 의 스낵바가 맡는다. */
    private fun submitFeedback() {
        val feedback = currentState.feedback ?: return
        if (!feedback.canSubmit) return
        val id = mealId ?: return
        dispatch(MealDetailReducerEvent.FeedbackSubmitStarted)
        viewModelScope.launch {
            submitAnalysisFeedback(id, feedback.ratingHalfSteps, feedback.selectedReason, feedback.comment)
                .onSuccess { dispatch(MealDetailReducerEvent.FeedbackSubmitted) }
                .onFailure { dispatch(MealDetailReducerEvent.FeedbackSubmitFailed) }
        }
    }
}

/**
 * 평가 카드가 떠 있고 보내는 중이 아닐 때만 입력 변경 [transform]을 적용한다.
 * 보내는 동안 바꾼 입력은 이미 보낸 내용과 달라 화면과 서버가 어긋나므로 무시한다.
 */
private fun MealDetailUIState.withEditableFeedback(
    transform: (AnalysisFeedbackUiState) -> AnalysisFeedbackUiState,
): MealDetailUIState = withFeedback { feedback -> if (feedback.isSubmitting) feedback else transform(feedback) }

/** 상세를 받고 평가 카드를 띄우기까지 기다리는 시간. 상세를 먼저 보게 한 뒤 묻는다. */
private const val FEEDBACK_PROMPT_DELAY_MILLIS = 1_500L
