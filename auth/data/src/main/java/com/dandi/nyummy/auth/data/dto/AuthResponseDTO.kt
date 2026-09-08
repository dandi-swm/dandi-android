package com.dandi.nyummy.auth.data.dto

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
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
 * @property emailVerifiedToken 회원가입 요청에 사용하는 인증 완료 토큰
 */
@Serializable
data class EmailVerifiedDTO(
    val emailVerifiedToken: String? = null,
) {
    fun toVO(): EmailVerifiedVO = EmailVerifiedVO(
        emailVerifiedToken = emailVerifiedToken.orEmpty(),
    )
}
