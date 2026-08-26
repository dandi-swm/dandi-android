package com.dandi.nyummy.common.data.preference

/**
 * 앱 전역 로컬 환경설정 저장소 계약.
 *
 * 특정 feature 소유가 아닌 cross-cutting 플래그(권한 안내 노출 여부 등)를 담으므로
 * TokenProvider 와 같이 common/data 가 소유한다. feature 는 자기 Repository 를 통해 읽는다.
 */
interface AppPreferenceProvider {
    /** 권한 안내(접근권한 고지)를 이미 노출했는지. */
    suspend fun hasShownPermissionNotice(): Boolean

    /** 권한 안내 노출 완료를 기록한다. */
    suspend fun markPermissionNoticeShown()
}
