package com.dandi.nyummy.common.data.remoteconfig

import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.remoteconfig.FeatureFlag
import com.dandi.nyummy.common.domain.remoteconfig.FeatureFlagHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.entity.remoteconfig.VersionCheckVO
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Firebase 원격 구성 기반 [RemoteConfigHelper]와 [FeatureFlagHelper] 구현.
 *
 * 키는 [env]에 따라 고른다(release 빌드는 운영 키, 그 밖의 빌드는 `_debug` 키).
 *
 * 버전 확인 기본값으로 **현재 앱 버전**을 깔아 두므로, 받아 오지 못해도(오프라인 등)
 * "최소 버전 = 현재 버전"이 되어 강제 업데이트가 걸리지 않는다(안전). 서버 값이 있으면 그 위에 덮인다.
 * 기능 플래그는 기본값을 따로 두지 않는다. 원격 구성은 없는 키를 false로 돌려주므로 받지 못하면 꺼진 채로 동작한다.
 */
class FirebaseRemoteConfigHelper(
    private val remoteConfig: FirebaseRemoteConfig,
    private val deviceHelper: DeviceHelper,
    private val env: RemoteConfigEnv,
) : RemoteConfigHelper, FeatureFlagHelper {

    private val syncMutex = Mutex()

    private var isSynced = false

    override suspend fun sync() {
        syncMutex.withLock {
            if (isSynced) return
            runCatching {
                // 기본값을 먼저 깔고(현재 앱 버전 기준), 그 위에 서버 값을 받아 덮는다.
                remoteConfig.setDefaultsAsync(buildDefaults()).await()
                remoteConfig.fetchAndActivate().await()
            }.onSuccess { isSynced = true } // 실패하면 다음 sync() 호출에서 다시 시도한다.
        }
    }

    override fun getVersionCheck(): VersionCheckVO = VersionCheckVO(
        minimumVersion = remoteConfig.getString(env.keyOf(KEY_MINIMUM_VERSION)),
        minimumVersionCode = remoteConfig.getLong(env.keyOf(KEY_MINIMUM_VERSION_CODE)),
        latestVersionUpdateLink = remoteConfig.getString(env.keyOf(KEY_LATEST_VERSION_UPDATE_LINK)),
        minimumVersionReleaseNote = remoteConfig.getString(env.keyOf(KEY_MINIMUM_VERSION_RELEASE_NOTE)),
    )

    override fun isEnabled(flag: FeatureFlag): Boolean = remoteConfig.getBoolean(env.keyOf(flag.key))

    /**
     * 받아 오기 전이나 실패했을 때 쓰는 기본값. 최소 버전을 현재 앱 버전으로 두어
     * 값이 없으면 강제 업데이트가 걸리지 않게(안전) 한다.
     */
    private fun buildDefaults(): Map<String, Any> = mapOf(
        env.keyOf(KEY_MINIMUM_VERSION) to deviceHelper.appVersionName,
        env.keyOf(KEY_MINIMUM_VERSION_CODE) to deviceHelper.appVersionCode,
        // TODO: 추후 스토어 링크를 넣어야 한다. 현재는 앱스토어 링크가 없으므로 빈 문자열로 둔다.
        env.keyOf(KEY_LATEST_VERSION_UPDATE_LINK) to "",
        env.keyOf(KEY_MINIMUM_VERSION_RELEASE_NOTE) to "",
    )

    private companion object {
        const val KEY_MINIMUM_VERSION = "android_minimum_version"
        const val KEY_MINIMUM_VERSION_CODE = "android_minimum_version_code"
        const val KEY_LATEST_VERSION_UPDATE_LINK = "android_latest_version_update_link"
        const val KEY_MINIMUM_VERSION_RELEASE_NOTE = "android_minimum_version_release_note"
    }
}
