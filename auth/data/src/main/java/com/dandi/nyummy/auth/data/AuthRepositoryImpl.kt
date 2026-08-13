package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.data.token.TokenProvider

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val tokenProvider: TokenProvider,
) : AuthRepository {
    override suspend fun socialLogin(socialLoginType: SocialLoginType): AuthTokenVO {
        TODO("Not yet implemented")
    }

    override suspend fun login(email: String, password: String) {
        dataSource.login(LoginRequestDTO(email = email, password = password))
            .toVO()
            .also { saveToken(it) }
    }

    override suspend fun requestEmailVerification(email: String) {
        dataSource.requestEmailVerification(EmailVerificationRequestDTO(email = email))
    }

    override suspend fun confirmEmailVerification(email: String, verificationCode: String) {
        dataSource.confirmEmailVerification(
            EmailVerificationConfirmRequestDTO(email = email, verificationCode = verificationCode),
        )
    }

    /** 발급 토큰 영속화 — 이후 요청부터 인증 헤더/Authenticator 가 사용한다. */
    private suspend fun saveToken(token: AuthTokenVO) {
        if (token.accessToken.isBlank() || token.refreshToken.isBlank()) return
        tokenProvider.update(access = token.accessToken, refresh = token.refreshToken)
    }
}
