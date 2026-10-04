package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.presentation.social.SocialLoginResult
import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface LoginIntent : MviIntent {
    // 소셜 로그인 버튼을 클릭했다.
    data class ClickSocialLogin(val socialType: SocialLoginType) : LoginIntent

    // 화면이 소셜 제공자 SDK 로그인을 띄웠다.
    data class SocialLoginLaunched(val attempt: SocialLoginAttempt) : LoginIntent

    // 소셜 제공자 SDK 로그인 결과가 도착했다.
    data class SocialLoginResultReceived(
        val attempt: SocialLoginAttempt,
        val result: SocialLoginResult,
    ) : LoginIntent

    // 소셜 로그인 로딩 중 뒤로가기를 눌렀다.
    data object SocialLoginBackPressed : LoginIntent

    // 이메일 로그인 버튼을 클릭했다.
    data object ClickEmailLogin : LoginIntent

    // 테스트 계정 로그인 버튼을 클릭했다 (debug 빌드 전용).
    data object ClickTestLogin : LoginIntent

}
