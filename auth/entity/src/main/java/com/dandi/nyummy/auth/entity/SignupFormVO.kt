package com.dandi.nyummy.auth.entity

/**
 * 회원가입 요청 입력값.
 *
 * @property gender 표시 문자열 그대로 전송 (예: "남성")
 * @property birth ISO-8601 날짜 문자열 (예: "2026-07-12")
 */
data class SignupFormVO(
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val gender: String = "",
    val birth: String = "",
    val height: Int = 0,
    val weight: Int = 0,
) {
    companion object {
        val empty: SignupFormVO = SignupFormVO()
    }
}
