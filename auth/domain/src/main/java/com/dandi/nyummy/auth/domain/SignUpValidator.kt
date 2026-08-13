package com.dandi.nyummy.auth.domain

/**
 * 회원가입 입력값 검증
 */
object SignUpValidator {

    private const val PASSWORD_MIN_LENGTH = 8

    fun validateEmail(email: String): SignUpFieldError? =
        if (EmailLoginValidator.validateEmail(email) == null) null else SignUpFieldError.EMAIL_FORMAT

    /** 8자 이상 + 영문·숫자 각 1자 이상 */
    fun validatePassword(password: String): SignUpFieldError? {
        val satisfiesPolicy = password.length >= PASSWORD_MIN_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
        return if (satisfiesPolicy) null else SignUpFieldError.PASSWORD_POLICY
    }

    fun validatePasswordConfirm(password: String, passwordConfirm: String): SignUpFieldError? =
        if (password == passwordConfirm) null else SignUpFieldError.PASSWORD_CONFIRM_MISMATCH

    fun validateNickname(nickname: String): SignUpFieldError? =
        if (nickname.isNotBlank()) null else SignUpFieldError.NICKNAME_EMPTY
}
