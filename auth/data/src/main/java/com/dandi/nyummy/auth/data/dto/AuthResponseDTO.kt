package com.dandi.nyummy.auth.data.dto

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.SocialLoginVO
import kotlinx.serialization.Serializable

/**
 * 로그인/회원가입 성공 시 발급되는 인증 토큰 응답입니다.
 *
 * @property accessToken 만료 30분의 접근 토큰
 * @property refreshToken 만료 15일의 갱신 토큰
 * @property redirectUrl 로그인 응답에만 포함되는 이동 대상 URL
 */
@Serializable
data class AuthTokenDTO(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val redirectUrl: String? = null,
) {
    fun toVO(): AuthTokenVO = AuthTokenVO(
        accessToken = accessToken.orEmpty(),
        refreshToken = refreshToken.orEmpty(),
        redirectUrl = redirectUrl.orEmpty(),
    )
}

/**
 * 소셜 로그인 응답입니다. 기존 회원은 인증 토큰을, 신규 회원은 회원가입에 쓸 검증 완료 토큰을 받습니다.
 *
 * @property redirectUrl 이동 대상 URL (기존 회원은 홈, 신규 회원은 프로필 입력 화면)
 * @property accessToken 기존 회원에게 발급되는 접근 토큰
 * @property refreshToken 기존 회원에게 발급되는 갱신 토큰
 * @property verifiedToken 신규 회원이 회원가입 API 에 넘기는 검증 완료 토큰
 */
@Serializable
data class OAuthLoginDTO(
    val redirectUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val verifiedToken: String? = null,
) {
    fun toVO(): SocialLoginVO = SocialLoginVO(
        token = AuthTokenVO(
            accessToken = accessToken.orEmpty(),
            refreshToken = refreshToken.orEmpty(),
            redirectUrl = redirectUrl.orEmpty(),
        ),
        verifiedToken = verifiedToken.orEmpty(),
    )
}

/**
 * 이메일 인증 코드 발송 응답입니다.
 *
 * @property emailChallengeToken 코드 확인 요청에 함께 보내는 챌린지 토큰
 */
@Serializable
data class EmailChallengeDTO(
    val emailChallengeToken: String? = null,
) {
    fun toVO(): EmailChallengeVO = EmailChallengeVO(
        emailChallengeToken = emailChallengeToken.orEmpty(),
    )
}

/**
 * 이메일 인증 코드 확인 응답입니다.
 *
 * @property verifiedToken 회원가입 요청에 사용하는 인증 완료 토큰
 */
@Serializable
data class EmailVerifiedDTO(
    val verifiedToken: String? = null,
) {
    fun toVO(): EmailVerifiedVO = EmailVerifiedVO(
        emailVerifiedToken = verifiedToken.orEmpty(),
    )
}
