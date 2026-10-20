package com.dandi.nyummy.common.domain.analytics

/**
 * 사용 분석(Firebase Analytics, GA4) 기록 헬퍼.
 *
 * 화면과 ViewModel은 이 인터페이스만 의존하고, 구현은 data 레이어에서 Firebase로 보낸다.
 * 수집은 release 빌드에서만 켜져 있어서 개발 중 기록은 콘솔에 쌓이지 않는다.
 */
interface AnalyticsHelper {

    /** 화면 하나가 보였다. [screenName]은 내비게이션 경로(예: `/meal/record`)이고, 개인 정보를 넣지 않는다. */
    fun logScreenView(screenName: String)

    /** 사용자 행동 하나를 남긴다. 이벤트 이름과 매개변수는 [AnalyticsEvent]에서만 정한다. */
    fun logEvent(event: AnalyticsEvent)
}
