package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface SignUpIntent : MviIntent {
    data class InputEmail(val value: String) : SignUpIntent
    data class InputPassword(val value: String) : SignUpIntent
    data class InputPasswordConfirm(val value: String) : SignUpIntent
    data object ClickSendCode : SignUpIntent

    data class InputCode(val value: String) : SignUpIntent
    data object ClickVerifyCode : SignUpIntent
    data object ClickResendCode : SignUpIntent

    data class InputNickname(val value: String) : SignUpIntent
    data class SelectGender(val value: Gender) : SignUpIntent
    data class SelectBirthYear(val value: Int) : SignUpIntent
    data class SelectBirthMonth(val value: Int) : SignUpIntent
    data class SelectBirthDay(val value: Int) : SignUpIntent
    data class SelectHeight(val value: Int) : SignUpIntent
    data class SelectWeight(val value: Int) : SignUpIntent
    data object ClickSubmit : SignUpIntent

    data object ClickBackStep : SignUpIntent
}
