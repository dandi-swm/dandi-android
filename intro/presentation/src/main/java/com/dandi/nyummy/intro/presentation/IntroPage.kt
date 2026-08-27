package com.dandi.nyummy.intro.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester

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
    // 허용 여부는 보지 않는다 — 시작 게이트는 비차단이라 결과 도착 자체가 신호다.
    val permissionRequester = rememberPermissionRequester {
        onIntent(IntroIntent.OnPermissionsResult)
    }

    IntroSplashContent(
        isComplete = uiState.isSplashComplete,
        isPermissionNoticeVisible = uiState.showPermissionNotice,
    )

    PermissionNoticeBottomSheet(
        visible = uiState.showPermissionNotice,
        onConfirm = { permissionRequester.request(uiState.pendingPermissions) },
    )
}
