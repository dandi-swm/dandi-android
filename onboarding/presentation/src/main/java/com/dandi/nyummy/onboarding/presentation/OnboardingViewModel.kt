package com.dandi.nyummy.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.onboarding.domain.CatNameValidator
import com.dandi.nyummy.onboarding.domain.FinishOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val finishOnboardingUseCase: FinishOnboardingUseCase,
) : MviViewModel<OnboardingIntent, OnboardingUIState, OnboardingReducerEvent>(OnboardingUIState.empty) {

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.TapDialogue -> advance()
            OnboardingIntent.TypingFinished -> dispatch(OnboardingReducerEvent.LineRevealed)
            is OnboardingIntent.SelectChoice -> selectChoice(intent.index)
            is OnboardingIntent.InputCatName -> dispatch(OnboardingReducerEvent.CatNameChanged(intent.value))
            OnboardingIntent.SubmitCatName -> submitCatName()
            OnboardingIntent.Skip -> if (currentState.isSkipVisible) dispatch(OnboardingReducerEvent.SkippedToNaming)
            OnboardingIntent.ClickStart -> start()
            is OnboardingIntent.ClickMealTime -> if (currentState.isMealTimeEditable) {
                dispatch(OnboardingReducerEvent.MealTimeEditStarted(intent.meal))
            }
            is OnboardingIntent.SelectMealTime -> dispatch(OnboardingReducerEvent.MealTimeChanged(intent.meal, intent.time))
            OnboardingIntent.DismissMealTimeSheet -> dispatch(OnboardingReducerEvent.MealTimeEditDismissed)
            OnboardingIntent.ConfirmMealTimes -> finish()
        }
    }

    override fun reduce(state: OnboardingUIState, event: OnboardingReducerEvent): OnboardingUIState = when (event) {
        OnboardingReducerEvent.LineRevealed -> state.copy(isLineRevealed = true)
        OnboardingReducerEvent.NextLine -> state.copy(lineIndex = state.lineIndex + 1, isLineRevealed = false)
        OnboardingReducerEvent.NextScene -> state.toScene(state.sceneIndex + 1)
        is OnboardingReducerEvent.ChoiceSelected -> state.copy(
            choiceIndex = event.index,
            lineIndex = 0,
            isLineRevealed = false,
        )
        OnboardingReducerEvent.SkippedToNaming -> state.toScene(OnboardingScript.namingSceneIndex)
        is OnboardingReducerEvent.CatNameChanged -> state.copy(catNameInput = event.value, catNameError = null)
        is OnboardingReducerEvent.CatNameInvalid -> state.copy(catNameError = event.error)
        is OnboardingReducerEvent.CatNamed -> state
            .toScene(state.sceneIndex + 1)
            .copy(catName = event.name, catNameError = null)
        is OnboardingReducerEvent.MealTimeEditStarted -> state.copy(editingMeal = event.meal)
        is OnboardingReducerEvent.MealTimeChanged -> state.copy(
            mealTimes = state.mealTimes.with(event.meal, event.time),
            editingMeal = null,
        )
        OnboardingReducerEvent.MealTimeEditDismissed -> state.copy(editingMeal = null)
        OnboardingReducerEvent.FinishStarted -> state.copy(isFinishing = true, editingMeal = null)
        OnboardingReducerEvent.FinishFailed -> state.copy(isFinishing = false)
    }

    private fun OnboardingUIState.toScene(index: Int) = copy(
        sceneIndex = index.coerceAtMost(OnboardingScript.scenes.lastIndex),
        lineIndex = 0,
        choiceIndex = null,
        isLineRevealed = false,
    )

    private fun advance() {
        val state = currentState
        when {
            !state.isLineRevealed -> dispatch(OnboardingReducerEvent.LineRevealed)
            !state.isLastLine -> dispatch(OnboardingReducerEvent.NextLine)
            // 선택지·이름 입력·시작 버튼이 떠 있으면 탭으로 넘기지 않고 사용자의 행동을 기다린다.
            state.isActionVisible -> Unit
            state.currentScene.action == OnboardingSceneAction.Start -> Unit
            state.currentScene.action == OnboardingSceneAction.MealTime -> Unit
            else -> dispatch(OnboardingReducerEvent.NextScene)
        }
    }

    /** "함께 시작하기"는 평소 식사 시각 장면으로 넘어간다. */
    private fun start() {
        val state = currentState
        if (state.currentScene.action == OnboardingSceneAction.Start && state.isActionVisible) {
            dispatch(OnboardingReducerEvent.NextScene)
        }
    }

    /** 이름과 식사 시각으로 고양이를 만들고 홈으로 간다. 실패하면 이 장면에 머물러 다시 누를 수 있다. */
    private fun finish() {
        val state = currentState
        if (state.isFinishing || state.currentScene.action != OnboardingSceneAction.MealTime) return
        dispatch(OnboardingReducerEvent.FinishStarted)
        viewModelScope.launch {
            finishOnboardingUseCase(state.catName, state.mealTimes)
                .onFailure { dispatch(OnboardingReducerEvent.FinishFailed) }
        }
    }

    private fun selectChoice(index: Int) {
        val action = currentState.currentScene.action as? OnboardingSceneAction.Choice ?: return
        if (!currentState.isActionVisible || index !in action.options.indices) return
        dispatch(OnboardingReducerEvent.ChoiceSelected(index))
    }

    /** 이름은 여기서 검사만 하고 넘어간다. 서버에는 마지막 장면에서 식사 시각과 함께 보낸다. */
    private fun submitCatName() {
        val state = currentState
        if (state.currentScene.action != OnboardingSceneAction.NameInput) return
        CatNameValidator.validate(state.catNameInput)?.let { error ->
            dispatch(OnboardingReducerEvent.CatNameInvalid(error))
            return
        }
        dispatch(OnboardingReducerEvent.CatNamed(state.catNameInput.trim()))
    }
}
