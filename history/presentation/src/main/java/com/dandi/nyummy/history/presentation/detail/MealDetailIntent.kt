package com.dandi.nyummy.history.presentation.detail

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason

sealed interface MealDetailIntent : MviIntent {

    /** 화면에 들어왔습니다. 같은 식사를 다시 받으면 무시합니다. */
    data class Enter(val mealId: Long) : MealDetailIntent

    /** 불러오기에 실패한 뒤 다시 시도를 눌렀습니다. */
    data object RetryLoad : MealDetailIntent

    data object ClickBack : MealDetailIntent

    data object ClickEditName : MealDetailIntent

    data class ChangeNameDraft(val text: String) : MealDetailIntent

    data object ConfirmEditName : MealDetailIntent

    data object CancelEditName : MealDetailIntent

    data object ClickDelete : MealDetailIntent

    /** 삭제 확인 다이얼로그에서 삭제를 확정했습니다. 지우면 기록 화면으로 돌아갑니다. */
    data object ConfirmDelete : MealDetailIntent

    data object CancelDelete : MealDetailIntent

    /** 평가 카드에서 별점을 골랐습니다. [halfSteps]는 반 칸 수(1~10)입니다. */
    data class ChangeFeedbackRating(val halfSteps: Int) : MealDetailIntent

    /** 평가 카드에서 아쉬운 점을 골랐습니다. 이미 고른 것을 다시 누르면 선택을 풉니다. */
    data class SelectFeedbackReason(val reason: AnalysisFeedbackReason) : MealDetailIntent

    data class ChangeFeedbackComment(val text: String) : MealDetailIntent

    data object SubmitFeedback : MealDetailIntent

    /** 평가 카드를 닫았습니다. 아무것도 보내지 않습니다. */
    data object DismissFeedback : MealDetailIntent
}
