package com.dandi.nyummy.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 회원가입 퍼널 화면. 계정 정보 → 이메일 인증 코드 → 프로필 입력의 3단계를
 * 한 라우트 안에서 진행한다 (비밀번호 등 민감 값이 라우트 인자로 남지 않도록).
 */
@Composable
fun SignUpPage(viewModel: SignUpViewModel = hiltViewModel<SignUpViewModel>()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpContent(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun SignUpContent(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    BackHandler(enabled = uiState.step != SignUpStep.ACCOUNT) {
        onIntent(SignUpIntent.ClickBackStep)
    }
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
        Spacer(Modifier.height(SignUpWordmarkTopPadding))
        DandiText(
            text = stringResource(R.string.auth_login_wordmark),
            color = DesignSystemThemeImpl.designSystemColor.contentBrandWordmark,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
        )
        Spacer(Modifier.height(SignUpWordmarkTitleSpacing))
        when (uiState.step) {
            SignUpStep.ACCOUNT -> SignUpAccountStep(uiState = uiState, onIntent = onIntent)
            SignUpStep.CODE -> SignUpCodeStep(uiState = uiState, onIntent = onIntent)
            SignUpStep.PROFILE -> SignUpProfileStep(uiState = uiState, onIntent = onIntent)
        }
        Spacer(Modifier.height(SignUpBottomPadding))
    }
}

internal val SignUpWordmarkTopPadding = 16.dp
internal val SignUpWordmarkTitleSpacing = 40.dp
internal val SignUpTitleSubtitleSpacing = 30.dp
private val SignUpBottomPadding = 24.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpAccountContentPreview() {
    DesignSystemTheme {
        SignUpContent(uiState = SignUpUIState.empty, onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpCodeContentPreview() {
    DesignSystemTheme {
        SignUpContent(
            uiState = SignUpUIState.empty.copy(
                step = SignUpStep.CODE,
                email = "jinu@example.com",
                code = "427",
                resendRemainingSeconds = 272,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun SignUpProfileContentPreview() {
    DesignSystemTheme {
        SignUpContent(
            uiState = SignUpUIState.empty.copy(
                step = SignUpStep.PROFILE,
                nickname = "진우 집사",
            ),
            onIntent = {},
        )
    }
}
