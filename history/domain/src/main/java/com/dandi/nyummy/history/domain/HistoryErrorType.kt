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

    /** 분석 실패 상태가 아닌 식사를 다시 분석하려 할 때(409). */
    ANALYSIS_NOT_RETRYABLE(
        type = "api.meal.analysisNotRetryable",
        errorMsg = "이미 분석하고 있는 기록이에요",
    ),
}
