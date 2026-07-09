package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.ReducerEvent
import com.swm.dandi.meal.entity.MealAnalysisStatusVO

sealed interface NewFoodReducerEvent : ReducerEvent {
    data class FoodNameChanged(val foodName: String) : NewFoodReducerEvent
    data class PhotoAttachmentChanged(val isPhotoAttached: Boolean) : NewFoodReducerEvent
    data class EatenTimeSelected(val hour: Int, val minute: Int) : NewFoodReducerEvent
    data class ValidationFailed(
        val foodNameError: String,
        val photoError: String,
        val eatenTimeError: String,
    ) : NewFoodReducerEvent

    data object SubmitStarted : NewFoodReducerEvent
    data class SubmitSucceeded(
        val foodName: String,
        val mealRecordId: String,
        val analysisStatus: MealAnalysisStatusVO,
    ) : NewFoodReducerEvent

    data class AnalysisLoaded(
        val analysisStatus: MealAnalysisStatusVO,
    ) : NewFoodReducerEvent

    data class AnalysisFailed(val message: String) : NewFoodReducerEvent
    data class SubmitFailed(val message: String) : NewFoodReducerEvent
    data object AnalysisDismissed : NewFoodReducerEvent
    data object SubmitResultCleared : NewFoodReducerEvent
}
