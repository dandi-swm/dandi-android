package com.dandi.nyummy.auth.entity
/**
 * 로그인/회원가입 성공 시 발급되는 인증 토큰.
 *
 * @property redirectUrl 로그인 응답에만 존재 (예: "dandi://"). 회원가입 응답에는 없음.
 */
data class AuthTokenVO(
    val accessToken: String = "",
    val refreshToken: String = "",
    val redirectUrl: String = "",
) {
    companion object {
        val empty: AuthTokenVO = AuthTokenVO()
    }
}
