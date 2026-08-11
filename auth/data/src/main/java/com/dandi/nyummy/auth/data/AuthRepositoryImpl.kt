package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import javax.inject.Inject

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
) : AuthRepository {
    override suspend fun socialLogin(socialLoginType: SocialLoginType): AuthTokenVO {
        TODO("Not yet implemented")
    }

    override suspend fun login(email: String, password: String) {
        dataSource.login(LoginRequestDTO(email = email, password = password)).also {
            saveToken(it.accessToken, it.refreshToken)
        }
    }


    private fun saveToken(accessToken: String?, refreshToken: String?) {
        // TODO: 토큰 저장 로직 구현 DataStore

    }
}