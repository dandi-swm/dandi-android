package com.dandi.nyummy.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStepIndicator
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 비밀번호 찾기 화면. 이메일 → 인증 코드 → 완료를 한 라우트 안에서 진행한다.
 * 코드 단계는 회원가입과 같은 화면을 쓰고, 완료하면 임시 비밀번호를 보낸 메일 주소를 알려 준다.
 */
@Composable
fun PasswordResetPage(
    viewModel: PasswordResetViewModel = hiltViewModel<PasswordResetViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PasswordResetContent(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
internal fun PasswordResetContent(
    uiState: PasswordResetUIState,
    onIntent: (PasswordResetIntent) -> Unit,
) {
    BackHandler(enabled = uiState.step == PasswordResetStep.CODE) {
        onIntent(PasswordResetIntent.ClickBack)
    }
    AuthFormScaffold(
        onBackClick = { onIntent(PasswordResetIntent.ClickBack) },
        bottom = {
            when (uiState.step) {
                PasswordResetStep.EMAIL -> NyummyButton(
                    text = stringResource(R.string.auth_password_reset_cta),
                    onClick = { onIntent(PasswordResetIntent.ClickSendCode) },
                    enabled = uiState.isSendCodeEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordResetStep.CODE -> AuthCodeStepBottom(
                    resendRemainingSeconds = uiState.resendRemainingSeconds,
                    isLoading = uiState.isLoading,
                    isVerifyEnabled = uiState.isVerifyEnabled,
                    onResendClick = { onIntent(PasswordResetIntent.ClickResendCode) },
                    onVerifyClick = { onIntent(PasswordResetIntent.ClickVerifyCode) },
                )
                PasswordResetStep.DONE -> NyummyButton(
                    text = stringResource(R.string.auth_password_reset_done_cta),
                    onClick = { onIntent(PasswordResetIntent.ClickLogin) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        if (uiState.step != PasswordResetStep.DONE) {
            NyummyStepIndicator(currentStep = uiState.step.ordinal + 1, totalSteps = InputStepCount)
            Spacer(Modifier.height(NyummyTheme.spacing.s24))
        }
        when (uiState.step) {
            PasswordResetStep.EMAIL -> PasswordResetEmailStep(uiState = uiState, onIntent = onIntent)
            PasswordResetStep.CODE -> AuthCodeStepContent(
                email = uiState.email,
                code = uiState.code,
                codeError = uiState.codeError,
                isLoading = uiState.isLoading,
                onCodeChange = { onIntent(PasswordResetIntent.InputCode(it)) },
            )
            PasswordResetStep.DONE -> PasswordResetDoneStep(email = uiState.email)
        }
    }
}

/** 1단계. 가입한 이메일 입력. */
@Composable
private fun ColumnScope.PasswordResetEmailStep(
    uiState: PasswordResetUIState,
    onIntent: (PasswordResetIntent) -> Unit,
) {
    AuthFormHeader(
        title = stringResource(R.string.auth_password_reset_title),
        subtitle = stringResource(R.string.auth_password_reset_subtitle),
    )
    NyummyTextField(
        value = uiState.email,
        onValueChange = { onIntent(PasswordResetIntent.InputEmail(it)) },
        label = stringResource(R.string.auth_email_label),
        placeholder = stringResource(R.string.auth_email_placeholder),
        errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) },
        enabled = !uiState.isLoading,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (uiState.isSendCodeEnabled) onIntent(PasswordResetIntent.ClickSendCode) },
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 완료. 임시 비밀번호를 보낸 메일 주소를 알려 주고 로그인으로 돌려보낸다. */
@Composable
private fun ColumnScope.PasswordResetDoneStep(email: String) {
    AuthFormHeader(
        title = stringResource(R.string.auth_password_reset_done_title),
        subtitle = stringResource(R.string.auth_password_reset_done_subtitle, email),
    )
    NyummyText(
        text = stringResource(R.string.auth_signup_code_spam_hint),
        style = NyummyTheme.typography.bodyS,
        color = NyummyTheme.colors.content.tertiary,
    )
}

/** 단계 표시에 넣는 입력 단계 수(이메일, 인증 코드). 완료 단계는 세지 않는다. */
private const val InputStepCount = 2

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PasswordResetEmailPreview() {
    NyummyTheme {
        PasswordResetContent(uiState = PasswordResetUIState.empty, onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PasswordResetCodePreview() {
    NyummyTheme {
        PasswordResetContent(
            uiState = PasswordResetUIState(
                step = PasswordResetStep.CODE,
                email = "nyummy@cat.com",
                code = "482",
                resendRemainingSeconds = 272,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PasswordResetDonePreview() {
    NyummyTheme {
        PasswordResetContent(
            uiState = PasswordResetUIState(step = PasswordResetStep.DONE, email = "nyummy@cat.com"),
            onIntent = {},
        )
    }
}
