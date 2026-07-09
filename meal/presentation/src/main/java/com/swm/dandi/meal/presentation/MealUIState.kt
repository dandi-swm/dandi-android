package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.UiState

data class MealUIState(
    val showMealStatusSheet: Boolean = false,
    val mealStatusSheet: MealStatusSheetUiState = MealStatusSheetUiState(),
    val isMealStatusLoading: Boolean = false,
    val mealStatusLoadErrorMessage: String = "",
) : UiState {
    companion object {
        val empty: MealUIState = MealUIState()
    }
}
