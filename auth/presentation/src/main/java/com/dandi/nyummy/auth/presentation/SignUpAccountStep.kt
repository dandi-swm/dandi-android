package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPasswordVisibilityToggle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 회원가입 1단계. 이메일, 비밀번호, 비밀번호 확인. */
@Composable
internal fun ColumnScope.SignUpAccountStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val enabled = !uiState.isLoading
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    AuthFormHeader(
        title = stringResource(R.string.auth_signup_title),
        subtitle = stringResource(R.string.auth_signup_subtitle),
    )
    NyummyTextField(
        value = uiState.email,
        onValueChange = { onIntent(SignUpIntent.InputEmail(it)) },
        label = stringResource(R.string.auth_email_label),
        placeholder = stringResource(R.string.auth_email_placeholder),
        errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(NyummyTheme.spacing.s16))
    NyummyTextField(
        value = uiState.password,
        onValueChange = { onIntent(SignUpIntent.InputPassword(it)) },
        label = stringResource(R.string.auth_password_label),
        placeholder = stringResource(R.string.auth_signup_password_placeholder),
        helperText = stringResource(R.string.auth_signup_password_helper),
        errorMessage = uiState.passwordError?.let { stringResource(it.messageRes()) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = { NyummyPasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(NyummyTheme.spacing.s16))
    NyummyTextField(
        value = uiState.passwordConfirm,
        onValueChange = { onIntent(SignUpIntent.InputPasswordConfirm(it)) },
        label = stringResource(R.string.auth_signup_password_confirm_label),
        placeholder = stringResource(R.string.auth_signup_password_confirm_placeholder),
        errorMessage = uiState.passwordConfirmError?.let { stringResource(it.messageRes()) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = { NyummyPasswordVisibilityToggle(visible = confirmVisible, onToggle = { confirmVisible = !confirmVisible }) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 1단계 하단 고정 영역. "인증 코드 받기" + 약관 동의 고지. */
@Composable
internal fun SignUpAccountBottom(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    NyummyButton(
        text = stringResource(R.string.auth_signup_cta),
        onClick = { onIntent(SignUpIntent.ClickSendCode) },
        enabled = uiState.isSendCodeEnabled,
        modifier = Modifier.fillMaxWidth(),
    )
    NyummyText(
        text = stringResource(R.string.auth_signup_terms),
        style = NyummyTheme.typography.bodyS,
        color = NyummyTheme.colors.content.tertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun SignUpFieldError.messageRes(): Int = when (this) {
    SignUpFieldError.EMAIL_FORMAT -> R.string.auth_email_error_format
    SignUpFieldError.PASSWORD_POLICY -> R.string.auth_signup_error_password_policy
    SignUpFieldError.PASSWORD_CONFIRM_MISMATCH -> R.string.auth_signup_error_password_mismatch
    SignUpFieldError.NICKNAME_EMPTY -> R.string.auth_signup_error_nickname_empty_desc
}
