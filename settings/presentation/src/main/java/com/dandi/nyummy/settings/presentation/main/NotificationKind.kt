package com.dandi.nyummy.settings.presentation.main

/** 알림 수신 설정의 두 묶음. 토글 저장과 "자세히 보기" 시트가 함께 쓴다. */
enum class NotificationKind {
    /** 서비스 알림: 식사 시간 알림, 분석 결과 알림 */
    SERVICE,

    /** 마케팅 알림: 기록을 쉴 때 알림. 광고성 정보라 동의해야 받는다. */
    MARKETING,
}
