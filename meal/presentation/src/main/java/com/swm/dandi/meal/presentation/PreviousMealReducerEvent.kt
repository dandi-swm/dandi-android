package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface PreviousMealReducerEvent : ReducerEvent {
    data object LoadStarted : PreviousMealReducerEvent
    data class PreviousMealPageLoaded(val foods: ImmutableList<PreviousFoodUiState>) : PreviousMealReducerEvent
    data class LoadFailed(val message: String) : PreviousMealReducerEvent
    data class FoodSelected(val foodId: String) : PreviousMealReducerEvent
    data class EatenTimeSelected(val hour: Int, val minute: Int) : PreviousMealReducerEvent
    data class ValidationFailed(
        val selectedFoodError: String,
        val eatenTimeError: String,
    ) : PreviousMealReducerEvent

    data object SubmitStarted : PreviousMealReducerEvent
    data class SubmitSucceeded(val foodName: String) : PreviousMealReducerEvent
    data class SubmitFailed(val message: String) : PreviousMealReducerEvent
    data object SubmitResultCleared : PreviousMealReducerEvent
}
