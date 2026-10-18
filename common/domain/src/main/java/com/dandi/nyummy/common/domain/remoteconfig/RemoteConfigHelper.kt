package com.dandi.nyummy.common.domain.remoteconfig

import com.dandi.nyummy.common.entity.remoteconfig.VersionCheckVO

/**
 * 원격 구성(Remote Config) 접근 헬퍼.
 *
 * 받아 오기([sync])와 값 조회를 나눈다. 앱 시작 때 인트로가 [sync]를 한 번 해 두면, 이후 버전 확인과
 * 기능 플래그([FeatureFlagHelper]) 조회는 받아 둔 값만 읽어서 조회가 늘어나도 네트워크 요청이 반복되지 않는다.
 *
 * 구현은 data 레이어에서 Firebase 원격 구성으로 처리하고, domain은 이 인터페이스만 의존해 버전 비교를 순수하게 둔다.
 */
interface RemoteConfigHelper {
    /**
     * 기본값을 깔고 서버 값을 받아 적용한다. 앱 시작 때 한 번 부른다.
     * 성공한 뒤 다시 부르면 아무것도 하지 않고, 실패해도 예외를 던지지 않는다(기본값으로 동작).
     */
    suspend fun sync()

    /** 받아 둔 값에서 버전 확인 값을 읽는다. 받아 오지 않는다. */
    fun getVersionCheck(): VersionCheckVO
}
