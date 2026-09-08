package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialLoginType

interface AuthRepository {

    suspend fun socialLogin(socialLoginType: SocialLoginType): AuthTokenVO

    /** 이메일 로그인  */
    suspend fun login(email: String, password: String)

    /**
     * 회원가입. 성공 시 발급 토큰을 저장한다.
     *
     * @param emailVerifiedToken 이메일 인증 완료 토큰
     * @param birth 생년월일 (`yyyy-MM-dd` 형식, 선택)
     * @param height 키 (cm, 선택)
     * @param weight 몸무게 (kg, 선택)
     */
    suspend fun signUp(
        emailVerifiedToken: String,
        password: String,
        confirmPassword: String,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    )

    /** 이메일 인증 코드 발송. 코드 확인에 쓸 챌린지 토큰을 반환한다. */
    suspend fun requestEmailVerification(email: String): EmailChallengeVO

    /** 이메일 인증 코드 확인. 회원가입에 쓸 인증 완료 토큰을 반환한다. */
    suspend fun confirmEmailVerification(authCode: String, emailChallengeToken: String): EmailVerifiedVO
}