package com.dandi.nyummy.common.domain.navigation

/** 링크(알림 등)로 화면을 열 때 [NavRoute.args]에 함께 싣는 공통 인자. */
object LinkArgs {
    /**
     * 화면을 연 알림의 식별자. 서버 푸시 data의 `notificationId`와 이름이 같다(백그라운드 알림은 이 이름으로 extras에 온다).
     * 같은 링크라도 알림마다 다른 라우트 키가 되어 화면이 새 이벤트로 다시 그려지고, 화면은 이 값으로 같은 알림을 한 번만 처리한다.
     */
    const val NOTIFICATION_ID = "notificationId"
}
