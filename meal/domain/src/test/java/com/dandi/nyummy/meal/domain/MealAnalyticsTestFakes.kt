package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper

/** 남긴 분석 기록을 모아 둔다. */
internal class RecordingAnalyticsHelper : AnalyticsHelper {
    val events = mutableListOf<AnalyticsEvent>()

    override fun logScreenView(screenName: String) = Unit

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }
}
