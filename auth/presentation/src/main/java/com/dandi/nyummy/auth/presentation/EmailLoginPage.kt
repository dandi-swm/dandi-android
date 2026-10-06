package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPasswordVisibilityToggle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 이메일 로그인 화면.
 *
 * 이메일과 비밀번호를 입력해 로그인한다. 로그인 중에는 입력과 버튼을 막고 버튼 문구를 "로그인 중…"으로 바꾼다.
 * 서버 오류(이메일 또는 비밀번호 불일치 등)는 UseCase가 공통 다이얼로그로 띄운다.
 * 비밀번호 찾기는 화면이 아직 없어 진입만 예약되어 있다.
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
internal fun EmailLoginContent(
    uiState: EmailLoginUIState,
    onIntent: (EmailLoginIntent) -> Unit,
) {
    val theme = NyummyTheme
    val enabled = !uiState.isLoading
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AuthFormScaffold(
        onBackClick = { onIntent(EmailLoginIntent.ClickBack) },
        bottom = {
            NyummyButton(
                text = stringResource(
                    if (uiState.isLoading) R.string.auth_login_button_loading else R.string.auth_login_button,
                ),
                onClick = { onIntent(EmailLoginIntent.ClickLogin) },
                enabled = uiState.isLoginEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            NyummyButton(
                text = stringResource(R.string.auth_signup_button),
                onClick = { onIntent(EmailLoginIntent.ClickSignUp) },
                style = NyummyButtonStyle.Secondary,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            NyummyText(
                text = stringResource(R.string.auth_email_login_footer),
                style = theme.typography.bodyS,
                color = theme.colors.content.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        AuthFormHeader(
            title = stringResource(R.string.auth_email_login_headline),
            subtitle = stringResource(R.string.auth_email_login_subtitle),
        )
        NyummyTextField(
            value = uiState.email,
            onValueChange = { onIntent(EmailLoginIntent.InputEmail(it)) },
            label = stringResource(R.string.auth_email_label),
            placeholder = stringResource(R.string.auth_email_placeholder),
            errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(theme.spacing.s16))
        NyummyTextField(
            value = uiState.password,
            onValueChange = { onIntent(EmailLoginIntent.InputPassword(it)) },
            label = stringResource(R.string.auth_password_label),
            placeholder = stringResource(R.string.auth_password_placeholder),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (uiState.isLoginEnabled) onIntent(EmailLoginIntent.ClickLogin) },
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { NyummyPasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = theme.spacing.s4),
            contentAlignment = Alignment.CenterEnd,
        ) {
            NyummyButton(
                text = stringResource(R.string.auth_forgot_password),
                onClick = { onIntent(EmailLoginIntent.ClickForgotPassword) },
                style = NyummyButtonStyle.Ghost,
                size = NyummyButtonSize.M,
                enabled = enabled,
            )
        }
    }
}

/**
 * 인증 화면 공통 틀. 상단 바(뒤로 가기) + 스크롤되는 본문 + 화면 아래에 고정되는 버튼 영역.
 * 키보드가 열리면 버튼 영역이 키보드 위로 올라가고, 본문은 남은 높이에서 스크롤된다.
 */
@Composable
internal fun AuthFormScaffold(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomDivider: Boolean = false,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = NyummyTheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colors.bg.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NyummyTopBar(title = "", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = FormMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = theme.spacing.gutter)
                .padding(top = theme.spacing.s16, bottom = theme.spacing.s24),
            content = content,
        )
        if (bottomDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(theme.borderWidth.hairline)
                    .background(theme.colors.border.subtle),
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = FormMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = theme.spacing.gutter)
                .padding(top = theme.spacing.s12, bottom = theme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.s12),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottom,
        )
    }
}

/** 인증 화면 제목(display/m) + 부제(body/m). 아래 28을 띄운다. */
@Composable
internal fun AuthFormHeader(title: String, subtitle: String) {
    val theme = NyummyTheme
    NyummyText(text = title, style = theme.typography.displayM)
    Spacer(Modifier.height(theme.spacing.s8))
    NyummyText(text = subtitle, style = theme.typography.bodyM, color = theme.colors.content.secondary)
    Spacer(Modifier.height(HeaderBottomGap))
}

private fun EmailLoginFieldError.messageRes(): Int = when (this) {
    EmailLoginFieldError.EMAIL_FORMAT -> R.string.auth_email_error_format
}

private val FormMaxWidth = 480.dp
private val HeaderBottomGap = 28.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmailLoginContentPreview() {
    NyummyTheme {
        EmailLoginContent(uiState = EmailLoginUIState.empty, onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmailLoginErrorPreview() {
    NyummyTheme {
        EmailLoginContent(
            uiState = EmailLoginUIState(email = "nyummy@cat", password = "password", emailError = EmailLoginFieldError.EMAIL_FORMAT),
            onIntent = {},
        )
    }
}
