package com.dandi.nyummy.common.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.domain.helper.PermissionHelper

/**
 * [ContextCompat.checkSelfPermission] 기반 [PermissionHelper] 구현.
 *
 * NOTIFICATION(POST_NOTIFICATIONS)은 API 33+ 에서만 존재하는 런타임 권한이므로
 * 그 미만 버전에서는 항상 허용으로 취급한다.
 */
class PermissionHelperImpl(
    private val context: Context,
) : PermissionHelper {

    override fun isGranted(permission: AppPermission): Boolean = when (permission) {
        AppPermission.CAMERA -> isSelfGranted(Manifest.permission.CAMERA)
        AppPermission.NOTIFICATION ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                isSelfGranted(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun isSelfGranted(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
}
