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
 * @property emailVerifiedToken 이메일 인증 완료 토큰
 * @property confirmPassword 비밀번호 확인 값
 * @property gender 성별 문자열 (선택)
 * @property birth 생년월일 (`yyyy-MM-dd` 형식, 선택)
 * @property height 키 (cm, 선택)
 * @property weight 몸무게 (kg, 선택)
 */
@Serializable
data class SignUpRequestDTO(
    val emailVerifiedToken: String,
    val password: String,
    val confirmPassword: String,
    val nickname: String,
    val gender: String? = null,
    val birth: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
)

/**
 * 토큰 재발급 요청 바디입니다.
 */
@Serializable
data class RefreshTokenRequestDTO(
    val refreshToken: String,
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
 * @property authCode 이메일로 받은 6자리 인증 코드
 * @property emailChallengeToken 코드 발송 시 발급받은 챌린지 토큰
 */
@Serializable
data class EmailVerificationConfirmRequestDTO(
    val authCode: String,
    val emailChallengeToken: String,
)
