package com.dandi.nyummy.intro.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.intro.domain.RemoteConfigHelper
import com.dandi.nyummy.intro.entity.VersionCheckVO
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

/**
 * Firebase RemoteConfig 기반 [RemoteConfigHelper] 구현.
 *
 * 빌드가 debuggable(개발/QA)이면 `_qa` 키를, 아니면(릴리스) 운영 키를 읽는다.
 * `BuildConfig.DEBUG` 대신 [ApplicationInfo.FLAG_DEBUGGABLE] 을 쓰는 이유는 JankModule 과 동일하다.
 *
 * 기본값(defaults)으로 **현재 앱 버전**을 깔아두므로, fetch 가 실패해도(오프라인 등)
 * "최소 버전 = 현재 버전"이 되어 강제 업데이트가 걸리지 않는다(안전). 서버 값이 있으면 그 위에 덮인다.
 */
class RemoteConfigHelperImpl(
    private val remoteConfig: FirebaseRemoteConfig,
    private val deviceHelper: DeviceHelper,
    context: Context,
) : RemoteConfigHelper {

    private val isDebuggable =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override suspend fun checkVersion(): VersionCheckVO {
        runCatching {
            // 기본값을 먼저 깔고(현재 앱 버전 기준), 그 위에 서버 값을 fetch 해 덮는다.
            remoteConfig.setDefaultsAsync(buildDefaults()).await()
            remoteConfig.fetchAndActivate().await()
        }

        return VersionCheckVO(
            minimumVersion = remoteConfig.getString(key(KEY_MINIMUM_VERSION)),
            minimumVersionCode = remoteConfig.getLong(key(KEY_MINIMUM_VERSION_CODE)),
            latestVersionUpdateLink = remoteConfig.getString(key(KEY_LATEST_VERSION_UPDATE_LINK)),
            minimumVersionReleaseNote = remoteConfig.getString(key(KEY_MINIMUM_VERSION_RELEASE_NOTE)),
        )
    }

    /**
     * fetch 이전/실패 시 사용할 기본값. minimum 버전을 현재 앱 버전으로 두어
     * 값이 없으면 강제 업데이트가 걸리지 않도록(안전) 한다.
     */
    private fun buildDefaults(): Map<String, Any> = mapOf(
        key(KEY_MINIMUM_VERSION) to deviceHelper.appVersionName,
        key(KEY_MINIMUM_VERSION_CODE) to deviceHelper.appVersionCode,
        //TODO: 추후 스토어 링크를 넣어야 한다. 현재는 앱스토어 링크가 없으므로 빈 문자열로 둔다.
        key(KEY_LATEST_VERSION_UPDATE_LINK) to "",
        key(KEY_MINIMUM_VERSION_RELEASE_NOTE) to "",
    )

    /** debuggable 빌드는 `_qa` 접미 키(QA용), 릴리스는 운영 키를 사용한다. */
    private fun key(base: String): String = if (isDebuggable) "${base}_qa" else base

    companion object {
        private const val KEY_MINIMUM_VERSION = "android_minimum_version"
        private const val KEY_MINIMUM_VERSION_CODE = "android_minimum_version_code"
        private const val KEY_LATEST_VERSION_UPDATE_LINK = "android_latest_version_update_link"
        private const val KEY_MINIMUM_VERSION_RELEASE_NOTE = "android_minimum_version_release_note"
    }
}
