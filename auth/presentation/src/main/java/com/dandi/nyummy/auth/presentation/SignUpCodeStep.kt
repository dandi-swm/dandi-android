package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyCodeInput
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import java.util.Locale

/**
 * 회원가입 2단계 — 이메일 인증 코드 입력 (Figma `LIVE / AUTH · Email Verification Code`).
 *
 * 발송 직후 5분 재발송 쿨다운을 mm:ss 카운트다운으로 보여주고, 0이 되면
 * "다시 보내기" 링크가 활성화된다.
 */
@Composable
internal fun SignUpCodeStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    Column {
        DandiText(
            text = stringResource(R.string.auth_signup_code_title),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Spacer(Modifier.height(SignUpTitleSubtitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_signup_code_subtitle, uiState.email),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularL,
        )
        Spacer(Modifier.height(SignUpCodeInputSpacing))
        NyummyCodeInput(
            value = uiState.code,
            onValueChange = { onIntent(SignUpIntent.InputCode(it)) },
            length = SignUpUIState.CODE_LENGTH,
            enabled = !uiState.isLoading,
            inputDescription = stringResource(R.string.auth_signup_code_input_description),
        )
        Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space20))
        DandiText(
            text = stringResource(R.string.auth_signup_code_spam_hint),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Spacer(Modifier.height(SignUpCodeResendSpacing))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DandiText(
                text = stringResource(R.string.auth_signup_code_resend_question),
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            Box(
                modifier = Modifier
                    .heightIn(min = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget)
                    .clickable(enabled = !uiState.isLoading, role = Role.Button) {
                        onIntent(SignUpIntent.ClickResendCode)
                    },
                contentAlignment = Alignment.Center,
            ) {
                DandiText(
                    text = if (uiState.resendRemainingSeconds > 0) {
                        stringResource(
                            R.string.auth_signup_code_resend_countdown,
                            uiState.resendRemainingSeconds.toCountdownText(),
                        )
                    } else {
                        stringResource(R.string.auth_signup_code_resend)
                    },
                    color = DesignSystemThemeImpl.designSystemColor.contentActionSecondary,
                    textDecoration = TextDecoration.Underline,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
            }
        }
        Spacer(Modifier.height(SignUpCodeCtaSpacing))
        NyummyButton(
            label = stringResource(R.string.auth_signup_code_cta),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            enabled = uiState.isVerifyEnabled,
            loading = uiState.isLoading,
            onClick = { onIntent(SignUpIntent.ClickVerifyCode) },
        )
    }
}

private fun Int.toCountdownText(): String =
    String.format(Locale.US, "%02d:%02d", this / 60, this % 60)

private val SignUpCodeInputSpacing = 28.dp
private val SignUpCodeResendSpacing = 120.dp
private val SignUpCodeCtaSpacing = 16.dp
