package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.intro.entity.VersionCheckVO

/**
 * RemoteConfig(원격 구성) 접근 헬퍼.
 *
 * fetch/activate([sync])와 값 조회를 분리한다 — 앱 시작 시 [sync] 를 1회 수행해 두면,
 * 이후 버전 체크·기능 flag 등 어떤 값을 조회하든 활성화된 스냅샷만 읽으므로
 * 조회 메서드가 늘어나도 네트워크 fetch 가 반복되지 않는다.
 *
 * 구현은 data 레이어에서 Firebase RemoteConfig 로 처리하며, domain 은 이 인터페이스만 의존해
 * 버전 비교 로직을 순수하게 유지한다.
 */
interface RemoteConfigHelper {
    /**
     * 기본값을 깔고 서버 값을 fetch/activate 한다. 앱 시작 시 1회 호출.
     * 성공 후 재호출은 no-op 이며, 실패해도 throw 하지 않는다(기본값으로 동작).
     */
    suspend fun sync()

    /** 활성화된 스냅샷에서 버전 체크 값을 읽는다. fetch 하지 않는다. */
    fun getVersionCheck(): VersionCheckVO
}
