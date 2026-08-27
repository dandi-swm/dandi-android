package com.dandi.nyummy.common.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.data.preference.AppPreferenceProviderImpl
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.token.TokenProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** 인증 토큰 전용 DataStore 파일("auth_tokens") 바인딩 qualifier. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthTokenDataStore

/** 앱 전역 환경설정 DataStore 파일("app_prefs") 바인딩 qualifier. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppPreferenceDataStore

private val Context.authTokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_tokens",
)

private val Context.appPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_prefs",
)

/** 로컬 저장소(DataStore Preferences) 공용 바인딩. 네트워크 생성은 [NetworkModule] 담당. */
@Module
@InstallIn(SingletonComponent::class)
object LocalStorageModule {

    @Provides
    @Singleton
    @AuthTokenDataStore
    fun provideAuthTokenDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.authTokenDataStore

    @Provides
    @Singleton
    fun provideTokenProvider(
        @AuthTokenDataStore dataStore: DataStore<Preferences>,
    ): TokenProvider = TokenProviderImpl(dataStore)

    @Provides
    @Singleton
    @AppPreferenceDataStore
    fun provideAppPreferenceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appPreferenceDataStore

    @Provides
    @Singleton
    fun provideAppPreferenceProvider(
        @AppPreferenceDataStore dataStore: DataStore<Preferences>,
    ): AppPreferenceProvider = AppPreferenceProviderImpl(dataStore)
}
