package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType
import com.dandi.nyummy.common.domain.helper.StringResource

/**
 * 인증 API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다
 * (예: `{"code": "api.auth.emailCodeMismatch", "message": "..."}`).
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 * 서버의 에러 코드(AuthErrorCode, EmailErrorCode)가 바뀌면 여기도 같이 고친다.
 *
 * `api.auth.unauthorized`, `api.auth.tokenExpired`는 로그인 세션 실패 전용이라 여기 두지 않는다. 공통 처리(로그인 만료)가 맡는다.
 */
enum class AuthErrorType(
    override val type: String,
    override val errorMsg: StringResource,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    INVALID_CREDENTIALS(
        type = "api.auth.invalidCredentials",
        errorMsg = StringResource.AUTH_INVALID_CREDENTIALS,
    ),
    EMAIL_SEND_RATE_LIMITED(
        type = "api.auth.emailSendRateLimited",
        errorMsg = StringResource.AUTH_EMAIL_SEND_RATE_LIMITED,
    ),
    EMAIL_SEND_FAILED(
        type = "api.email.sendFailed",
        errorMsg = StringResource.AUTH_EMAIL_SEND_FAILED,
    ),

    INCORRECT_EMAIL(
        type = "api.auth.incorrectEmail",
        errorMsg = StringResource.AUTH_INCORRECT_EMAIL,
    ),
    EMAIL_CODE_ATTEMPT_EXCEEDED(
        type = "api.auth.emailCodeAttemptExceeded",
        errorMsg = StringResource.AUTH_EMAIL_CODE_ATTEMPT_EXCEEDED,
    ),
    EMAIL_CODE_MISMATCH(
        type = "api.auth.emailCodeMismatch",
        errorMsg = StringResource.AUTH_EMAIL_CODE_MISMATCH,
    ),
    EMAIL_CODE_EXPIRED(
        type = "api.auth.emailCodeExpired",
        errorMsg = StringResource.AUTH_EMAIL_CODE_EXPIRED,
    ),
    VERIFICATION_EXPIRED(
        type = "api.auth.verificationExpired",
        errorMsg = StringResource.AUTH_VERIFICATION_RESTART,
    ),

    /** 인증 코드 확인 때 받은 challenge 토큰이 무효하다(HTTP 401). 코드를 다시 받아야 한다. */
    INVALID_EMAIL_CHALLENGE_TOKEN(
        type = "api.auth.invalidEmailChallengeToken",
        errorMsg = StringResource.AUTH_INVALID_EMAIL_CHALLENGE,
    ),

    /** 가입·비밀번호 찾기에 쓰는 인증 완료 토큰이 무효하다(HTTP 401). 이메일 인증을 처음부터 다시 해야 한다. */
    INVALID_VERIFIED_TOKEN(
        type = "api.auth.invalidVerifiedToken",
        errorMsg = StringResource.AUTH_VERIFICATION_RESTART,
    ),
    EMAIL_ALREADY_EXISTS(
        type = "api.auth.emailAlreadyExists",
        errorMsg = StringResource.AUTH_EMAIL_ALREADY_EXISTS,
    ),
    EMAIL_NOT_FOUND(
        type = "api.auth.emailNotFound",
        errorMsg = StringResource.AUTH_EMAIL_NOT_REGISTERED,
    ),
    OAUTH_ACCOUNT_ALREADY_EXISTS(
        type = "api.auth.oauthAccountAlreadyExists",
        errorMsg = StringResource.AUTH_ACCOUNT_ALREADY_EXISTS,
    ),
    INVALID_OAUTH_TOKEN(
        type = "api.auth.invalidOAuthToken",
        errorMsg = StringResource.AUTH_SOCIAL_LOGIN_FAILED,
    ),
    OAUTH_PROVIDER_UNAVAILABLE(
        type = "api.auth.oauthProviderUnavailable",
        errorMsg = StringResource.AUTH_SOCIAL_SERVER_UNAVAILABLE,
    ),
}

