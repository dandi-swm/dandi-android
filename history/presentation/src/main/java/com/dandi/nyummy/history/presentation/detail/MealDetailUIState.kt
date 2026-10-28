package com.dandi.nyummy.history.presentation.detail

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.MealHistoryVO

/**
 * 식사 상세 화면 상태.
 *
 * @property meal 받은 식사. null 이면 아직 받지 못했습니다([isLoading] 또는 [isLoadFailed]).
 * @property nameDraft 이름 수정 다이얼로그의 입력값. [MealDetailMode.EditingName]에서만 의미가 있습니다.
 * @property isActionInFlight 이름 수정이나 삭제를 서버에 보내는 중. 그동안 버튼과 닫기를 막습니다.
 * @property feedback AI 분석 평가 카드. null 이면 카드를 보여 주지 않습니다.
 */
data class MealDetailUIState(
    val meal: MealHistoryVO? = null,
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val mode: MealDetailMode = MealDetailMode.Viewing,
    val nameDraft: String = "",
    val isActionInFlight: Boolean = false,
    val feedback: AnalysisFeedbackUiState? = null,
) : UiState {
    fun withFeedback(transform: (AnalysisFeedbackUiState) -> AnalysisFeedbackUiState): MealDetailUIState =
        feedback?.let { copy(feedback = transform(it)) } ?: this
}

/**
 * AI 분석 평가 카드 상태.
 *
 * @property ratingHalfSteps 고른 별점(반 칸 수, 1~10 = 0.5~5.0점). 0 이면 아직 고르지 않았습니다.
 * @property selectedReason 고른 아쉬운 점(선택).
 * @property comment 직접 쓴 내용(선택).
 * @property isSubmitting 보내는 중.
 */
data class AnalysisFeedbackUiState(
    val ratingHalfSteps: Int = 0,
    val selectedReason: AnalysisFeedbackReason? = null,
    val comment: String = "",
    val isSubmitting: Boolean = false,
) {
    val isRated: Boolean
        get() = ratingHalfSteps > 0

    val canSubmit: Boolean
        get() = isRated && !isSubmitting
}

enum class MealDetailMode {
    Viewing,
    EditingName,
    ConfirmingDelete,
}
