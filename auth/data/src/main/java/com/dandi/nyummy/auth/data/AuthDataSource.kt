package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.PasswordChangeRequestDTO
import com.dandi.nyummy.auth.data.dto.SignupRequestDTO
import com.dandi.nyummy.common.data.BaseRemoteDataSource

class AuthDataSource(
    private val apiService: AuthApiService,
) : BaseRemoteDataSource() {

    suspend fun login(request: LoginRequestDTO): AuthTokenDTO =
        checkResponse(apiService.login(request))

    suspend fun signup(request: SignupRequestDTO): AuthTokenDTO =
        checkResponse(apiService.signup(request))

    suspend fun requestEmailVerification(request: EmailVerificationRequestDTO) {
        checkResponse(apiService.requestEmailVerification(request))
    }

    suspend fun confirmEmailVerification(request: EmailVerificationConfirmRequestDTO) {
        checkResponse(apiService.confirmEmailVerification(request))
    }

    suspend fun changePassword(userId: Long, request: PasswordChangeRequestDTO) {
        checkResponse(apiService.changePassword(userId, request))
    }
}
