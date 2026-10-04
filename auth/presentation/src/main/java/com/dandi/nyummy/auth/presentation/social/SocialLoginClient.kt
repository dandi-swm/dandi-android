package com.dandi.nyummy.auth.presentation.social

import android.app.Activity
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType

/**
 * 소셜 제공자 SDK 로그인 창구.
 *
 * 제공자를 추가할 때(구글·네이버) 이 인터페이스를 구현해 [socialLoginClientOf] 에 등록하면
 * 로그인 화면의 실행 흐름과 서버 검증(SocialLoginUseCase)은 그대로 재사용된다.
 *
 * SDK 는 앱 전환·커스텀 탭을 띄우느라 Activity 가 필요하므로 view 레이어에서만 호출하고,
 * 얻은 자격 증명은 ViewModel 을 거쳐 서버에서 검증한다.
 */
interface SocialLoginClient {

    val type: SocialLoginType

    /** SDK 가 초기화되어 로그인을 시작할 수 있는지. 앱 키 없이 빌드되면 false. */
    val isAvailable: Boolean

    /**
     * 제공자 로그인을 시작한다. [onResult] 는 메인 스레드에서 정확히 한 번 호출된다.
     *
     * 로그인 창이 떠 있는 동안 화면이 재생성될 수 있으므로 [onResult] 에 Activity 를 캡처하지 않는다.
     */
    fun launch(activity: Activity, onResult: (SocialLoginResult) -> Unit)
}

/** 제공자 SDK 로그인 결과입니다. */
sealed interface SocialLoginResult {

    /** 서버 검증에 넘길 자격 증명을 얻었습니다. */
    data class Success(val credential: SocialCredentialVO) : SocialLoginResult

    /** 사용자가 로그인·동의를 취소했습니다. */
    data object Cancelled : SocialLoginResult

    /** SDK 오류로 자격 증명을 얻지 못했습니다. */
    data object Failed : SocialLoginResult

    /** 아직 연동되지 않았거나 앱 키 없이 빌드돼 쓸 수 없는 제공자입니다. */
    data object Unavailable : SocialLoginResult
}
