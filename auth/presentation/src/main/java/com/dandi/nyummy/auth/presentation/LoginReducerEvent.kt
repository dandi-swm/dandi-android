package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface LoginReducerEvent : ReducerEvent {

    /**
     * 소셜 로그인 버튼이 클릭되어 화면에 SDK 실행을 요청한다.
     */
    data class SocialLoginStarted(val socialType: SocialLoginType) : LoginReducerEvent

    /**
     * 화면이 SDK 를 띄웠다. 실행 요청을 비우고 결과를 기다린다.
     */
    data class SocialLoginLaunched(val socialType: SocialLoginType) : LoginReducerEvent

    /**
     * SDK 에서 자격 증명을 받아 서버 검증을 시작한다.
     */
    data object SocialLoginVerifying : LoginReducerEvent

    /**
     * 로그인 요청이 끝났다 (성공 시 네비게이션은 UseCase 가 수행).
     */
    data object LoginFinished : LoginReducerEvent
    data object EmailLoginClicked : LoginReducerEvent

    data object TestLoginClicked : LoginReducerEvent

}
