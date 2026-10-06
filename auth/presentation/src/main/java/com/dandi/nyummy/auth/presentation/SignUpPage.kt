package com.dandi.nyummy.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStepIndicator
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 회원가입 퍼널 화면. 계정 정보 → 이메일 인증 코드 → 프로필 입력의
 * 3단계를 한 라우트 안에서 진행한다(비밀번호 등 민감 값이 라우트 인자로 남지 않도록).
 *
 * [isSocialSignUp]이면 소셜 로그인 신규 회원의 가입(`/signup/social`)으로, 단계 표시 없이 프로필 입력만 보인다.
 */
@Composable
fun SignUpPage(
    isSocialSignUp: Boolean = false,
    viewModel: SignUpViewModel = hiltViewModel<SignUpViewModel, SignUpViewModel.Factory>(
        creationCallback = { factory -> factory.create(isSocialSignUp) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpContent(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
internal fun SignUpContent(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    // 소셜 가입은 이전 단계가 없으므로 시스템 뒤로 가기가 라우트를 그대로 닫게 둔다.
    BackHandler(enabled = uiState.step != SignUpStep.ACCOUNT && !uiState.isSocialSignUp) {
        onIntent(SignUpIntent.ClickBackStep)
    }
    AuthFormScaffold(
        onBackClick = { onIntent(SignUpIntent.ClickBack) },
        bottomDivider = uiState.step == SignUpStep.PROFILE,
        bottom = {
            when (uiState.step) {
                SignUpStep.ACCOUNT -> SignUpAccountBottom(uiState = uiState, onIntent = onIntent)
                SignUpStep.CODE -> SignUpCodeBottom(uiState = uiState, onIntent = onIntent)
                SignUpStep.PROFILE -> SignUpProfileBottom(uiState = uiState, onIntent = onIntent)
            }
        },
    ) {
        if (!uiState.isSocialSignUp) {
            NyummyStepIndicator(currentStep = uiState.step.ordinal + 1, totalSteps = SignUpStep.entries.size)
            Spacer(Modifier.height(NyummyTheme.spacing.s24))
        }
        when (uiState.step) {
            SignUpStep.ACCOUNT -> SignUpAccountStep(uiState = uiState, onIntent = onIntent)
            SignUpStep.CODE -> SignUpCodeStep(uiState = uiState, onIntent = onIntent)
            SignUpStep.PROFILE -> SignUpProfileStep(uiState = uiState, onIntent = onIntent)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpAccountContentPreview() {
    NyummyTheme {
        SignUpContent(uiState = SignUpUIState.empty, onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpCodeContentPreview() {
    NyummyTheme {
        SignUpContent(
            uiState = SignUpUIState.empty.copy(
                step = SignUpStep.CODE,
                email = "nyummy@cat.com",
                code = "482",
                resendRemainingSeconds = 272,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun SignUpProfileContentPreview() {
    NyummyTheme {
        SignUpContent(
            uiState = SignUpUIState.empty.copy(step = SignUpStep.PROFILE),
            onIntent = {},
        )
    }
}
