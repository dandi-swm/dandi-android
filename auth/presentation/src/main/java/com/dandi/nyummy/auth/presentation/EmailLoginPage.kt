package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyScreenHeader
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 이메일 로그인 화면. 이메일·비밀번호를 받아 로그인한다.
 *
 * 성공 시 홈 이동과 실패 다이얼로그는 [com.dandi.nyummy.auth.domain.LoginUseCase] 가 담당한다.
 */
@Composable
fun EmailLoginPage(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailLoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailLoginScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun EmailLoginScreen(
    uiState: EmailLoginUIState,
    onIntent: (EmailLoginIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
    ) {
        NyummyScreenHeader(
            title = stringResource(R.string.auth_email_login_title),
            onBackClick = onBackClick,
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.email,
            onValueChange = { onIntent(EmailLoginIntent.EmailChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.auth_email_label),
            placeholder = stringResource(R.string.auth_email_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space16))
        NyummyTextField(
            value = uiState.password,
            onValueChange = { onIntent(EmailLoginIntent.PasswordChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.auth_password_label),
            placeholder = stringResource(R.string.auth_password_placeholder),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(modifier = Modifier.weight(1f))
        NyummyButton(
            label = stringResource(R.string.auth_login_button),
            modifier = Modifier.fillMaxWidth(),
            size = NyummyButtonSize.Large,
            enabled = uiState.isLoginEnabled,
            loading = uiState.isLoading,
            onClick = { onIntent(EmailLoginIntent.ClickLogin) },
        )
        Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space16))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmailLoginPagePreview() {
    DesignSystemTheme {
        EmailLoginScreen(
            uiState = EmailLoginUIState(email = "cat@dandi.com"),
            onIntent = {},
            onBackClick = {},
        )
    }
}
