package com.dandi.nyummy.common.data.analytics

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import org.junit.Assert.assertEquals
import org.junit.Test

/** 같은 기록이 모든 분석 도구에 차례로 넘어가는지 확인합니다. */
class CompositeAnalyticsHelperTest {

    private class RecordingHelper(private val name: String, private val log: MutableList<String>) : AnalyticsHelper {
        override fun logScreenView(screenName: String) { log += "$name:screen:$screenName" }
        override fun logEvent(event: AnalyticsEvent) { log += "$name:event:${event.name}" }
        override fun setUserProperty(name: String, value: String?) { log += "${this.name}:prop:$name=$value" }
    }

    private val log = mutableListOf<String>()
    private val helper = CompositeAnalyticsHelper(listOf(RecordingHelper("ga", log), RecordingHelper("ab", log)))

    @Test
    fun `화면 기록을 모든 도구에 넘긴다`() {
        helper.logScreenView("/home")
        assertEquals(listOf("ga:screen:/home", "ab:screen:/home"), log)
    }

    @Test
    fun `이벤트를 모든 도구에 넘긴다`() {
        helper.logEvent(AnalyticsEvent.OnboardingComplete)
        assertEquals(listOf("ga:event:onboarding_complete", "ab:event:onboarding_complete"), log)
    }

    @Test
    fun `사용자 속성을 모든 도구에 넘긴다`() {
        helper.setUserProperty("group", null)
        assertEquals(listOf("ga:prop:group=null", "ab:prop:group=null"), log)
    }
}
