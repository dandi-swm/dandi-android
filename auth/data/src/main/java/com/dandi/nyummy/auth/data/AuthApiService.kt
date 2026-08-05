package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.PasswordChangeRequestDTO
import com.dandi.nyummy.auth.data.dto.SignupRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApiService {

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDTO): Response<AuthTokenDTO>

    @POST("/api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequestDTO): Response<AuthTokenDTO>

    // TODO: 명세 표(email-verifications)와 본문(email-verification) 표기가 달라 복수형 기준 — 백엔드 확정 시 재확인
    @POST("/api/v1/auth/email-verifications")
    suspend fun requestEmailVerification(@Body request: EmailVerificationRequestDTO): Response<Unit>

    @POST("/api/v1/auth/email-verifications/confirm")
    suspend fun confirmEmailVerification(@Body request: EmailVerificationConfirmRequestDTO): Response<Unit>

    @PATCH("/api/v1/users/me/password")
    suspend fun changePassword(
        @Header("X-User-Id") userId: Long,
        @Body request: PasswordChangeRequestDTO,
    ): Response<Unit>
}
