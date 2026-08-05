package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 이메일 로그인 화면 상태를 변이시키는 내부 이벤트. */
sealed interface EmailLoginReducerEvent : ReducerEvent {

    /** 이메일 입력값이 갱신됐다. */
    data class EmailChanged(val email: String) : EmailLoginReducerEvent

    /** 비밀번호 입력값이 갱신됐다. */
    data class PasswordChanged(val password: String) : EmailLoginReducerEvent

    /** 로그인 요청이 시작됐다. */
    data object LoginStarted : EmailLoginReducerEvent

    /** 로그인 요청이 끝났다 (성공 시 네비게이션은 UseCase 가 수행). */
    data object LoginFinished : EmailLoginReducerEvent
}
