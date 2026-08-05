package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SignupFormVO

interface AuthRepository {

    /** 이메일 로그인. 성공 시 토큰 쌍을 반환한다. */
    suspend fun login(email: String, password: String): AuthTokenVO

    /** 회원가입. 성공 시 토큰 쌍을 반환한다. */
    suspend fun signup(form: SignupFormVO): AuthTokenVO

    /** 이메일 인증 코드 발송 요청. */
    suspend fun requestEmailVerification(email: String)

    /** 이메일로 받은 인증 코드 검증. */
    suspend fun confirmEmailVerification(email: String, verificationCode: Int)

    /** 비밀번호 변경. */
    suspend fun changePassword(userId: Long, password: String, newPassword: String)
}
