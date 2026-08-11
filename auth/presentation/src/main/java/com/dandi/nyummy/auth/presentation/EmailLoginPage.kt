package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 이메일 로그인 화면
 *
 * 이메일·비밀번호를 입력해 로그인한다. 비밀번호 찾기와 회원가입은 화면이 아직 없어 진입만 예약되어 있다.
 */
@Composable
fun EmailLoginPage(
    viewModel: EmailLoginViewModel = hiltViewModel<EmailLoginViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailLoginContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun EmailLoginContent(
    uiState: EmailLoginUIState,
    onIntent: (EmailLoginIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
    ) {
        Spacer(modifier = Modifier.height(EmailLoginWordmarkTopPadding))
        DandiText(
            text = stringResource(R.string.auth_login_wordmark),
            color = DesignSystemThemeImpl.designSystemColor.contentBrandWordmark,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
        )
        Spacer(modifier = Modifier.height(EmailLoginWordmarkTitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_email_login_headline),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Spacer(modifier = Modifier.height(EmailLoginTitleSubtitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_email_login_subtitle),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularL,
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.email,
            onValueChange = { onIntent(EmailLoginIntent.InputEmail(it)) },
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
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.password,
            onValueChange = { onIntent(EmailLoginIntent.InputPassword(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.auth_password_placeholder),
            label = stringResource(R.string.auth_password_label),
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (uiState.isLoginEnabled) onIntent(EmailLoginIntent.ClickLogin)
                },
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget)
                .clickable(enabled = !uiState.isLoading, role = Role.Button) {
                    onIntent(EmailLoginIntent.ClickForgotPassword)
                },
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = stringResource(R.string.auth_forgot_password),
                color = DesignSystemThemeImpl.designSystemColor.contentBrandWordmark,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
            )
        }
        Spacer(modifier = Modifier.height(EmailLoginForgotLoginSpacing))
        NyummyButton(
            label = stringResource(R.string.auth_login_button),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            enabled = uiState.isLoginEnabled,
            loading = uiState.isLoading,
            onClick = { onIntent(EmailLoginIntent.ClickLogin) },
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
        NyummyButton(
            label = stringResource(R.string.auth_signup_button),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.Large,
            enabled = !uiState.isLoading,
            onClick = { onIntent(EmailLoginIntent.ClickSignUp) },
        )
        Spacer(modifier = Modifier.height(EmailLoginSignUpFooterSpacing))
        DandiText(
            text = stringResource(R.string.auth_email_login_footer),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Spacer(modifier = Modifier.height(EmailLoginBottomPadding))
    }
}

private fun EmailLoginFieldError.messageRes(): Int = when (this) {
    EmailLoginFieldError.EMAIL_FORMAT -> R.string.auth_email_error_format
}

private val EmailLoginWordmarkTopPadding = 16.dp
private val EmailLoginWordmarkTitleSpacing = 40.dp
private val EmailLoginTitleSubtitleSpacing = 30.dp
private val EmailLoginForgotLoginSpacing = 14.dp
private val EmailLoginSignUpFooterSpacing = 30.dp
private val EmailLoginBottomPadding = 24.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmailLoginContentPreview() {
    DesignSystemTheme {
        EmailLoginContent(
            uiState = EmailLoginUIState.empty,
            onIntent = {},
        )
    }
}
