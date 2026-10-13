package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 기록 탭 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 * 서버의 MealErrorCode가 바뀌면 여기도 같이 고친다.
 */
enum class HistoryErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 다른 기기에서 지웠거나 이미 지운 기록(404). 공통 404 안내("준비 중인 기능")보다 먼저 처리한다. */
    MEAL_NOT_FOUND(
        type = "api.meal.notFound",
        errorMsg = "이미 지워진 기록이에요",
    ),

    /**
     * 분석 실패(FAILED) 상태가 아닌 식사를 다시 분석하려 할 때(409).
     * 재분석 버튼은 실패한 식사에만 보이므로, 다른 곳에서 이미 다시 분석했거나 분석이 끝나 목록이 낡은 경우다.
     */
    ANALYSIS_NOT_RETRYABLE(
        type = "api.meal.analysisNotRetryable",
        errorMsg = "이미 다시 분석한 기록이에요",
    ),

    /** 별점이 0.5 ~ 5.0, 0.5 단위가 아닐 때(400). 화면이 반 칸 단위로만 고르게 하므로 정상 흐름에서는 나오지 않는다. */
    INVALID_RATING(
        type = "api.meal.invalidRating",
        errorMsg = "별점을 다시 골라 주세요",
    ),

    /** 이미 평가한 식사(409). 다른 기기에서 먼저 평가한 경우라 평가는 끝난 것으로 본다. */
    ANALYSIS_FEEDBACK_ALREADY_EXISTS(
        type = "api.meal.analysisFeedbackAlreadyExists",
        errorMsg = "이미 평가한 식사예요",
    ),

    /** 분석이 끝나지 않은 식사를 평가하려 할 때(409). */
    ANALYSIS_NOT_COMPLETED(
        type = "api.meal.analysisNotCompleted",
        errorMsg = "분석이 끝난 식사만 평가할 수 있어요",
    ),
}
