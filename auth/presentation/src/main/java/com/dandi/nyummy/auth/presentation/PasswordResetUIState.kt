package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.common.presentation.mvi.UiState

/** 비밀번호 찾기 단계. 한 라우트 안에서 이메일 → 인증 코드 → 완료 순서로 진행한다. */
enum class PasswordResetStep { EMAIL, CODE, DONE }

/**
 * @property emailVerifiedToken 코드 확인으로 받은 인증 완료 토큰. 재설정이 메일 발송 실패 등으로 실패하면
 * 남겨 두었다가 "인증하기"를 다시 누를 때 코드 확인 없이 재설정만 다시 부른다.
 */
data class PasswordResetUIState(
    val step: PasswordResetStep = PasswordResetStep.EMAIL,
    val email: String = "",
    val emailError: EmailLoginFieldError? = null,
    val emailChallengeToken: String = "",
    val emailVerifiedToken: String = "",
    val code: String = "",
    val codeError: String? = null,
    val resendRemainingSeconds: Int = 0,
    val isLoading: Boolean = false,
) : UiState {

    val isSendCodeEnabled: Boolean
        get() = email.isNotBlank() && !isLoading

    val isVerifyEnabled: Boolean
        get() = code.length == AuthCodeLength && !isLoading

    companion object {
        val empty = PasswordResetUIState()
    }
}
