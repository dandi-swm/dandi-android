package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.ReducerEvent
import com.swm.dandi.meal.entity.MealSessionStatusTypeVO

sealed interface MealReducerEvent : ReducerEvent {
    data object MealStatusSheetOpened : MealReducerEvent
    data object MealStatusRecordTypeRequested : MealReducerEvent
    data object MealStatusSheetDismissed : MealReducerEvent
    data object MealStatusLoadStarted : MealReducerEvent
    data class MealStatusLoaded(val mealStatusSheet: MealStatusSheetUiState) : MealReducerEvent
    data class MealStatusLoadFailed(val message: String) : MealReducerEvent
    data class MealStatusAnalysisChanged(
        val mealRecordId: String,
        val status: MealSessionStatusTypeVO,
    ) : MealReducerEvent
}
