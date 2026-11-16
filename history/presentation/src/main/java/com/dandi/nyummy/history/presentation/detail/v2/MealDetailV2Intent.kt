package com.dandi.nyummy.history.presentation.detail.v2

import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason

sealed interface MealDetailV2Intent : MviIntent {

    /** 화면에 들어왔습니다. [initial]은 들어오기 전에 이미 받아 둔 상세로, 있으면 다시 조회하지 않습니다. */
    data class Enter(val mealId: Long, val initial: MealDetailV2VO? = null) : MealDetailV2Intent

    /** 불러오지 못한 뒤 `다시 불러오기`를 눌렀습니다. */
    data object RetryLoad : MealDetailV2Intent

    data object ClickBack : MealDetailV2Intent

    /** 식사 이름 옆 연필을 눌렀습니다. */
    data object ClickEditName : MealDetailV2Intent
    data class ChangeNameDraft(val text: String) : MealDetailV2Intent
    data object ConfirmEditName : MealDetailV2Intent
    data object CancelEditName : MealDetailV2Intent

    /** 오른쪽 위 휴지통을 눌렀습니다. */
    data object ClickDelete : MealDetailV2Intent
    data object ConfirmDelete : MealDetailV2Intent
    data object CancelDelete : MealDetailV2Intent

    data class ChangeFeedbackRating(val halfSteps: Int) : MealDetailV2Intent
    data class SelectFeedbackReason(val reason: AnalysisFeedbackReason) : MealDetailV2Intent
    data class ChangeFeedbackComment(val text: String) : MealDetailV2Intent
    data object SubmitFeedback : MealDetailV2Intent
    data object DismissFeedback : MealDetailV2Intent

    /** 음식 줄의 연필을 눌렀습니다. 음식 고치기 시트를 엽니다. */
    data class ClickEditFood(val mealFoodId: Long) : MealDetailV2Intent

    /** 시트에서 아이콘 분류 칩을 눌렀습니다. null이면 "전체"입니다. */
    data class SelectIconCategory(val category: NyummyFoodIconCategory?) : MealDetailV2Intent
    data class SelectIcon(val iconId: String) : MealDetailV2Intent
    data class ChangeFoodName(val text: String) : MealDetailV2Intent

    /** 시트에서 인분의 +(또는 −)를 눌렀습니다. */
    data class StepPortion(val increase: Boolean) : MealDetailV2Intent

    /** 시트에서 `직접 설정하기`를 켜거나 껐습니다. */
    data class SetManualNutrition(val enabled: Boolean) : MealDetailV2Intent
    data class ChangeNutrient(val nutrient: MealFoodNutrient, val text: String) : MealDetailV2Intent

    /** 시트에서 `이 음식 지우기`를 눌렀습니다. */
    data object ClickDeleteFood : MealDetailV2Intent

    /** 시트에서 `취소`를 눌렀거나 시트를 닫았습니다. */
    data object CancelFoodSheet : MealDetailV2Intent
    data object SaveFoodSheet : MealDetailV2Intent

    /** 음식 줄을 끌어 [fromIndex]에서 [toIndex]로 옮겨 놓았습니다. */
    data class MoveFood(val fromIndex: Int, val toIndex: Int) : MealDetailV2Intent

    /** `음식 추가`를 눌렀습니다. */
    data object ClickAddFood : MealDetailV2Intent
    data class ChangeAddName(val text: String) : MealDetailV2Intent
    data object ConfirmAddFood : MealDetailV2Intent
    data object CancelAddFood : MealDetailV2Intent
}
