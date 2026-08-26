package com.dandi.nyummy.intro.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 앱 시작 부트스트랩 화면. [IntroViewModel] 이 시작 게이트를 수행해
 * 홈/로그인으로 루트 전환하며, 최초 1회 권한 안내가 필요할 때만 바텀시트를 띄운다.
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
    // 시스템 권한 팝업은 Activity 가 필요한 view 레이어 행위 — launcher 로 요청하고
    // 결과(허용/거부 무관)를 단일 진입점 onIntent 로 되돌린다.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onIntent(IntroIntent.OnPermissionsResult) }

    PermissionNoticeBottomSheet(
        visible = uiState.showPermissionNotice,
        onConfirm = {
            val permissions = startupManifestPermissions()
            if (permissions.isEmpty()) {
                onIntent(IntroIntent.OnPermissionsResult)
            } else {
                permissionLauncher.launch(permissions)
            }
        },
    )
}

/** 시작 권한의 Manifest 문자열 매핑. POST_NOTIFICATIONS 는 API 33+ 에만 존재한다. */
private fun startupManifestPermissions(): Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()
