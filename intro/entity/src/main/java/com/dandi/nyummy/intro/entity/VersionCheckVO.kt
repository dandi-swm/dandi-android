package com.dandi.nyummy.intro.entity

/**
 * RemoteConfig 기반 앱 버전 체크 결과.
 *
 * @property minimumVersion 최소 요구 버전 이름 (표시용, 예: "1.2.0")
 * @property minimumVersionCode 최소 요구 버전 코드 — 현재 앱 versionCode 가 미만이면 강제 업데이트
 * @property latestVersionUpdateLink 업데이트 유도 시 이동할 스토어/웹 링크
 * @property minimumVersionReleaseNote 강제 업데이트 다이얼로그에 노출할 안내 문구
 */
data class VersionCheckVO(
    val minimumVersion: String = "",
    val minimumVersionCode: Long = 0L,
    val latestVersionUpdateLink: String = "",
    val minimumVersionReleaseNote: String = "",
) {
    companion object {
        val empty: VersionCheckVO = VersionCheckVO()
    }
}
