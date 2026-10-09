package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 인증 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다
 * (예: `{"code": "api.auth.emailCodeMismatch", "message": "..."}`).
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 * 서버의 에러 코드(AuthErrorCode, EmailErrorCode)가 바뀌면 여기도 같이 고친다.
 *
 * `api.auth.unauthorized`는 로그인 세션 실패 전용이라 여기 두지 않는다. 공통 처리(로그인 만료)가 맡는다.
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
    EMAIL_SEND_RATE_LIMITED(
        type = "api.auth.emailSendRateLimited",
        errorMsg = "인증 코드를 보낸 지 얼마 되지 않았어요. 잠시 후 다시 보내 주세요.",
    ),
    EMAIL_SEND_FAILED(
        type = "api.email.sendFailed",
        errorMsg = EMAIL_SEND_FAILED_MESSAGE,
    ),

    /** 서버가 메일 발송 수단을 바꾸기 전(SES)의 코드. 배포가 끝나면 지운다. */
    SES_EMAIL_SEND_FAILED(
        type = "api.ses.emailSendFailed",
        errorMsg = EMAIL_SEND_FAILED_MESSAGE,
    ),
    INCORRECT_EMAIL(
        type = "api.auth.incorrectEmail",
        errorMsg = "이 이메일로 보낸 인증 코드가 없어요. 코드를 다시 받아 주세요.",
    ),
    EMAIL_CODE_ATTEMPT_EXCEEDED(
        type = "api.auth.emailCodeAttemptExceeded",
        errorMsg = "인증 코드를 여러 번 틀렸어요. 코드를 다시 받아 주세요.",
    ),
    EMAIL_CODE_MISMATCH(
        type = "api.auth.emailCodeMismatch",
        errorMsg = "인증 코드가 일치하지 않습니다.",
    ),
    EMAIL_CODE_EXPIRED(
        type = "api.auth.emailCodeExpired",
        errorMsg = "인증 코드 유효 시간이 지났습니다. 코드를 재발송 받으세요.",
    ),
    VERIFICATION_EXPIRED(
        type = "api.auth.verificationExpired",
        errorMsg = VERIFICATION_RESTART_MESSAGE,
    ),

    /** 인증 코드 확인 때 받은 challenge 토큰이 무효하다(HTTP 401). 코드를 다시 받아야 한다. */
    INVALID_EMAIL_CHALLENGE_TOKEN(
        type = "api.auth.invalidEmailChallengeToken",
        errorMsg = "인증 요청이 올바르지 않아요. 인증 코드를 다시 받아 주세요.",
    ),

    /** 가입·비밀번호 찾기에 쓰는 인증 완료 토큰이 무효하다(HTTP 401). 이메일 인증을 처음부터 다시 해야 한다. */
    INVALID_VERIFIED_TOKEN(
        type = "api.auth.invalidVerifiedToken",
        errorMsg = VERIFICATION_RESTART_MESSAGE,
    ),
    EMAIL_ALREADY_EXISTS(
        type = "api.auth.emailAlreadyExists",
        errorMsg = "이미 가입된 이메일이에요. 로그인해 주세요.",
    ),
    EMAIL_NOT_FOUND(
        type = "api.auth.emailNotFound",
        errorMsg = "가입되지 않은 이메일이에요.",
    ),
    OAUTH_ACCOUNT_ALREADY_EXISTS(
        type = "api.auth.oauthAccountAlreadyExists",
        errorMsg = "이미 가입된 계정이에요. 다시 로그인해주세요.",
    ),
    INVALID_OAUTH_TOKEN(
        type = "api.auth.invalidOAuthToken",
        errorMsg = "소셜 로그인에 실패했어요. 다시 시도해주세요.",
    ),
    OAUTH_PROVIDER_UNAVAILABLE(
        type = "api.auth.oauthProviderUnavailable",
        errorMsg = "소셜 로그인 서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.",
    ),
    UNKNOWN(
        type = "api.auth.unknown",
        errorMsg = "알 수 없는 오류가 발생했습니다.",
    ),
}

private const val EMAIL_SEND_FAILED_MESSAGE = "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요."
private const val VERIFICATION_RESTART_MESSAGE = "인증이 만료됐어요. 처음부터 다시 인증해 주세요."
