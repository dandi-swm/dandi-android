package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

/**
 * 이메일 로그인 화면 상태.
 *
 * @property email 이메일 입력값
 * @property password 비밀번호 입력값
 * @property isLoading 로그인 요청 진행 중 여부
 */
data class EmailLoginUIState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) : UiState {

    /** 두 입력이 모두 채워졌고 요청 중이 아닐 때만 로그인 가능. */
    val isLoginEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading

    companion object {
        val empty = EmailLoginUIState()
    }
}
