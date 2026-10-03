package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.mvi.UiState

/**
 * 로그인 랜딩 화면 상태.
 *
 * 소셜 로그인은 `실행 요청([socialLoginToLaunch]) → 결과 대기([awaitingSocialLogin]) →
 * 서버 검증([verifyingSocialLogin])` 순으로 진행되며 그동안 [isLoading] 이 켜져 있다. 화면이 SDK 를 띄우면서 실행을 알리면 요청 값이 비워지므로,
 * 로그인 창이 떠 있는 동안 화면이 재생성돼도 SDK 가 다시 실행되지 않는다.
 */
data class LoginUIState(
    val isLoading: Boolean = false,
    // debug 빌드에 테스트 계정이 주입됐을 때만 true — 테스트 계정 로그인 버튼 노출 여부.
    val isTestLoginAvailable: Boolean = false,
    // 화면이 실행해야 하는 소셜 로그인. 실행을 알리면 비워진다.
    val socialLoginToLaunch: SocialLoginType? = null,
    // SDK 결과를 기다리는 소셜 로그인. 이 값과 다른(지난 시도의) 결과는 무시한다.
    val awaitingSocialLogin: SocialLoginType? = null,
    // 서버에서 검증 중인 소셜 로그인. 이 단계에서는 뒤로가기로 취소할 수 없다.
    val verifyingSocialLogin: SocialLoginType? = null,
) : UiState {

    /**
     * 진행 중인 소셜 로그인. 실행 요청·결과 대기·서버 검증 중 어느 단계든 값이 있다.
     * 로그인 창에서 돌아온 뒤 카카오 토큰 발급과 서버 검증이 끝날 때까지 로딩을 보여주는 데 쓴다.
     */
    val socialLoginInProgress: SocialLoginType?
        get() = socialLoginToLaunch ?: awaitingSocialLogin ?: verifyingSocialLogin
    companion object {
        val empty = LoginUIState()
    }
}
