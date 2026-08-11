package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialLoginType

interface AuthRepository {

    suspend fun socialLogin(socialLoginType: SocialLoginType): AuthTokenVO

    /** 이메일 로그인  */
    suspend fun login(email: String, password: String)


}