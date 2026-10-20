package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/** 회원가입 2단계. 이메일로 받은 6자리 인증 코드 입력. */
@Composable
internal fun ColumnScope.SignUpCodeStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    AuthCodeStepContent(
        email = uiState.email,
        code = uiState.code,
        codeError = uiState.codeError,
        isLoading = uiState.isLoading,
        onCodeChange = { onIntent(SignUpIntent.InputCode(it)) },
    )
}

/** 2단계 하단 고정 영역. "코드를 못 받았나요? 다시 보내기" + "인증하기". */
@Composable
internal fun SignUpCodeBottom(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    AuthCodeStepBottom(
        resendRemainingSeconds = uiState.resendRemainingSeconds,
        isLoading = uiState.isLoading,
        isVerifyEnabled = uiState.isVerifyEnabled,
        onResendClick = { onIntent(SignUpIntent.ClickResendCode) },
        onVerifyClick = { onIntent(SignUpIntent.ClickVerifyCode) },
    )
}
