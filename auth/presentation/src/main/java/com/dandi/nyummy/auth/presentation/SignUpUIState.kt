package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.mvi.UiState

/**
 * 회원가입 퍼널 단계. 한 라우트(`/signup`) 안에서 순차 진행된다.
 */
enum class SignUpStep { ACCOUNT, CODE, PROFILE }

data class SignUpUIState(
    val step: SignUpStep = SignUpStep.ACCOUNT,
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val emailError: SignUpFieldError? = null,
    val passwordError: SignUpFieldError? = null,
    val passwordConfirmError: SignUpFieldError? = null,
    val code: String = "",
    val codeError: String? = null,
    val resendRemainingSeconds: Int = 0,
    val nickname: String = "",
    val gender: Gender = Gender.MALE,
    val birthYear: Int = DEFAULT_BIRTH_YEAR,
    val birthMonth: Int = DEFAULT_BIRTH_MONTH,
    val birthDay: Int = DEFAULT_BIRTH_DAY,
    val height: Int = DEFAULT_HEIGHT,
    val weight: Int = DEFAULT_WEIGHT,
    val nicknameError: SignUpFieldError? = null,
    val isLoading: Boolean = false,
) : UiState {

    val isSendCodeEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && passwordConfirm.isNotBlank() && !isLoading

    val isVerifyEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isLoading

    companion object {
        val empty = SignUpUIState()
        const val CODE_LENGTH = 6
        const val DEFAULT_BIRTH_YEAR = 2000
        const val DEFAULT_BIRTH_MONTH = 3
        const val DEFAULT_BIRTH_DAY = 30
        const val DEFAULT_HEIGHT = 172
        const val DEFAULT_WEIGHT = 70
    }
}

/** 해당 연·월의 일수. `java.time`은 minSdk 24에서 desugaring 없이 못 쓴다. */
internal fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(year)) 29 else 28
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
