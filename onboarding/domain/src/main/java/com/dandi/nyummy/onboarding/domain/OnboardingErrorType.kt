package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 고양이 생성 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 */
enum class OnboardingErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /**
     * 이름이나 식사 시각이 서버 규칙(이름 1~20자, 시각 0~23, 끼니끼리 겹치지 않음)에 맞지 않다(400).
     * 앱이 먼저 검사하므로 보통은 오지 않는다.
     */
    INVALID_INPUT(
        type = "api.common.invalidInputValue",
        errorMsg = "이름이나 식사 시각을 다시 확인해주세요.",
    ),

    /** 이미 고양이가 있는 회원(409). 만들어진 것으로 보고 온보딩을 마친다. */
    CAT_ALREADY_EXISTS(
        type = "api.cat.alreadyExists",
        errorMsg = "이미 함께하는 고양이가 있어요.",
    ),
}
