package com.dandi.nyummy.common.domain.featureflag

/**
 * 기능 플래그 조회.
 *
 * 앱 시작 때 인트로가 원격 구성을 한 번 받아 두면(`RemoteConfigHelper.sync`), 여기서는 그 값만 읽고 네트워크 요청은 하지 않는다.
 */
interface FeatureFlagHelper {
    fun isEnabled(flag: FeatureFlag): Boolean
}
