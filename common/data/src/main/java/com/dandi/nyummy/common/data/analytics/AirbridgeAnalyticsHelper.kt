package com.dandi.nyummy.common.data.analytics

import co.ab180.airbridge.Airbridge
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper

/**
 * Airbridge 로 보내는 [AnalyticsHelper]. [toAirbridgeEvent]가 고른 핵심 전환만 보내고 나머지는 무시한다.
 * 화면 조회와 사용자 속성은 보내지 않는다. SDK 를 켜지 않은 빌드에서는 아무것도 하지 않는다.
 */
class AirbridgeAnalyticsHelper : AnalyticsHelper {

    override fun logScreenView(screenName: String) = Unit

    override fun logEvent(event: AnalyticsEvent) {
        if (!AirbridgeSdk.isInitialized) return
        val airbridgeEvent = event.toAirbridgeEvent() ?: return
        Airbridge.trackEvent(airbridgeEvent.category, airbridgeEvent.semanticAttributes)
    }
}
