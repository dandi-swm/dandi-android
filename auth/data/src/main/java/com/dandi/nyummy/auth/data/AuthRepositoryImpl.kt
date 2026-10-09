package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.OAuthLoginRequestDTO
import com.dandi.nyummy.auth.data.dto.SignUpRequestDTO
import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.data.token.TokenProvider

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val tokenProvider: TokenProvider,
    private val appPreferenceProvider: AppPreferenceProvider,
) : AuthRepository {
    override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
        appPreferenceProvider.setOnboardingIncomplete(incomplete)
    }

    override suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO =
        dataSource.oauthLogin(
            OAuthLoginRequestDTO(
                provider = credential.type.name,
                token = credential.token,
                nonce = credential.nonce.ifBlank { null },
            ),
        )
            .toVO()
            // 신규 회원은 토큰 없이 verifiedToken 만 오므로 저장되지 않는다(saveToken 이 빈 토큰을 건너뜀).
            .also { saveToken(it.token) }

    override suspend fun login(email: String, password: String): AuthTokenVO =
        dataSource.login(LoginRequestDTO(email = email, password = password))
            .toVO()
            .also { saveToken(it) }

    override suspend fun signUp(
        verifiedToken: String,
        password: String?,
        confirmPassword: String?,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    ) {
        dataSource.signUp(
            SignUpRequestDTO(
                verifiedToken = verifiedToken,
                password = password,
                confirmPassword = confirmPassword,
                nickname = nickname,
                gender = gender?.name,
                birth = birth,
                height = height,
                weight = weight,
            ),
        )
            .toVO()
            .also { saveToken(it) }
    }

    override suspend fun requestEmailVerification(
        email: String,
        purpose: EmailVerificationPurpose,
    ): EmailChallengeVO =
        dataSource.requestEmailVerification(
            EmailVerificationRequestDTO(email = email, purpose = purpose.name),
        ).toVO()

    override suspend fun confirmEmailVerification(
        authCode: String,
        emailChallengeToken: String,
    ): EmailVerifiedVO =
        dataSource.confirmEmailVerification(
            EmailVerificationConfirmRequestDTO(
                authCode = authCode,
                emailChallengeToken = emailChallengeToken,
            ),
        ).toVO()

    override suspend fun logout() {
        // 서버가 실패해도 이 기기의 토큰은 꼭 지운다. 지우지 않으면 로그아웃했는데도 로그인된 채로 남는다.
        try {
            dataSource.logout()
        } finally {
            tokenProvider.clear()
        }
    }

    /** 발급 토큰 영속화 — 이후 요청부터 인증 헤더/Authenticator 가 사용한다. */
    private suspend fun saveToken(token: AuthTokenVO) {
        if (token.accessToken.isBlank() || token.refreshToken.isBlank()) return
        tokenProvider.update(access = token.accessToken, refresh = token.refreshToken)
    }
}
