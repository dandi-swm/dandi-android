package com.dandi.nyummy.auth.entity

/**
 * 소셜 제공자 SDK 로그인으로 얻어 서버 검증에 넘기는 자격 증명입니다.
 *
 * @property type 소셜 제공자
 * @property token 제공자가 발급한 토큰 (OIDC 제공자는 ID 토큰, 그 외는 access token)
 * @property nonce SDK 로그인 때 넘긴 nonce. OIDC 제공자(카카오)는 필수, 그 외는 빈 값
 */
data class SocialCredentialVO(
    val type: SocialLoginType = SocialLoginType.KAKAO,
    val token: String = "",
    val nonce: String = "",
) {
    /** 토큰이 로그에 남지 않도록 제공자만 노출한다. */
    override fun toString(): String = "SocialCredentialVO(type=$type)"
}
