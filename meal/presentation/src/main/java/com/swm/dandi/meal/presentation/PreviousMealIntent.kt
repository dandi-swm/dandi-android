package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.MviIntent

sealed interface PreviousMealIntent : MviIntent {
    data object ClickBack : PreviousMealIntent
    data object ClickNewFood : PreviousMealIntent
    data class SelectFood(val foodId: String) : PreviousMealIntent
    data class QuickRecord(val foodId: String) : PreviousMealIntent
    data class SelectEatenTime(val hour: Int, val minute: Int) : PreviousMealIntent
    data object SelectCurrentTime : PreviousMealIntent
    data object Submit : PreviousMealIntent
    data object ClearSubmitResult : PreviousMealIntent
}
