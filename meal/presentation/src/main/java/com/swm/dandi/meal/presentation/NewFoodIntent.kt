package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.MviIntent

sealed interface NewFoodIntent : MviIntent {
    data object ClickBack : NewFoodIntent
    data class ChangeFoodName(val foodName: String) : NewFoodIntent
    data object TogglePhotoAttachment : NewFoodIntent
    data class SelectEatenTime(val hour: Int, val minute: Int) : NewFoodIntent
    data object SelectCurrentTime : NewFoodIntent
    data object Submit : NewFoodIntent
    data object DismissAnalysis : NewFoodIntent
    data object ClearSubmitResult : NewFoodIntent
}
