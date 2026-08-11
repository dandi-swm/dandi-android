package com.dandi.nyummy.auth.data.dto

import com.dandi.nyummy.auth.entity.AuthTokenVO
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
