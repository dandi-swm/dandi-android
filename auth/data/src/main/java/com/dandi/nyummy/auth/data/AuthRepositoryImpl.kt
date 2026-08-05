package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.PasswordChangeRequestDTO
import com.dandi.nyummy.auth.data.dto.SignupRequestDTO
import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SignupFormVO

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthTokenVO =
        dataSource.login(LoginRequestDTO(email = email, password = password)).toVO()

    override suspend fun signup(form: SignupFormVO): AuthTokenVO =
        dataSource.signup(
            SignupRequestDTO(
                email = form.email,
                password = form.password,
                nickname = form.nickname,
                gender = form.gender,
                birth = form.birth,
                height = form.height,
                weight = form.weight,
            ),
        ).toVO()

    override suspend fun requestEmailVerification(email: String) {
        dataSource.requestEmailVerification(EmailVerificationRequestDTO(email = email))
    }

    override suspend fun confirmEmailVerification(email: String, verificationCode: Int) {
        dataSource.confirmEmailVerification(
            EmailVerificationConfirmRequestDTO(email = email, verificationCode = verificationCode),
        )
    }

    override suspend fun changePassword(userId: Long, password: String, newPassword: String) {
        dataSource.changePassword(
            userId = userId,
            request = PasswordChangeRequestDTO(password = password, newPassword = newPassword),
        )
    }
}
