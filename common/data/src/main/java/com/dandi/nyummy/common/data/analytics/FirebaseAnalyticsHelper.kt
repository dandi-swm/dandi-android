package com.dandi.nyummy.common.data.analytics

import android.os.Bundle
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics 기반 [AnalyticsHelper]. 데이터는 Firebase 프로젝트에 연결된 GA4 속성에 쌓인다.
 * 수집 여부는 매니페스트(`firebase_analytics_collection_enabled`)가 빌드 종류로 정한다.
 */
class FirebaseAnalyticsHelper(
    private val analytics: FirebaseAnalytics,
) : AnalyticsHelper {

    override fun logScreenView(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                // 화면은 모두 한 액티비티 안의 Compose 경로라, 클래스 이름 대신 경로를 그대로 쓴다.
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            },
        )
    }

    override fun logEvent(event: AnalyticsEvent) {
        analytics.logEvent(
            event.name,
            Bundle().apply { event.params.forEach { (key, value) -> putString(key, value) } },
        )
    }
}
