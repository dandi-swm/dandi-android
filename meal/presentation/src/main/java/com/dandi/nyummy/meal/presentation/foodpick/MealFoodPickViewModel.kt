package com.dandi.nyummy.meal.presentation.foodpick

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.meal.domain.GetFoundFoodsUseCase
import com.dandi.nyummy.meal.domain.SubmitMealFoodsUseCase
import com.dandi.nyummy.meal.presentation.model.FoodPickDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 기록 탭의 확인 대기 식사에서 들어온 찾은 음식 고르기.
 *
 * - 들어오면 그 식사에서 찾은 음식을 불러온다. 그 사이 서버가 영양 계산을 시작했거나 식사가 지워졌으면
 *   UseCase가 안내하고, 화면은 기록 탭으로 돌아간다.
 * - 가져가면 연출 없이 기록 탭으로 돌아간다. 기록 탭은 돌아올 때 목록을 다시 읽어 분석 중 카드로 바뀐다.
 * - 닫기와 뒤로 가기는 기록을 지우지 않는다(기록 직후의 고르기와 다르다).
 */
@HiltViewModel
class MealFoodPickViewModel @Inject constructor(
    private val getFoundFoods: GetFoundFoodsUseCase,
    private val submitMealFoods: SubmitMealFoodsUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealFoodPickIntent, MealFoodPickUIState, MealFoodPickReducerEvent>(MealFoodPickUIState()) {

    private var mealId: Long? = null

    override fun onIntent(intent: MealFoodPickIntent) {
        when (intent) {
            is MealFoodPickIntent.Enter -> enter(intent.mealId)
            MealFoodPickIntent.ClickRetry -> load()
            MealFoodPickIntent.ClickClose -> if (!currentState.isSubmitting) navigationHelper.navigateToBack()
            is MealFoodPickIntent.ToggleFood -> whenEditable { dispatch(MealFoodPickReducerEvent.FoodToggled(intent.key)) }
            MealFoodPickIntent.OpenNameInput -> whenEditable { dispatch(MealFoodPickReducerEvent.NameInputOpened) }
            is MealFoodPickIntent.ChangeNameInput ->
                whenEditable { dispatch(MealFoodPickReducerEvent.NameInputChanged(intent.text)) }
            MealFoodPickIntent.ConfirmNameInput -> whenEditable { dispatch(MealFoodPickReducerEvent.NameInputConfirmed) }
            MealFoodPickIntent.ClickSubmit -> submit()
        }
    }

    override fun reduce(state: MealFoodPickUIState, event: MealFoodPickReducerEvent): MealFoodPickUIState =
        when (event) {
            MealFoodPickReducerEvent.LoadStarted -> state.copy(isLoading = true, isLoadFailed = false)
            is MealFoodPickReducerEvent.Loaded ->
                state.copy(isLoading = false, isLoadFailed = false, draft = FoodPickDraft.of(event.foods))
            MealFoodPickReducerEvent.LoadFailed -> state.copy(isLoading = false, isLoadFailed = true)
            is MealFoodPickReducerEvent.FoodToggled -> state.copy(draft = state.draft.toggle(event.key))
            MealFoodPickReducerEvent.NameInputOpened -> state.copy(draft = state.draft.openNameInput())
            is MealFoodPickReducerEvent.NameInputChanged -> state.copy(draft = state.draft.changeNameInput(event.text))
            MealFoodPickReducerEvent.NameInputConfirmed -> state.copy(draft = state.draft.confirmNameInput())
            MealFoodPickReducerEvent.SubmitStarted -> state.copy(isSubmitting = true)
            MealFoodPickReducerEvent.SubmitFailed -> state.copy(isSubmitting = false)
        }

    /** 화면이 다시 그려져 같은 식사로 또 들어와도 한 번만 불러온다. */
    private fun enter(mealId: Long) {
        if (this.mealId == mealId) return
        this.mealId = mealId
        load()
    }

    private fun load() {
        val id = mealId ?: return
        dispatch(MealFoodPickReducerEvent.LoadStarted)
        viewModelScope.launch {
            getFoundFoods(id)
                .onSuccess { confirm ->
                    // 더는 고를 수 없는 식사는 UseCase가 이미 안내했다.
                    if (confirm.isConfirmable && confirm.foods.isNotEmpty()) {
                        dispatch(MealFoodPickReducerEvent.Loaded(confirm.foods))
                    } else {
                        navigationHelper.navigateToBack()
                    }
                }
                .onFailure { dispatch(MealFoodPickReducerEvent.LoadFailed) }
        }
    }

    private fun submit() {
        val id = mealId ?: return
        val state = currentState
        if (state.isLoading || state.isLoadFailed || state.isSubmitting || !state.draft.canSubmit) return
        dispatch(MealFoodPickReducerEvent.SubmitStarted)
        viewModelScope.launch {
            submitMealFoods(id, state.draft.toFoods())
                // 받았든 더는 고를 수 없든 이 화면에서 할 일은 끝났다.
                .onSuccess { navigationHelper.navigateToBack() }
                .onFailure { dispatch(MealFoodPickReducerEvent.SubmitFailed) }
        }
    }

    /** 불러오는 중이거나 보내는 중에는 목록을 고치지 않는다. */
    private inline fun whenEditable(block: () -> Unit) {
        val state = currentState
        if (!state.isLoading && !state.isLoadFailed && !state.isSubmitting) block()
    }
}
