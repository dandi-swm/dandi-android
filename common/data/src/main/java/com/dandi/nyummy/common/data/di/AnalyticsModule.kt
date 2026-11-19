package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.analytics.AirbridgeAnalyticsHelper
import com.dandi.nyummy.common.data.analytics.CompositeAnalyticsHelper
import com.dandi.nyummy.common.data.analytics.FirebaseAnalyticsHelper
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 분석 도구 바인딩. 모든 기록은 GA4(Firebase)로 가고, 핵심 전환만 Airbridge 로도 간다. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsHelper(): AnalyticsHelper = CompositeAnalyticsHelper(
        listOf(FirebaseAnalyticsHelper(Firebase.analytics), AirbridgeAnalyticsHelper()),
    )
}
