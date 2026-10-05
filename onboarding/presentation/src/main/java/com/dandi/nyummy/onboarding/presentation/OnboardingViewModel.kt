package com.dandi.nyummy.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.onboarding.domain.CatNameValidator
import com.dandi.nyummy.onboarding.domain.FinishOnboardingUseCase
import com.dandi.nyummy.onboarding.domain.RegisterCatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val registerCatUseCase: RegisterCatUseCase,
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
            OnboardingIntent.ClickStart -> finishOnboardingUseCase()
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
        OnboardingReducerEvent.SubmitStarted -> state.copy(isSubmitting = true, catNameError = null)
        is OnboardingReducerEvent.CatRegistered -> state
            .toScene(state.sceneIndex + 1)
            .copy(catName = event.name, isSubmitting = false)
        OnboardingReducerEvent.SubmitFailed -> state.copy(isSubmitting = false)
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
            state.isSubmitting -> Unit
            !state.isLineRevealed -> dispatch(OnboardingReducerEvent.LineRevealed)
            !state.isLastLine -> dispatch(OnboardingReducerEvent.NextLine)
            // 선택지·이름 입력·시작 버튼이 떠 있으면 탭으로 넘기지 않고 사용자의 행동을 기다린다.
            state.isActionVisible -> Unit
            state.currentScene.action == OnboardingSceneAction.Start -> Unit
            else -> dispatch(OnboardingReducerEvent.NextScene)
        }
    }

    private fun selectChoice(index: Int) {
        val action = currentState.currentScene.action as? OnboardingSceneAction.Choice ?: return
        if (!currentState.isActionVisible || index !in action.options.indices) return
        dispatch(OnboardingReducerEvent.ChoiceSelected(index))
    }

    private fun submitCatName() {
        val state = currentState
        if (state.isSubmitting || state.currentScene.action != OnboardingSceneAction.NameInput) return
        CatNameValidator.validate(state.catNameInput)?.let { error ->
            dispatch(OnboardingReducerEvent.CatNameInvalid(error))
            return
        }
        dispatch(OnboardingReducerEvent.SubmitStarted)
        viewModelScope.launch {
            registerCatUseCase(state.catNameInput)
                .onSuccess { cat -> dispatch(OnboardingReducerEvent.CatRegistered(cat.name.ifBlank { state.catNameInput.trim() })) }
                .onFailure { dispatch(OnboardingReducerEvent.SubmitFailed) }
        }
    }
}
