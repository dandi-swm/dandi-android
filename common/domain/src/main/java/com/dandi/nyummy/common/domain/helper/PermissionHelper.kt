package com.dandi.nyummy.common.domain.helper

/**
 * 런타임 권한의 도메인 표현.
 *
 * Android `Manifest.permission` 문자열로의 매핑은 Android 를 아는 레이어(data 구현체,
 * presentation 의 launcher 매퍼)가 담당한다 — common/domain 은 순수 JVM 모듈이다.
 */
enum class AppPermission {
    /** 카메라 — 식단 사진 촬영. */
    CAMERA,

    /** 알림(POST_NOTIFICATIONS, API 33+) — 기록 리마인드 알림. */
    NOTIFICATION,
}

/**
 * 런타임 권한 허용 상태 조회 헬퍼.
 *
 * 조회 전용이다 — 시스템 권한 요청(팝업)은 Activity 가 필요한 view 레이어 행위이므로
 * Compose 의 launcher 가 담당하고, domain 은 이 인터페이스로 상태만 판단한다.
 */
interface PermissionHelper {
    /** [permission] 이 현재 허용돼 있는지 반환한다. */
    fun isGranted(permission: AppPermission): Boolean
}
