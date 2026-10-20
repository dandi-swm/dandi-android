package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.OAuthLoginRequestDTO
import com.dandi.nyummy.auth.data.dto.ResetPasswordRequestDTO
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
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val tokenProvider: TokenProvider,
    private val appPreferenceProvider: AppPreferenceProvider,
    private val pushTokenHelper: PushTokenHelper,
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

    override suspend fun resetPassword(verifiedToken: String) {
        dataSource.resetPassword(ResetPasswordRequestDTO(verifiedToken = verifiedToken))
    }

    override suspend fun logout() {
        // 서버가 실패해도 이 기기의 토큰은 꼭 지운다. 지우지 않으면 로그아웃했는데도 로그인된 채로 남는다.
        try {
            // 서버는 액세스 토큰으로 사용자를 찾는다. 토큰이 없으면 지울 세션을 알 수 없으니 부르지 않는다.
            // 토큰이 만료돼 401이 나면 재발급하지 않고 실패로 둔다(이 서비스는 인증 클라이언트가 아니다).
            tokenProvider.accessToken?.takeIf { it.isNotBlank() }?.let { dataSource.logout(it) }
        } finally {
            // 요청이 취소돼도(화면을 나가는 등) 지운다. 취소된 코루틴에서는 suspend 저장도 바로 취소되기 때문이다.
            withContext(NonCancellable) { tokenProvider.clear() }
        }
    }

    /**
     * 발급 토큰 영속화 — 이후 요청부터 인증 헤더/Authenticator 가 사용한다.
     * 로그인과 가입이 모두 여기를 지나므로, 이 기기의 푸시 토큰도 여기서 서버에 등록한다(재발급은 지나지 않는다).
     */
    private suspend fun saveToken(token: AuthTokenVO) {
        if (token.accessToken.isBlank() || token.refreshToken.isBlank()) return
        tokenProvider.update(access = token.accessToken, refresh = token.refreshToken)
        pushTokenHelper.register()
    }
}
