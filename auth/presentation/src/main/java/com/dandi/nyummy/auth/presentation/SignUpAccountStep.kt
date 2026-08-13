package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 회원가입 1단계 — 이메일·비밀번호 입력 (Figma `LIVE / AUTH · Email Signup`).
 */
@Composable
internal fun SignUpAccountStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    Column {
        DandiText(
            text = stringResource(R.string.auth_signup_title),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Spacer(Modifier.height(SignUpTitleSubtitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_signup_subtitle),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularL,
        )
        Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.email,
            onValueChange = { onIntent(SignUpIntent.InputEmail(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.auth_email_placeholder),
            label = stringResource(R.string.auth_email_label),
            helperText = uiState.emailError?.let { stringResource(it.messageRes()) },
            isError = uiState.emailError != null,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.password,
            onValueChange = { onIntent(SignUpIntent.InputPassword(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.auth_signup_password_placeholder),
            label = stringResource(R.string.auth_password_label),
            helperText = stringResource(
                uiState.passwordError?.messageRes() ?: R.string.auth_signup_password_helper,
            ),
            isError = uiState.passwordError != null,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.passwordConfirm,
            onValueChange = { onIntent(SignUpIntent.InputPasswordConfirm(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.auth_signup_password_confirm_placeholder),
            label = stringResource(R.string.auth_signup_password_confirm_label),
            helperText = uiState.passwordConfirmError?.let { stringResource(it.messageRes()) },
            isError = uiState.passwordConfirmError != null,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(SignUpAccountCtaSpacing))
        NyummyButton(
            label = stringResource(R.string.auth_signup_cta),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            enabled = uiState.isSendCodeEnabled,
            loading = uiState.isLoading,
            onClick = { onIntent(SignUpIntent.ClickSendCode) },
        )
        Spacer(Modifier.height(SignUpAccountTermsSpacing))
        DandiText(
            text = stringResource(R.string.auth_login_terms),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
    }
}

internal fun SignUpFieldError.messageRes(): Int = when (this) {
    SignUpFieldError.EMAIL_FORMAT -> R.string.auth_email_error_format
    SignUpFieldError.PASSWORD_POLICY -> R.string.auth_signup_error_password_policy
    SignUpFieldError.PASSWORD_CONFIRM_MISMATCH -> R.string.auth_signup_error_password_mismatch
    SignUpFieldError.NICKNAME_EMPTY -> R.string.auth_signup_error_nickname_empty_desc
}

private val SignUpAccountCtaSpacing = 30.dp
private val SignUpAccountTermsSpacing = 12.dp
