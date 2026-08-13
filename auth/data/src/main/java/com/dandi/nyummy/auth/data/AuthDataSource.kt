package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.SignUpRequestDTO
import com.dandi.nyummy.common.data.BaseRemoteDataSource
import javax.inject.Inject

class AuthDataSource(
    private val apiService: AuthApiService,
) : BaseRemoteDataSource() {

    suspend fun login(request: LoginRequestDTO): AuthTokenDTO =
        checkResponse(apiService.login(request))

    suspend fun signUp(request: SignUpRequestDTO): AuthTokenDTO =
        checkResponse(apiService.signUp(request))

    suspend fun requestEmailVerification(request: EmailVerificationRequestDTO) {
        checkResponse(apiService.requestEmailVerification(request))
    }

    suspend fun confirmEmailVerification(request: EmailVerificationConfirmRequestDTO) {
        checkResponse(apiService.confirmEmailVerification(request))
    }
}