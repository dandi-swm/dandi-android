package com.dandi.nyummy.common.data.featureflag

import com.dandi.nyummy.common.domain.featureflag.FeatureFlag
import com.dandi.nyummy.common.domain.featureflag.FeatureFlagHelper

/**
 * Firebase 원격 구성 기반 [FeatureFlagHelper].
 *
 * debuggable 빌드는 `_qa` 키를 읽어 QA에서만 먼저 켜 볼 수 있게 한다(`RemoteConfigHelperImpl`과 같은 규칙).
 * 원격 구성은 없는 키를 false로 돌려주므로 받지 못했을 때는 꺼진 채로 동작한다.
 *
 * @param readBoolean 활성화된 원격 구성 값을 읽는다. 실제 앱에서는 `FirebaseRemoteConfig.getBoolean`이다.
 */
class FirebaseFeatureFlagHelper(
    private val readBoolean: (key: String) -> Boolean,
    private val isDebuggable: Boolean,
) : FeatureFlagHelper {

    override fun isEnabled(flag: FeatureFlag): Boolean = readBoolean(keyOf(flag))

    private fun keyOf(flag: FeatureFlag): String = if (isDebuggable) "${flag.key}$QA_SUFFIX" else flag.key

    private companion object {
        const val QA_SUFFIX = "_qa"
    }
}
