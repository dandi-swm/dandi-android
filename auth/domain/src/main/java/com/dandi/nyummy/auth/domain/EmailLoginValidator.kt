package com.dandi.nyummy.auth.domain

/**
 * 이메일 로그인 입력값 검증
 */
object EmailLoginValidator {

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(email: String): EmailLoginFieldError? =
        if (EMAIL_REGEX.matches(email)) null else EmailLoginFieldError.EMAIL_FORMAT
}
