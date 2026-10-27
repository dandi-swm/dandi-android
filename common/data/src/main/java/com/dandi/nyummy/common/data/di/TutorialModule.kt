package com.dandi.nyummy.common.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dandi.nyummy.common.data.tutorial.TutorialHelperImpl
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 첫 사용자 튜토리얼 진행 상태 헬퍼 바인딩. 앱 전역 환경설정 DataStore를 함께 쓴다. */
@Module
@InstallIn(SingletonComponent::class)
object TutorialModule {

    @Provides
    @Singleton
    fun provideTutorialHelper(
        @AppPreferenceDataStore dataStore: DataStore<Preferences>,
    ): TutorialHelper = TutorialHelperImpl(dataStore)
}
