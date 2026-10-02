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
 * @property verifiedToken 이메일 인증 확인 또는 소셜 로그인 응답의 검증 완료 토큰
 * @property password 비밀번호 (이메일 가입 전용, 소셜 가입은 생략)
 * @property confirmPassword 비밀번호 확인 값 (이메일 가입 전용, 소셜 가입은 생략)
 * @property gender 성별 문자열 (선택)
 * @property birth 생년월일 (`yyyy-MM-dd` 형식, 선택)
 * @property height 키 (cm, 선택)
 * @property weight 몸무게 (kg, 선택)
 */
@Serializable
data class SignUpRequestDTO(
    val verifiedToken: String,
    val nickname: String,
    val password: String? = null,
    val confirmPassword: String? = null,
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
 *
 * @property purpose 발송 목적 (`SIGNUP` \| `RESET_PASSWORD`)
 */
@Serializable
data class EmailVerificationRequestDTO(
    val email: String,
    val purpose: String,
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
