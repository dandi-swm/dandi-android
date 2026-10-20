package com.dandi.nyummy.common.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {

    /** 모든 이벤트 하나씩. 이벤트를 추가하면 여기에도 넣는다. */
    private val allEvents = listOf(
        AnalyticsEvent.Login(method = "kakao"),
        AnalyticsEvent.SignUpStep(step = "account", method = "email"),
        AnalyticsEvent.SignUp(method = "email"),
        AnalyticsEvent.OnboardingStep(scene = 6, kind = "name_input"),
        AnalyticsEvent.OnboardingComplete,
    )

    @Test
    fun `이벤트 이름과 매개변수 키는 GA4 규칙(영문 소문자로 시작, 소문자 숫자 밑줄, 40자 안)을 지킨다`() {
        allEvents.forEach { event ->
            assertTrue(event.name, GaName.matches(event.name))
            event.params.keys.forEach { key -> assertTrue("${event.name}.$key", GaName.matches(key)) }
        }
    }

    @Test
    fun `매개변수 값은 100자를 넘지 않는다`() {
        allEvents.forEach { event ->
            event.params.forEach { (key, value) -> assertTrue("${event.name}.$key", value.length <= 100) }
        }
    }

    @Test
    fun `이벤트 이름이 서로 겹치지 않는다`() {
        assertEquals(allEvents.size, allEvents.map { it.name }.toSet().size)
    }

    @Test
    fun `로그인과 가입은 GA4 권장 이벤트 이름과 method 매개변수를 쓴다`() {
        assertEquals("login" to mapOf("method" to "kakao"), AnalyticsEvent.Login("kakao").let { it.name to it.params })
        assertEquals("sign_up" to mapOf("method" to "email"), AnalyticsEvent.SignUp("email").let { it.name to it.params })
    }

    private companion object {
        val GaName = Regex("^[a-z][a-z0-9_]{0,39}$")
    }
}
