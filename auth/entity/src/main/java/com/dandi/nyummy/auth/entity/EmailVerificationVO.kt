package com.dandi.nyummy.auth.entity

/**
 * 이메일 인증 코드 발송 결과. 코드 확인 시 함께 보내는 챌린지 토큰을 담는다.
 */
data class EmailChallengeVO(
    val emailChallengeToken: String = "",
)

/**
 * 이메일 인증 코드 확인 결과. 회원가입 요청에 사용하는 인증 완료 토큰을 담는다.
 */
data class EmailVerifiedVO(
    val emailVerifiedToken: String = "",
)
