package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCodeInput
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import java.util.Locale

/**
 * 회원가입 2단계. 이메일로 받은 6자리 인증 코드 입력.
 * 오류는 셀 아래에 원인과 방법을 보여 준다.
 */
@Composable
internal fun ColumnScope.SignUpCodeStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val theme = NyummyTheme
    AuthFormHeader(
        title = stringResource(R.string.auth_signup_code_title),
        subtitle = stringResource(R.string.auth_signup_code_subtitle, uiState.email),
    )
    NyummyCodeInput(
        value = uiState.code,
        onValueChange = { onIntent(SignUpIntent.InputCode(it)) },
        length = SignUpUIState.CODE_LENGTH,
        isError = uiState.codeError != null,
        errorMessage = uiState.codeError,
        enabled = !uiState.isLoading,
        contentDescription = stringResource(R.string.auth_signup_code_input_description),
        modifier = Modifier.fillMaxWidth(),
    )
    uiState.codeError?.let { codeError ->
        Spacer(Modifier.height(theme.spacing.s8))
        NyummyText(text = codeError, style = theme.typography.bodyS, color = theme.colors.content.danger)
    }
    Spacer(Modifier.height(theme.spacing.s8))
    NyummyText(
        text = stringResource(R.string.auth_signup_code_spam_hint),
        style = theme.typography.bodyS,
        color = theme.colors.content.tertiary,
    )
}

/**
 * 2단계 하단 고정 영역. "코드를 못 받았나요? 다시 보내기" + "인증하기".
 * 발송 직후 5분 동안은 남은 시간을 회색으로 보여 주고, 0이 되면 밑줄 친 브랜드색 링크로 바뀐다.
 */
@Composable
internal fun SignUpCodeBottom(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val theme = NyummyTheme
    val canResend = uiState.resendRemainingSeconds <= 0 && !uiState.isLoading
    Row(
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = stringResource(R.string.auth_signup_code_resend_question),
            style = theme.typography.bodyM,
            color = theme.colors.content.tertiary,
        )
        if (uiState.resendRemainingSeconds > 0) {
            NyummyText(
                text = stringResource(
                    R.string.auth_signup_code_resend_countdown,
                    uiState.resendRemainingSeconds.toCountdownText(),
                ),
                style = theme.typography.labelM,
                color = theme.colors.content.disabled,
            )
        } else {
            val label = stringResource(R.string.auth_signup_code_resend)
            NyummyText(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
                },
                style = theme.typography.labelM,
                color = if (canResend) theme.colors.content.brand else theme.colors.content.disabled,
                modifier = Modifier.nyummyClickable(
                    onClick = { onIntent(SignUpIntent.ClickResendCode) },
                    enabled = canResend,
                ),
            )
        }
    }
    NyummyButton(
        text = stringResource(R.string.auth_signup_code_cta),
        onClick = { onIntent(SignUpIntent.ClickVerifyCode) },
        enabled = uiState.isVerifyEnabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Int.toCountdownText(): String =
    String.format(Locale.US, "%02d:%02d", this / 60, this % 60)
