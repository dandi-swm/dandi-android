package com.dandi.nyummy.auth.domain

/**
 * 회원가입 폼 필드 검증 오류
 */
enum class SignUpFieldError {
    EMAIL_FORMAT,
    PASSWORD_POLICY,
    PASSWORD_CONFIRM_MISMATCH,
    NICKNAME_EMPTY,

    /** 집사 이름이 [SignUpValidator.NICKNAME_MAX_LENGTH]자를 넘는다. */
    NICKNAME_TOO_LONG,
}
