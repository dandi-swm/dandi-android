package com.dandi.nyummy.common.domain.error

/**
 * 로그인 세션(AccessToken) 인증 실패 code. 서버는 이 code만 세션 문제로 내려 주고,
 * 회원가입·비밀번호 찾기의 인증 토큰 오류는 토큰별 code(`api.auth.invalidVerifiedToken` 등)로 따로 준다.
 */
const val SESSION_UNAUTHORIZED_CODE = "api.auth.unauthorized"

/** 서버 공통 에러 바디의 `code`. data 레이어가 [HttpResponseException.cause]의 message에 담아 둔다. 없으면 null. */
val HttpResponseException.serverErrorCode: String?
    get() = cause?.message?.takeIf { it.isNotBlank() }

/** [ErrorType]에 등록된 code면 그 항목을 돌려준다. [HttpErrorType.isHandledOnDomain]과 상관없다. */
inline fun <reified ErrorType> HttpResponseException.registeredErrorType(): ErrorType?
        where ErrorType : Enum<ErrorType>,
              ErrorType : HttpErrorType {
    val code = serverErrorCode ?: return null
    return enumValues<ErrorType>().firstOrNull { it.type == code }
}

/** [ErrorType]에 등록돼 있고 domain에서 처리하는 code면 그 항목을 돌려준다. */
inline fun <reified ErrorType> HttpResponseException.handlingErrorOnUseCase(): ErrorType?
        where ErrorType : Enum<ErrorType>,
              ErrorType : HttpErrorType {
    return registeredErrorType<ErrorType>()?.takeIf { it.isHandledOnDomain }
}

/**
 * 로그인 세션이 끝난 401인지. code가 없거나 [SESSION_UNAUTHORIZED_CODE]일 때만 그렇다.
 * 다른 code가 붙은 401(인증 코드, 소셜 토큰 오류 등)은 로그인 만료가 아니라 그 화면의 오류다.
 */
fun HttpResponseException.isSessionExpired(): Boolean {
    if (rawCode != 401) return false
    val code = serverErrorCode
    return code == null || code == SESSION_UNAUTHORIZED_CODE
}

/** 여러 화면에 공통으로 안내하는 오류인지. 로그인 만료 401, 404, 5xx. */
fun HttpResponseException.isCommonErrorHandling(): Boolean {
    return isSessionExpired() || this.rawCode == 404 || this.rawCode >= 500
}
