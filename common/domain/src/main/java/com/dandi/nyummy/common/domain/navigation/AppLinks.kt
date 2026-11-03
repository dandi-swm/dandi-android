package com.dandi.nyummy.common.domain.navigation

/**
 * 앱 링크 주소. 서버가 주는 화면 링크(푸시, 로그인 redirectUrl, 상점 SDUI 액션)는 모두 이 주소로 시작한다.
 * 백엔드 앱 링크 `base-url`과 같고, 매니페스트 App Link intent-filter 호스트와도 같아야 한다.
 */
object AppLinks {
    const val HOST = "link.nyummy.co.kr"
    const val BASE_URL = "https://$HOST"
}
