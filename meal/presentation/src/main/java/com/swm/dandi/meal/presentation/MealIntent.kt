package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.MviIntent

sealed interface MealIntent : MviIntent {
    data object ClickMealStatus : MealIntent
    data object ClickMealStatusRecord : MealIntent
    data class ClickRetryMealAnalysis(val mealRecordId: String) : MealIntent
    data object DismissMealStatus : MealIntent
    data object ClickPreviousMeal : MealIntent
    data object ClickNewFood : MealIntent
}
