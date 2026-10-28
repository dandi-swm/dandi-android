package com.dandi.nyummy.history.presentation.detail

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.MealHistoryVO

sealed interface MealDetailReducerEvent : ReducerEvent {
    data object LoadStarted : MealDetailReducerEvent
    data class Loaded(val meal: MealHistoryVO) : MealDetailReducerEvent
    data object LoadFailed : MealDetailReducerEvent

    data object NameEditStarted : MealDetailReducerEvent
    data class NameDraftChanged(val text: String) : MealDetailReducerEvent
    data class NameEditCommitted(val newName: String) : MealDetailReducerEvent
    data object NameEditCanceled : MealDetailReducerEvent

    data object DeleteRequested : MealDetailReducerEvent
    data object DeleteCanceled : MealDetailReducerEvent

    data object ActionStarted : MealDetailReducerEvent
    data object ActionFailed : MealDetailReducerEvent

    data object FeedbackPromptShown : MealDetailReducerEvent
    data class FeedbackRatingChanged(val halfSteps: Int) : MealDetailReducerEvent
    data class FeedbackReasonSelected(val reason: AnalysisFeedbackReason) : MealDetailReducerEvent
    data class FeedbackCommentChanged(val text: String) : MealDetailReducerEvent
    data object FeedbackSubmitStarted : MealDetailReducerEvent
    data object FeedbackSubmitFailed : MealDetailReducerEvent
    data object FeedbackSubmitted : MealDetailReducerEvent
    data object FeedbackDismissed : MealDetailReducerEvent
}
