package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.common.data.BaseRemoteDataSource
import javax.inject.Inject

class AuthDataSource(
    private val apiService: AuthApiService,
) : BaseRemoteDataSource() {

    suspend fun login(request: LoginRequestDTO): AuthTokenDTO =
        checkResponse(apiService.login(request))
}