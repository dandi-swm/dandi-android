package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.intro.entity.VersionCheckVO

/**
 * RemoteConfig(원격 구성) 접근 헬퍼. 앱 시작 시점의 버전 게이트 정보를 제공한다.
 *
 * 구현은 data 레이어에서 Firebase RemoteConfig 로 처리하며, domain 은 이 인터페이스만 의존해
 * 버전 비교 로직을 순수하게 유지한다.
 */
interface RemoteConfigHelper {
    /** RemoteConfig 를 fetch/activate 해 버전 체크 값을 조회한다. */
    suspend fun checkVersion(): VersionCheckVO
}
