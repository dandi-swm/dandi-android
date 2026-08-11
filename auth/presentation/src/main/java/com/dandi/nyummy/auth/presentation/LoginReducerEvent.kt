package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface LoginReducerEvent : ReducerEvent {

    /**
     * 소셜 로그인 버튼이 클릭되어 로그인 요청이 시작됐다.
     */
    data class SocialLoginStarted(val socialType: SocialLoginType) : LoginReducerEvent

    /**
     * 로그인 요청이 끝났다 (성공 시 네비게이션은 UseCase 가 수행).
     */
    data object LoginFinished : LoginReducerEvent
    data object EmailLoginClicked : LoginReducerEvent

}