package com.dandi.nyummy.history.presentation.detail.v2

import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason

sealed interface MealDetailV2ReducerEvent : ReducerEvent {

    data object LoadStarted : MealDetailV2ReducerEvent

    /** 상세를 받았습니다. 음식 목록 변경을 보내던 중이었으면 끝난 것으로 봅니다. */
    data class Loaded(val detail: MealDetailV2VO) : MealDetailV2ReducerEvent
    data object LoadFailed : MealDetailV2ReducerEvent

    data object NameEditStarted : MealDetailV2ReducerEvent
    data class NameDraftChanged(val text: String) : MealDetailV2ReducerEvent
    data class NameEditCommitted(val newName: String) : MealDetailV2ReducerEvent
    data object NameEditCanceled : MealDetailV2ReducerEvent
    data object DeleteRequested : MealDetailV2ReducerEvent
    data object DeleteCanceled : MealDetailV2ReducerEvent
    data object ActionStarted : MealDetailV2ReducerEvent
    data object ActionFailed : MealDetailV2ReducerEvent

    data object FeedbackPromptShown : MealDetailV2ReducerEvent
    data class FeedbackRatingChanged(val halfSteps: Int) : MealDetailV2ReducerEvent
    data class FeedbackReasonSelected(val reason: AnalysisFeedbackReason) : MealDetailV2ReducerEvent
    data class FeedbackCommentChanged(val text: String) : MealDetailV2ReducerEvent
    data object FeedbackSubmitStarted : MealDetailV2ReducerEvent
    data object FeedbackSubmitFailed : MealDetailV2ReducerEvent
    data object FeedbackSubmitted : MealDetailV2ReducerEvent
    data object FeedbackDismissed : MealDetailV2ReducerEvent

    /** 서버가 영양을 다시 구하기 시작했습니다(또는 구하는 중인 식사를 열었습니다). */
    data object CalculationStarted : MealDetailV2ReducerEvent

    /** 음식 목록 변경을 서버에 보내기 시작했습니다. */
    data object FoodsSaveStarted : MealDetailV2ReducerEvent

    /** 음식 목록 변경을 보내지 못했습니다. */
    data object FoodsSaveFailed : MealDetailV2ReducerEvent

    /** 음식 고치기 시트를 열었습니다. */
    data class FoodSheetOpened(val food: MealFoodVO, val canDelete: Boolean) : MealDetailV2ReducerEvent
    data object FoodSheetClosed : MealDetailV2ReducerEvent
    data class IconCategorySelected(val category: NyummyFoodIconCategory?) : MealDetailV2ReducerEvent
    data class IconSelected(val iconId: String) : MealDetailV2ReducerEvent
    data class FoodNameChanged(val text: String) : MealDetailV2ReducerEvent
    data class PortionStepped(val increase: Boolean) : MealDetailV2ReducerEvent
    data class ManualNutritionChanged(val enabled: Boolean) : MealDetailV2ReducerEvent
    data class NutrientChanged(val nutrient: MealFoodNutrient, val text: String) : MealDetailV2ReducerEvent

    /** 음식 줄의 순서를 화면에서 먼저 바꿨습니다(또는 저장에 실패해 되돌렸습니다). */
    data class FoodsReordered(val foods: List<MealFoodVO>) : MealDetailV2ReducerEvent

    data object AddNameInputOpened : MealDetailV2ReducerEvent
    data class AddNameInputChanged(val text: String) : MealDetailV2ReducerEvent
    data object AddNameInputClosed : MealDetailV2ReducerEvent
}
