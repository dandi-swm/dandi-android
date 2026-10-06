package com.dandi.nyummy.onboarding.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.onboarding.domain.CatNameError
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO

sealed interface OnboardingReducerEvent : ReducerEvent {
    data object LineRevealed : OnboardingReducerEvent
    data object NextLine : OnboardingReducerEvent
    data object NextScene : OnboardingReducerEvent
    data class ChoiceSelected(val index: Int) : OnboardingReducerEvent
    data object SkippedToNaming : OnboardingReducerEvent
    data class CatNameChanged(val value: String) : OnboardingReducerEvent
    data class CatNameInvalid(val error: CatNameError) : OnboardingReducerEvent
    data object SubmitStarted : OnboardingReducerEvent
    data class CatRegistered(val name: String) : OnboardingReducerEvent
    data object SubmitFailed : OnboardingReducerEvent
    data class MealTimeEditStarted(val meal: Meal) : OnboardingReducerEvent
    data class MealTimeChanged(val meal: Meal, val time: MealTimeVO) : OnboardingReducerEvent
    data object MealTimeEditDismissed : OnboardingReducerEvent
    data object FinishStarted : OnboardingReducerEvent
}
