package com.dandi.nyummy.common.data.analytics

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper

/** 같은 기록을 여러 분석 도구(GA4, Airbridge)에 차례로 넘기는 [AnalyticsHelper]. */
class CompositeAnalyticsHelper(
    private val helpers: List<AnalyticsHelper>,
) : AnalyticsHelper {

    override fun logScreenView(screenName: String) = helpers.forEach { it.logScreenView(screenName) }

    override fun logEvent(event: AnalyticsEvent) = helpers.forEach { it.logEvent(event) }

    override fun setUserProperty(name: String, value: String?) = helpers.forEach { it.setUserProperty(name, value) }
}
