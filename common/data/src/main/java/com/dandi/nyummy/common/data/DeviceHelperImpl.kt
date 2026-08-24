package com.dandi.nyummy.common.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.dandi.nyummy.common.domain.helper.DeviceHelper

/**
 * PackageManager 로 현재 설치된 앱/기기 정보를 읽는 [DeviceHelper] 구현.
 *
 * `PackageInfo.longVersionCode` 는 API 28+ 이므로, minSdk 24 대응을 위해
 * [PackageInfoCompat.getLongVersionCode] 로 읽는다.
 */
class DeviceHelperImpl(
    private val context: Context,
) : DeviceHelper {

    private val packageInfo by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    override val appVersionCode: Long
        get() = PackageInfoCompat.getLongVersionCode(packageInfo)

    override val appVersionName: String
        get() = packageInfo.versionName.orEmpty()
}
