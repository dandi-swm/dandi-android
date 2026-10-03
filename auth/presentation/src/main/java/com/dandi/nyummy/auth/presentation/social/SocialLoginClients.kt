package com.dandi.nyummy.auth.presentation.social

import android.app.Activity
import android.content.Context
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.presentation.BuildConfig
import com.kakao.sdk.common.KakaoSdk

/**
 * 제공자별 로그인 클라이언트. 아직 연동하지 않은 제공자는 null 이다.
 * 구글·네이버를 붙일 때 [SocialLoginClient] 구현체를 만들어 여기에 등록한다.
 */
fun socialLoginClientOf(type: SocialLoginType): SocialLoginClient? = when (type) {
    SocialLoginType.KAKAO -> KakaoLoginClient
    SocialLoginType.GOOGLE, SocialLoginType.NAVER -> null
}

/**
 * [type] 제공자 로그인을 시작한다.
 * 연동되지 않았거나 SDK 가 초기화되지 않은 제공자는 곧바로 [SocialLoginResult.Unavailable] 을 돌려준다.
 */
fun launchSocialLogin(
    type: SocialLoginType,
    activity: Activity,
    onResult: (SocialLoginResult) -> Unit,
) {
    val client = socialLoginClientOf(type)
    if (client == null || !client.isAvailable) {
        onResult(SocialLoginResult.Unavailable)
        return
    }
    client.launch(activity, onResult)
}

/**
 * 앱 시작 시 소셜 제공자 SDK 를 초기화한다(Application.onCreate).
 * 앱 키 없이 빌드된 경우(CI 등)는 건너뛰고, 그 제공자는 [SocialLoginResult.Unavailable] 로 처리된다.
 */
fun initializeSocialLoginSdks(context: Context) {
    if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
        KakaoSdk.init(context, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
