package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface PasswordResetIntent : MviIntent {
    data class InputEmail(val value: String) : PasswordResetIntent
    data object ClickSendCode : PasswordResetIntent

    data class InputCode(val value: String) : PasswordResetIntent
    data object ClickVerifyCode : PasswordResetIntent
    data object ClickResendCode : PasswordResetIntent

    /** 완료 단계의 "로그인하기". 이메일 로그인 화면으로 돌아간다. */
    data object ClickLogin : PasswordResetIntent

    /** 상단 바 뒤로 가기와 코드 단계의 시스템 뒤로 가기. 코드 단계면 이메일 단계로, 아니면 화면을 닫는다. */
    data object ClickBack : PasswordResetIntent
}
