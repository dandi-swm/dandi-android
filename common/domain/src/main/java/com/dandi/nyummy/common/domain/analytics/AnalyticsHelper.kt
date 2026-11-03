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

    /**
     * 사용자 속성 하나를 남긴다. 이후 이 기기의 이벤트에 함께 붙어 GA4에서 이 값으로 사용자를 나눠 볼 수 있다(A/B 그룹 등).
     * 이름은 [UserProperty]에서만 정한다. 값은 36자 안이고 개인 정보는 넣지 않는다. null이면 지운다.
     * 기본 구현은 아무것도 하지 않는다(사용자 속성을 보지 않는 테스트 대역용).
     */
    fun setUserProperty(name: String, value: String?) = Unit

    /** 사용자 속성 이름. GA4 규칙상 영문 소문자와 밑줄로 24자 안이다. */
    object UserProperty {
        /** 코인 충전 이벤트에서 이 기기가 받은 A/B 그룹(원격 구성 값의 `group`, 예: A, B). */
        const val COIN_CHARGE_GROUP = "coin_charge_group"

        /** 상점 추천 탭 배치 A/B 그룹(원격 구성 `android_shop_layout_variant`, A 또는 B). */
        const val SHOP_LAYOUT_GROUP = "shop_layout_group"
    }
}
