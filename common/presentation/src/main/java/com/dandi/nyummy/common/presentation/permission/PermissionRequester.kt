package com.dandi.nyummy.common.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.dandi.nyummy.common.domain.helper.AppPermission

/**
 * [AppPermission] 기반 시스템 권한 요청기.
 *
 * 상태 "조회"는 domain 의 PermissionHelper, 시스템 팝업 "요청"은 Activity 가 필요한
 * view 레이어 행위라 이 요청기가 담당한다. Manifest 문자열 매핑·API 레벨 분기·
 * launcher 보일러플레이트를 한 곳에 모아, 화면(인트로 시작 권한·식사 기록 카메라 등)은
 * 도메인 enum 만으로 요청하고 결과 콜백만 처리한다.
 */
@Stable
class PermissionRequester internal constructor(
    private val onRequest: (List<AppPermission>) -> Unit,
) {
    /**
     * [permissions] 를 시스템 팝업으로 일괄 요청한다.
     * 이 기기에서 요청이 불필요한 권한(하위 API 의 알림 등)만 있으면 팝업 없이 즉시 결과가 온다.
     */
    fun request(permissions: List<AppPermission>) {
        onRequest(permissions)
    }
}

/**
 * [PermissionRequester] 를 생성한다. [onResult] 는 요청한 권한별 허용 여부를 받는다
 * (요청이 불필요했던 권한은 허용으로 취급).
 */
@Composable
fun rememberPermissionRequester(
    onResult: (Map<AppPermission, Boolean>) -> Unit,
): PermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    var requested by remember { mutableStateOf<List<AppPermission>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        currentOnResult(
            requested.associateWith { permission ->
                permission.manifestNameOrNull()?.let { grantResults[it] == true } ?: true
            },
        )
    }

    return remember {
        PermissionRequester { permissions ->
            requested = permissions
            val manifestNames = permissions.mapNotNull { it.manifestNameOrNull() }
            if (manifestNames.isEmpty()) {
                currentOnResult(permissions.associateWith { true })
            } else {
                launcher.launch(manifestNames.toTypedArray())
            }
        }
    }
}

/** 이 기기에서 요청해야 하는 Manifest 권한명. 요청이 불필요하면(하위 API 등) null. */
private fun AppPermission.manifestNameOrNull(): String? = when (this) {
    AppPermission.CAMERA -> Manifest.permission.CAMERA
    AppPermission.NOTIFICATION ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
}
