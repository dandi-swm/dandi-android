package com.dandi.nyummy.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme

/**
 * 로그인 랜딩 화면
 *
 * 소셜 로그인(카카오·네이버·구글)은 MVP 에서 UI 만 제공하며 동작하지 않는다.
 * 이메일 원형 버튼만 이메일 로그인 화면으로 이동한다.
 */
@Composable
fun LoginPage(
    viewModel: LoginViewModel = hiltViewModel<LoginViewModel>(),
) {
    LoginPageContent()
}

@Composable
private fun LoginPageContent() {

}


@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPagePreview() {
    DesignSystemTheme {
        LoginPageContent()
    }
}
