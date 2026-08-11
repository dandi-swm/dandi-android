package com.dandi.nyummy.auth.data.dto

import kotlinx.serialization.Serializable

/**
 * 로그인 요청 바디입니다.
 */
@Serializable
data class LoginRequestDTO(
    val email: String,
    val password: String,
)

/**
 * 회원가입 요청 바디입니다.
 *
 * @property gender 성별 문자열
 * @property birth 생년월일 (`yyyy-MM-dd` 형식)
 * @property height 키 (cm)
 * @property weight 몸무게 (kg)
 */
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

/**
 * 이메일 인증 코드 발송 요청 바디입니다.
 */
@Serializable
data class EmailVerificationRequestDTO(
    val email: String,
)

/**
 * 이메일 인증 코드 확인 요청 바디입니다.
 *
 * @property verificationCode 이메일로 받은 인증 코드
 */
@Serializable
data class EmailVerificationConfirmRequestDTO(
    val email: String,
    val verificationCode: String,
)
