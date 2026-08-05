package com.dandi.nyummy.auth.data.dto

import kotlinx.serialization.Serializable

/** POST /api/v1/auth/login 요청 바디. */
@Serializable
data class LoginRequestDTO(
    val email: String,
    val password: String,
)

/** POST /api/v1/auth/signup 요청 바디. */
@Serializable
data class SignupRequestDTO(
    val email: String,
    val password: String,
    val nickname: String,
    val gender: String,
    val birth: String,
    val height: Int,
    val weight: Int,
)

/** POST /api/v1/auth/email-verifications 요청 바디. */
@Serializable
data class EmailVerificationRequestDTO(
    val email: String,
)

/** POST /api/v1/auth/email-verifications/confirm 요청 바디. */
@Serializable
data class EmailVerificationConfirmRequestDTO(
    val email: String,
    val verificationCode: Int,
)

/** PATCH /api/v1/users/me/password 요청 바디. */
@Serializable
data class PasswordChangeRequestDTO(
    val password: String,
    val newPassword: String,
)
