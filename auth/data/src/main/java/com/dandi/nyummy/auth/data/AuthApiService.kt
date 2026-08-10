package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.SignupRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 회원가입 · 로그인 · 이메일 인증 API
 */
interface AuthApiService {
    /** 로그인 */
    @POST("${AUTH_PATH}/login")
    suspend fun login(@Body loginRequest: LoginRequestDTO): Response<AuthTokenDTO>

    /** 회원가입 */
    @POST("${AUTH_PATH}/signup")
    suspend fun signup(@Body signupRequest: SignupRequestDTO): Response<AuthTokenDTO>

    /** 이메일 인증 코드 발송 */
    @POST("${AUTH_PATH}/email-verification")
    suspend fun requestEmailVerification(@Body request: EmailVerificationRequestDTO): Response<Unit>

    /** 이메일 인증 코드 확인 */
    @POST("${AUTH_PATH}/email-verification/confirm")
    suspend fun confirmEmailVerification(@Body request: EmailVerificationConfirmRequestDTO): Response<Unit>

    companion object {
        const val AUTH_PATH = "/api/v1/auth"
    }
}
