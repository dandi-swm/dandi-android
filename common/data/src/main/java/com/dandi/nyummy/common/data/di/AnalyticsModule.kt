package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.analytics.FirebaseAnalyticsHelper
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Firebase Analytics 바인딩. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsHelper(): AnalyticsHelper = FirebaseAnalyticsHelper(Firebase.analytics)
}
