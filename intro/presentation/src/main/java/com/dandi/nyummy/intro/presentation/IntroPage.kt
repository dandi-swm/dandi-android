package com.dandi.nyummy.intro.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 앱 시작 부트스트랩 화면. 시작 게이트가 도는 동안 스플래시([IntroSplashContent])를
 * 보여주고, [IntroViewModel] 이 완료되면 홈/로그인으로 루트 전환한다.
 * 최초 1회 권한 안내가 필요하면 스플래시 위에 바텀시트를 오버레이한다.
 */
@Composable
fun IntroPage(
    viewModel: IntroViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IntroScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun IntroScreen(
    uiState: IntroUIState,
    onIntent: (IntroIntent) -> Unit,
) {
    IntroSplashContent(
        isComplete = uiState.isSplashComplete,
        isPermissionNoticeVisible = uiState.showPermissionNotice,
    )

    PermissionNoticeBottomSheet(
        visible = uiState.showPermissionNotice,
        onConfirm = { onIntent(IntroIntent.ConfirmPermissionNotice) },
    )
}
