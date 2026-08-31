package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface LoginIntent : MviIntent {
    // 소셜 로그인 버튼을 클릭했다.
    data class ClickSocialLogin(val socialType: SocialLoginType) : LoginIntent

    // 이메일 로그인 버튼을 클릭했다.
    data object ClickEmailLogin : LoginIntent

    data object ClickTestLogin : LoginIntent

}