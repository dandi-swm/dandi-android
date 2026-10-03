package com.dandi.nyummy.auth.entity

/**
 * 소셜 로그인 서버 검증 결과입니다.
 *
 * 기존 회원이면 [token] 이, 신규 회원이면 회원가입에 쓸 [verifiedToken] 이 채워집니다.
 *
 * @property token 기존 회원에게 발급된 인증 토큰
 * @property verifiedToken 신규 회원이 회원가입 API 에 넘기는 검증 완료 토큰
 */
data class SocialLoginVO(
    val token: AuthTokenVO = AuthTokenVO.empty,
    val verifiedToken: String = "",
) {
    /** 기존 회원이라 바로 로그인됐는지. 토큰이 있으면 [verifiedToken] 보다 우선한다. */
    val isLoggedIn: Boolean
        get() = token.accessToken.isNotBlank() && token.refreshToken.isNotBlank()

    /** 신규 회원이라 프로필을 입력해 가입해야 하는지. */
    val isSignUpRequired: Boolean
        get() = !isLoggedIn && verifiedToken.isNotBlank()

    /** 토큰이 로그에 남지 않도록 결과 종류만 노출한다. */
    override fun toString(): String =
        "SocialLoginVO(isLoggedIn=$isLoggedIn, isSignUpRequired=$isSignUpRequired)"

    companion object {
        val empty = SocialLoginVO()
    }
}
