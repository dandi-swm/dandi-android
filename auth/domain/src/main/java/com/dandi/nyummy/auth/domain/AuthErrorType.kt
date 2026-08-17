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
        errorMsg = "입력값이 누락 되었습니다. 다시 시도해주세요.",
    ),
    INVALID_CREDENTIALS(
        type = "api.auth.invalidCredentials",
        errorMsg = "올바른 이메일과 비밀번호를 입력해주세요.",
    ),
    MAIL_RESEND_TOO_EARLY(
        type = "api.auth.mailResendTooEarly",
        errorMsg = "인증 코드를 발송한 지 5분이 지나지 않았습니다.",
    ),
    MAIL_NOT_FOUND(
        type = "api.auth.mailNotFound",
        errorMsg = "해당 이메일로 발송된 인증 코드가 없습니다.",
    ),
    MAIL_CODE_EXPIRED(
        type = "api.auth.mailCodeExpired",
        errorMsg = "인증 코드 유효 시간이 지났습니다. 코드를 재발송 받으세요.",
    ),
    MAIL_CODE_MISMATCH(
        type = "api.auth.mailCodeMismatch",
        errorMsg = "인증 코드가 일치하지 않습니다.",
    ),
    UNKNOWN(
        type = "api.auth.unknown",
        errorMsg = "알 수 없는 오류가 발생했습니다.",
    ),
}
