package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailChallengeDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerifiedDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.OAuthLoginDTO
import com.dandi.nyummy.auth.data.dto.OAuthLoginRequestDTO
import com.dandi.nyummy.auth.data.dto.RefreshTokenRequestDTO
import com.dandi.nyummy.auth.data.dto.SignUpRequestDTO
import com.dandi.nyummy.common.data.token.TokenAuthenticator
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 회원가입 · 로그인 · 로그아웃 · 이메일 인증 API
 */
interface AuthApiService {
    /** 로그인 */
    @POST("${AUTH_PATH}/login")
    suspend fun login(@Body loginRequest: LoginRequestDTO): Response<AuthTokenDTO>

    /** 소셜 로그인 */
    @POST("${AUTH_PATH}/oauth/login")
    suspend fun oauthLogin(@Body request: OAuthLoginRequestDTO): Response<OAuthLoginDTO>

    /** 회원가입 */
    @POST("${AUTH_PATH}/signup")
    suspend fun signUp(@Body signUpRequest: SignUpRequestDTO): Response<AuthTokenDTO>

    /** 이메일 인증 코드 발송 */
    @POST("${AUTH_PATH}/email-verification")
    suspend fun requestEmailVerification(@Body request: EmailVerificationRequestDTO): Response<EmailChallengeDTO>

    /** 이메일 인증 코드 확인 */
    @POST("${AUTH_PATH}/email-verification/confirm")
    suspend fun confirmEmailVerification(@Body request: EmailVerificationConfirmRequestDTO): Response<EmailVerifiedDTO>

    /** 토큰 재발급 */
    @POST("${AUTH_PATH}/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDTO): Response<AuthTokenDTO>

    /**
     * 로그아웃. 서버는 액세스 토큰으로 사용자를 찾아 RefreshToken과 디바이스 토큰을 지운다(204, 이미 지웠어도 성공).
     * 이 서비스는 인증 헤더가 붙지 않는 클라이언트라 [authorization]에 `Bearer {액세스 토큰}`을 직접 넘긴다.
     */
    @POST("${AUTH_PATH}/logout")
    suspend fun logout(
        @Header(TokenAuthenticator.HEADER_AUTHORIZATION) authorization: String,
    ): Response<Unit>

    companion object {
        const val AUTH_PATH = "/api/v1/auth"
    }
}
