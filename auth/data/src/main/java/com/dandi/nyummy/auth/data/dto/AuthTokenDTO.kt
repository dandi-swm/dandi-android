package com.dandi.nyummy.auth.data.dto

import com.dandi.nyummy.auth.entity.AuthTokenVO
import kotlinx.serialization.Serializable

/** 로그인/회원가입 200 응답 바디. redirectUrl 은 로그인 응답에만 존재한다. */
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
