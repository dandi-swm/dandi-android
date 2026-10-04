package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 고양이 등록 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 * 서버 명세 확정 전 임시 code 이므로 확정되면 맞춘다.
 */
enum class OnboardingErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    CAT_NAME_INVALID(
        type = "api.cat.invalidName",
        errorMsg = "이름은 1~10자로 지어주세요.",
    ),

    /** 이미 고양이가 있는 회원. 온보딩을 마친 것으로 보고 홈으로 보낸다. */
    CAT_ALREADY_EXISTS(
        type = "api.cat.alreadyExists",
        errorMsg = "이미 함께하는 고양이가 있어요.",
    ),
}
