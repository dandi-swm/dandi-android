package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginVO

interface AuthRepository {

    /**
     * 소셜 제공자 토큰을 서버에서 검증한다. 기존 회원이면 발급 토큰을 저장한다.
     * 신규 회원이면 토큰 없이 회원가입에 쓸 [SocialLoginVO.verifiedToken] 만 돌려준다.
     */
    suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO

    /** 이메일 로그인  */
    suspend fun login(email: String, password: String)

    /**
     * 회원가입. 성공 시 발급 토큰을 저장한다.
     *
     * @param verifiedToken 이메일 인증 확인 또는 소셜 로그인 응답의 검증 완료 토큰
     * @param password 이메일 가입 전용. 소셜 가입은 null
     * @param confirmPassword 이메일 가입 전용. 소셜 가입은 null
     * @param birth 생년월일 (`yyyy-MM-dd` 형식, 선택)
     * @param height 키 (cm, 선택)
     * @param weight 몸무게 (kg, 선택)
     */
    suspend fun signUp(
        verifiedToken: String,
        password: String?,
        confirmPassword: String?,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    )

    /** 이메일 인증 코드 발송. 코드 확인에 쓸 챌린지 토큰을 반환한다. */
    suspend fun requestEmailVerification(email: String, purpose: EmailVerificationPurpose): EmailChallengeVO

    /** 이메일 인증 코드 확인. 회원가입에 쓸 인증 완료 토큰을 반환한다. */
    suspend fun confirmEmailVerification(authCode: String, emailChallengeToken: String): EmailVerifiedVO
}
