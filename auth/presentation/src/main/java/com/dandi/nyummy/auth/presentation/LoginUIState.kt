package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.mvi.UiState

/**
 * 로그인 랜딩 화면 상태.
 *
 * 소셜 로그인은 `실행 요청([socialLoginToLaunch]) → 결과 대기([awaitingSocialLogin]) → 서버 검증` 순으로
 * 진행되며 그동안 [isLoading] 이 켜져 있다. 화면이 SDK 를 띄우면서 실행을 알리면 요청 값이 비워지므로,
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
) : UiState {
    companion object {
        val empty = LoginUIState()
    }
}
