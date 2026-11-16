package com.dandi.nyummy.history.presentation.detail.v2

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.history.domain.AwaitMealAnalysisUseCase
import com.dandi.nyummy.history.domain.DeleteMealFoodUseCase
import com.dandi.nyummy.history.domain.DeleteMealV2UseCase
import com.dandi.nyummy.history.domain.GetMealDetailV2UseCase
import com.dandi.nyummy.history.domain.MealAnalysisWaitResult
import com.dandi.nyummy.history.domain.SaveMealFoodListUseCase
import com.dandi.nyummy.history.domain.SubmitAnalysisFeedbackUseCase
import com.dandi.nyummy.history.domain.UpdateMealFoodUseCase
import com.dandi.nyummy.history.domain.UpdateMealNameV2UseCase
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodLineVO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.detail.AnalysisFeedbackUiState
import com.dandi.nyummy.history.presentation.detail.MealDetailMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 음식 목록 방식(V2) 식사 상세.
 *
 * 식사 이름 수정·식사 삭제·분석 평가는 기존 상세와 같고, 여기에 음식 목록 고치기가 더해진다.
 * - 순서 바꾸기·음식 추가는 목록 전체를, 한 줄 수정·삭제는 그 줄만 서버에 보낸다.
 * - 값을 새로 구해야 하는 변경(이름 변경, 음식 추가) 뒤에는 서버가 영양을 다시 계산한다. 그동안 분석 상태를 물으며
 *   기다리고([isCalculating][MealDetailV2UIState.isCalculating]), 끝나면 식사를 다시 받는다. 계산 중에는 음식 목록을 고칠 수 없다.
 * - 한 줄 수정은 응답에 본문이 없어 계산이 필요한지 알 수 없으므로, 고친 뒤 식사를 다시 받아 상태를 본다.
 * - 변경을 보내지 못하면 UseCase가 안내하고, 화면과 서버가 어긋나지 않게 식사를 다시 받는다.
 */
@HiltViewModel
class MealDetailV2ViewModel @Inject constructor(
    private val getMealDetail: GetMealDetailV2UseCase,
    private val updateMealName: UpdateMealNameV2UseCase,
    private val deleteMeal: DeleteMealV2UseCase,
    private val submitAnalysisFeedback: SubmitAnalysisFeedbackUseCase,
    private val saveMealFoodList: SaveMealFoodListUseCase,
    private val updateMealFood: UpdateMealFoodUseCase,
    private val deleteMealFood: DeleteMealFoodUseCase,
    private val awaitMealAnalysis: AwaitMealAnalysisUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
) : MviViewModel<MealDetailV2Intent, MealDetailV2UIState, MealDetailV2ReducerEvent>(MealDetailV2UIState()) {

    private var mealId: Long? = null
    private var loadJob: Job? = null
    private var pollJob: Job? = null

    override fun onIntent(intent: MealDetailV2Intent) {
        when (intent) {
            is MealDetailV2Intent.Enter -> enter(intent.mealId, intent.initial)
            MealDetailV2Intent.RetryLoad -> load()
            MealDetailV2Intent.ClickBack -> navigationHelper.navigateToBack()

            MealDetailV2Intent.ClickEditName -> dispatch(MealDetailV2ReducerEvent.NameEditStarted)
            is MealDetailV2Intent.ChangeNameDraft -> dispatch(MealDetailV2ReducerEvent.NameDraftChanged(intent.text))
            MealDetailV2Intent.ConfirmEditName -> confirmEditName()
            MealDetailV2Intent.CancelEditName -> dispatch(MealDetailV2ReducerEvent.NameEditCanceled)
            MealDetailV2Intent.ClickDelete -> dispatch(MealDetailV2ReducerEvent.DeleteRequested)
            MealDetailV2Intent.ConfirmDelete -> confirmDelete()
            MealDetailV2Intent.CancelDelete -> dispatch(MealDetailV2ReducerEvent.DeleteCanceled)

            is MealDetailV2Intent.ChangeFeedbackRating ->
                dispatch(MealDetailV2ReducerEvent.FeedbackRatingChanged(intent.halfSteps))
            is MealDetailV2Intent.SelectFeedbackReason ->
                dispatch(MealDetailV2ReducerEvent.FeedbackReasonSelected(intent.reason))
            is MealDetailV2Intent.ChangeFeedbackComment ->
                dispatch(MealDetailV2ReducerEvent.FeedbackCommentChanged(intent.text))
            MealDetailV2Intent.SubmitFeedback -> submitFeedback()
            MealDetailV2Intent.DismissFeedback -> dispatch(MealDetailV2ReducerEvent.FeedbackDismissed)

            is MealDetailV2Intent.ClickEditFood -> openFoodSheet(intent.mealFoodId)
            is MealDetailV2Intent.SelectIconCategory -> dispatch(MealDetailV2ReducerEvent.IconCategorySelected(intent.category))
            is MealDetailV2Intent.SelectIcon -> dispatch(MealDetailV2ReducerEvent.IconSelected(intent.iconId))
            is MealDetailV2Intent.ChangeFoodName -> dispatch(MealDetailV2ReducerEvent.FoodNameChanged(intent.text))
            is MealDetailV2Intent.StepPortion -> dispatch(MealDetailV2ReducerEvent.PortionStepped(intent.increase))
            is MealDetailV2Intent.SetManualNutrition -> dispatch(MealDetailV2ReducerEvent.ManualNutritionChanged(intent.enabled))
            is MealDetailV2Intent.ChangeNutrient ->
                dispatch(MealDetailV2ReducerEvent.NutrientChanged(intent.nutrient, intent.text))
            MealDetailV2Intent.ClickDeleteFood -> deleteSheetFood()
            MealDetailV2Intent.CancelFoodSheet ->
                if (!currentState.isSavingFoods) dispatch(MealDetailV2ReducerEvent.FoodSheetClosed)
            MealDetailV2Intent.SaveFoodSheet -> saveFoodSheet()

            is MealDetailV2Intent.MoveFood -> moveFood(intent.fromIndex, intent.toIndex)

            MealDetailV2Intent.ClickAddFood ->
                if (currentState.canEditFoods && currentState.canAddFood) dispatch(MealDetailV2ReducerEvent.AddNameInputOpened)
            is MealDetailV2Intent.ChangeAddName -> dispatch(MealDetailV2ReducerEvent.AddNameInputChanged(intent.text))
            MealDetailV2Intent.ConfirmAddFood -> addFood()
            MealDetailV2Intent.CancelAddFood ->
                if (!currentState.isSavingFoods) dispatch(MealDetailV2ReducerEvent.AddNameInputClosed)
        }
    }

    override fun reduce(state: MealDetailV2UIState, event: MealDetailV2ReducerEvent): MealDetailV2UIState =
        when (event) {
            MealDetailV2ReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)

            is MealDetailV2ReducerEvent.Loaded -> state.copy(
                meal = event.detail.meal,
                foods = event.detail.foods.toImmutableList(),
                isLoading = false,
                isLoadFailed = false,
                isSavingFoods = false,
                isCalculating = event.detail.meal.status.isCalculating,
            )

            // 이미 보여 주던 식사가 있으면 그대로 둔다(변경 뒤 다시 받기에 실패한 경우).
            MealDetailV2ReducerEvent.LoadFailed ->
                state.copy(isLoading = false, isLoadFailed = state.meal == null, isSavingFoods = false)

            MealDetailV2ReducerEvent.NameEditStarted -> state.meal?.let {
                state.copy(mode = MealDetailMode.EditingName, nameDraft = it.name)
            } ?: state

            is MealDetailV2ReducerEvent.NameDraftChanged ->
                if (event.text.length > MAX_MEAL_NAME_LENGTH) state else state.copy(nameDraft = event.text)

            is MealDetailV2ReducerEvent.NameEditCommitted -> state.copy(
                meal = state.meal?.copy(name = event.newName),
                mode = MealDetailMode.Viewing,
                nameDraft = "",
                isActionInFlight = false,
            )

            MealDetailV2ReducerEvent.NameEditCanceled ->
                if (state.isActionInFlight) state else state.copy(mode = MealDetailMode.Viewing, nameDraft = "")

            MealDetailV2ReducerEvent.DeleteRequested ->
                if (state.meal == null) state else state.copy(mode = MealDetailMode.ConfirmingDelete)

            MealDetailV2ReducerEvent.DeleteCanceled ->
                if (state.isActionInFlight) state else state.copy(mode = MealDetailMode.Viewing)

            MealDetailV2ReducerEvent.ActionStarted -> state.copy(isActionInFlight = true)
            MealDetailV2ReducerEvent.ActionFailed -> state.copy(isActionInFlight = false)

            MealDetailV2ReducerEvent.FeedbackPromptShown -> state.copy(feedback = AnalysisFeedbackUiState())
            is MealDetailV2ReducerEvent.FeedbackRatingChanged ->
                state.withEditableFeedback { it.copy(ratingHalfSteps = event.halfSteps) }
            is MealDetailV2ReducerEvent.FeedbackReasonSelected -> state.withEditableFeedback { feedback ->
                feedback.copy(selectedReason = event.reason.takeIf { it != feedback.selectedReason })
            }
            is MealDetailV2ReducerEvent.FeedbackCommentChanged -> state.withEditableFeedback { it.copy(comment = event.text) }
            MealDetailV2ReducerEvent.FeedbackSubmitStarted -> state.withFeedback { it.copy(isSubmitting = true) }
            MealDetailV2ReducerEvent.FeedbackSubmitFailed -> state.withFeedback { it.copy(isSubmitting = false) }
            MealDetailV2ReducerEvent.FeedbackSubmitted -> state.copy(feedback = null)
            MealDetailV2ReducerEvent.FeedbackDismissed ->
                if (state.feedback?.isSubmitting == true) state else state.copy(feedback = null)

            // 계산이 시작되면 열려 있던 시트와 입력 줄은 닫는다(계산 중에는 고칠 수 없다).
            MealDetailV2ReducerEvent.CalculationStarted ->
                state.copy(isCalculating = true, isSavingFoods = false, foodSheet = null, addNameInput = null)

            MealDetailV2ReducerEvent.FoodsSaveStarted -> state.copy(isSavingFoods = true)
            MealDetailV2ReducerEvent.FoodsSaveFailed -> state.copy(isSavingFoods = false)

            is MealDetailV2ReducerEvent.FoodSheetOpened ->
                state.copy(foodSheet = MealFoodSheetUiState.of(event.food, event.canDelete))
            MealDetailV2ReducerEvent.FoodSheetClosed -> state.copy(foodSheet = null)
            is MealDetailV2ReducerEvent.IconCategorySelected -> state.withEditableSheet { it.copy(iconCategory = event.category) }
            is MealDetailV2ReducerEvent.IconSelected -> state.withEditableSheet { it.copy(iconId = event.iconId) }
            is MealDetailV2ReducerEvent.FoodNameChanged -> state.withEditableSheet { it.withName(event.text) }
            is MealDetailV2ReducerEvent.PortionStepped -> state.withEditableSheet { it.withPortionStep(event.increase) }
            is MealDetailV2ReducerEvent.ManualNutritionChanged ->
                state.withEditableSheet { it.withManualNutrition(event.enabled) }
            is MealDetailV2ReducerEvent.NutrientChanged ->
                state.withEditableSheet { it.withNutrient(event.nutrient, event.text) }

            is MealDetailV2ReducerEvent.FoodsReordered -> state.copy(foods = event.foods.toImmutableList())

            MealDetailV2ReducerEvent.AddNameInputOpened -> state.copy(addNameInput = "")
            is MealDetailV2ReducerEvent.AddNameInputChanged ->
                if (state.addNameInput == null || state.isSavingFoods || event.text.length > MealFoodVO.MAX_NAME_LENGTH) {
                    state
                } else {
                    state.copy(addNameInput = event.text)
                }
            MealDetailV2ReducerEvent.AddNameInputClosed -> state.copy(addNameInput = null)
        }

    /** 화면이 다시 그려져 같은 식사로 또 들어와도 한 번만 받는다. */
    private fun enter(mealId: Long, initial: MealDetailV2VO?) {
        if (this.mealId == mealId) return
        this.mealId = mealId
        if (initial != null) onLoaded(initial, isFirstLoad = true) else load()
    }

    /** 식사를 받는다. 처음 받는 것이면 불러오는 중을 보여 주고, 아니면 보여 주던 내용을 그대로 둔 채 받는다. */
    private fun load() {
        val id = mealId ?: return
        val isFirstLoad = currentState.meal == null
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (isFirstLoad) dispatch(MealDetailV2ReducerEvent.LoadStarted)
            getMealDetail(id)
                .onSuccess { onLoaded(it, isFirstLoad) }
                .onFailure { dispatch(MealDetailV2ReducerEvent.LoadFailed) }
        }
    }

    /**
     * 받은 식사를 반영한다. 서버가 영양을 계산하는 중이면 끝나기를 기다리고, 처음 받은 완료 식사가 평가 대상이면
     * 상세를 잠깐 본 뒤 평가 카드를 띄운다. 음식 목록이 없는 식사(확인 대기, 인식 중, 실패)는 이 화면에서 보여 줄 것이 없어 돌아간다.
     */
    private fun onLoaded(detail: MealDetailV2VO, isFirstLoad: Boolean) {
        val status = detail.meal.status
        if (status != MealAnalysisStatus.COMPLETED && !status.isCalculating) {
            navigationHelper.navigateToBack()
            return
        }
        dispatch(MealDetailV2ReducerEvent.Loaded(detail))
        if (status.isCalculating) {
            awaitCalculation()
        } else if (isFirstLoad && detail.meal.canCreateAnalysisFeedback && currentState.feedback == null) {
            viewModelScope.launch {
                delay(FEEDBACK_PROMPT_DELAY_MILLIS)
                if (!currentState.isCalculating) dispatch(MealDetailV2ReducerEvent.FeedbackPromptShown)
            }
        }
    }

    /**
     * 서버가 영양을 다시 구하기를 기다린다. 끝나면 식사를 다시 받고, 실패했거나 너무 오래 걸리면 안내하고 기록 화면으로 돌아간다
     * (결과는 알림으로 오고, 실패한 식사는 기록 화면에서 다시 분석할 수 있다).
     */
    private fun awaitCalculation() {
        val id = mealId ?: return
        if (pollJob?.isActive == true) return
        dispatch(MealDetailV2ReducerEvent.CalculationStarted)
        pollJob = viewModelScope.launch {
            when (awaitMealAnalysis(id)) {
                MealAnalysisWaitResult.COMPLETED -> load()
                MealAnalysisWaitResult.FAILED -> leaveWithNotice(IconType.ERROR, R.string.history_detail_v2_calculation_failed)
                MealAnalysisWaitResult.TIMED_OUT -> leaveWithNotice(IconType.WARNING, R.string.history_detail_v2_calculation_slow)
            }
        }
    }

    private fun leaveWithNotice(iconType: IconType, messageRes: Int) {
        messageHelper.showSnackBar(iconType = iconType, messageRes = messageRes)
        navigationHelper.navigateToBack()
    }

    private fun confirmEditName() {
        val state = currentState
        if (state.isActionInFlight) return
        val newName = state.nameDraft.trim()
        val id = mealId ?: return
        if (newName.isEmpty()) return
        dispatch(MealDetailV2ReducerEvent.ActionStarted)
        viewModelScope.launch {
            updateMealName(id, newName)
                .onSuccess { dispatch(MealDetailV2ReducerEvent.NameEditCommitted(newName)) }
                .onFailure { dispatch(MealDetailV2ReducerEvent.ActionFailed) }
        }
    }

    /** 지우면 기록 화면으로 돌아간다. 기록 화면은 다시 보일 때 그날 목록을 다시 읽는다. */
    private fun confirmDelete() {
        if (currentState.isActionInFlight) return
        val id = mealId ?: return
        dispatch(MealDetailV2ReducerEvent.ActionStarted)
        viewModelScope.launch {
            deleteMeal(id)
                .onSuccess { navigationHelper.navigateToBack() }
                .onFailure { dispatch(MealDetailV2ReducerEvent.ActionFailed) }
        }
    }

    private fun submitFeedback() {
        val feedback = currentState.feedback ?: return
        if (!feedback.canSubmit) return
        val id = mealId ?: return
        dispatch(MealDetailV2ReducerEvent.FeedbackSubmitStarted)
        viewModelScope.launch {
            submitAnalysisFeedback(id, feedback.ratingHalfSteps, feedback.selectedReason, feedback.comment)
                .onSuccess { dispatch(MealDetailV2ReducerEvent.FeedbackSubmitted) }
                .onFailure { dispatch(MealDetailV2ReducerEvent.FeedbackSubmitFailed) }
        }
    }

    private fun openFoodSheet(mealFoodId: Long) {
        val state = currentState
        if (!state.canEditFoods) return
        val food = state.foods.firstOrNull { it.mealFoodId == mealFoodId } ?: return
        dispatch(MealDetailV2ReducerEvent.FoodSheetOpened(food = food, canDelete = state.foods.size > 1))
    }

    /** 시트에서 바꾼 항목만 보낸다. 바꾼 것이 없으면 시트만 닫는다. */
    private fun saveFoodSheet() {
        val state = currentState
        val sheet = state.foodSheet ?: return
        val id = mealId ?: return
        if (!state.canEditFoods || !sheet.canSave) return
        val change = sheet.toChange()
        if (change.isEmpty) {
            dispatch(MealDetailV2ReducerEvent.FoodSheetClosed)
            return
        }
        dispatch(MealDetailV2ReducerEvent.FoodsSaveStarted)
        viewModelScope.launch {
            updateMealFood(id, sheet.original.mealFoodId, change)
                .onSuccess {
                    dispatch(MealDetailV2ReducerEvent.FoodSheetClosed)
                    // 응답에 본문이 없어 계산이 필요한지 모른다. 다시 받아 상태를 본다.
                    load()
                }
                .onFailure { resyncAfterFailure() }
        }
    }

    private fun deleteSheetFood() {
        val state = currentState
        val sheet = state.foodSheet ?: return
        val id = mealId ?: return
        if (!state.canEditFoods || !sheet.canDelete) return
        dispatch(MealDetailV2ReducerEvent.FoodsSaveStarted)
        viewModelScope.launch {
            deleteMealFood(id, sheet.original.mealFoodId)
                .onSuccess {
                    dispatch(MealDetailV2ReducerEvent.FoodSheetClosed)
                    load()
                }
                .onFailure { resyncAfterFailure() }
        }
    }

    /** 끌어 놓은 순서를 화면에 먼저 반영하고 서버에 보낸다. 보내지 못하면 원래 순서로 되돌린다. */
    private fun moveFood(fromIndex: Int, toIndex: Int) {
        val state = currentState
        val id = mealId ?: return
        if (!state.canEditFoods || !state.canReorder) return
        if (fromIndex == toIndex || fromIndex !in state.foods.indices || toIndex !in state.foods.indices) return
        val previous = state.foods
        val moved = previous.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        dispatch(MealDetailV2ReducerEvent.FoodsReordered(moved))
        dispatch(MealDetailV2ReducerEvent.FoodsSaveStarted)
        viewModelScope.launch {
            saveMealFoodList(id, moved.map { it.toLine() })
                .onSuccess { status -> afterFoodListSaved(status) }
                .onFailure {
                    dispatch(MealDetailV2ReducerEvent.FoodsReordered(previous))
                    resyncAfterFailure()
                }
        }
    }

    /** 이름을 적은 음식을 1인분으로 목록 끝에 추가한다. 서버가 영양을 구할 때까지 기다린다. */
    private fun addFood() {
        val state = currentState
        val id = mealId ?: return
        if (!state.canConfirmAdd) return
        val name = state.addNameInput?.trim().orEmpty()
        if (name.isEmpty()) return
        dispatch(MealDetailV2ReducerEvent.FoodsSaveStarted)
        viewModelScope.launch {
            saveMealFoodList(id, state.foods.map { it.toLine() } + MealFoodLineVO(name = name))
                .onSuccess { status ->
                    dispatch(MealDetailV2ReducerEvent.AddNameInputClosed)
                    afterFoodListSaved(status)
                }
                .onFailure { resyncAfterFailure() }
        }
    }

    /** 목록을 보낸 뒤: 값을 새로 구할 것이 없으면 바로 다시 받고, 있으면 계산을 기다린다. */
    private fun afterFoodListSaved(status: MealAnalysisStatus) {
        if (status == MealAnalysisStatus.COMPLETED) load() else awaitCalculation()
    }

    /** 변경을 보내지 못했다(안내는 UseCase가 했다). 화면이 낡았을 수 있어 식사를 다시 받는다. */
    private fun resyncAfterFailure() {
        dispatch(MealDetailV2ReducerEvent.FoodsSaveFailed)
        load()
    }
}

private fun MealFoodVO.toLine() = MealFoodLineVO(mealFoodId = mealFoodId, name = name, portion = portion)

/** 평가 카드가 떠 있고 보내는 중이 아닐 때만 입력 변경을 적용한다. */
private fun MealDetailV2UIState.withEditableFeedback(
    transform: (AnalysisFeedbackUiState) -> AnalysisFeedbackUiState,
): MealDetailV2UIState = withFeedback { feedback -> if (feedback.isSubmitting) feedback else transform(feedback) }

/** 시트가 열려 있고 변경을 보내는 중이 아닐 때만 입력 변경을 적용한다. */
private fun MealDetailV2UIState.withEditableSheet(
    transform: (MealFoodSheetUiState) -> MealFoodSheetUiState,
): MealDetailV2UIState = if (isSavingFoods) this else withFoodSheet(transform)

/** 상세를 받고 평가 카드를 띄우기까지 기다리는 시간. 상세를 먼저 보게 한 뒤 묻는다. */
private const val FEEDBACK_PROMPT_DELAY_MILLIS = 1_500L

/** 식사 대표 이름 최대 길이. */
internal const val MAX_MEAL_NAME_LENGTH = 20
