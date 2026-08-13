package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialLoginType

interface AuthRepository {

    suspend fun socialLogin(socialLoginType: SocialLoginType): AuthTokenVO

    /** 이메일 로그인  */
    suspend fun login(email: String, password: String)

    /**
     * 회원가입. 성공 시 발급 토큰을 저장한다.
     *
     * @param birth 생년월일 (`yyyy-MM-dd` 형식)
     * @param height 키 (cm)
     * @param weight 몸무게 (kg)
     */
    suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        gender: Gender,
        birth: String,
        height: Int,
        weight: Int,
    )

    /** 이메일 인증 코드 발송 */
    suspend fun requestEmailVerification(email: String)

    /** 이메일 인증 코드 확인 */
    suspend fun confirmEmailVerification(email: String, verificationCode: String)
}