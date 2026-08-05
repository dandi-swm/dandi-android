package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 인증 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다
 * (예: `{"code": "api.common.missingParameter", "message": "..."}`).
 * 명세의 엔드포인트별 에러 code 표가 확정되면 항목을 추가한다.
 */
enum class AuthErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    MISSING_PARAMETER(
        type = "api.common.missingParameter",
        errorMsg = "A required value is missing.",
    ),
    UNKNOWN(
        type = "api.auth.unknown",
        errorMsg = "A temporary error occurred.",
    ),
}
