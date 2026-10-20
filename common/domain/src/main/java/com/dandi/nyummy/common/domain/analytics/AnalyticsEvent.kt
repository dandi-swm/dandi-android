package com.dandi.nyummy.common.domain.analytics

/**
 * 분석 이벤트 하나. 이름과 매개변수를 이 파일에서만 정해 화면마다 철자가 달라지지 않게 한다.
 *
 * GA4 규칙: 이름과 매개변수 키는 영문 소문자와 밑줄로 40자 안, 값은 100자 안이다. 개인 정보(이메일, 닉네임 등)는 넣지 않는다.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)
