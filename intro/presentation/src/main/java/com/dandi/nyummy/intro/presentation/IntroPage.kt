package com.dandi.nyummy.intro.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester

/**
 * 앱 시작 부트스트랩 화면. 시작 게이트가 도는 동안 스플래시([IntroSplashContent])를
 * 보여주고, [IntroViewModel] 이 완료되면 홈/로그인으로 루트 전환한다.
 * 최초 1회 권한 안내가 필요하면 스플래시 위에 바텀시트를 오버레이하고, 확인하면 시스템 권한 팝업을 띄운다.
 */
@Composable
fun IntroPage(
    viewModel: IntroViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionRequester = rememberPermissionRequester { viewModel.onIntent(IntroIntent.PermissionRequestFinished) }
    // 연타해도 시스템 팝업이 한 번만 뜨도록 버튼이 아니라 상태가 바뀔 때 이 한 곳에서 띄운다.
    LaunchedEffect(uiState.isRequestingPermissions) {
        if (uiState.isRequestingPermissions) permissionRequester.request(uiState.requestedPermissions)
    }
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
