package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 이메일 로그인 화면에서 발생하는 사용자 입력. */
sealed interface EmailLoginIntent : MviIntent {

    /** 이메일 입력값이 바뀌었다. */
    data class EmailChanged(val email: String) : EmailLoginIntent

    /** 비밀번호 입력값이 바뀌었다. */
    data class PasswordChanged(val password: String) : EmailLoginIntent

    /** 로그인 버튼을 탭했다. */
    data object ClickLogin : EmailLoginIntent
}
