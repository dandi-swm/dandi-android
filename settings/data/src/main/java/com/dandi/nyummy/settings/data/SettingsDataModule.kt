package com.dandi.nyummy.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dandi.nyummy.common.data.di.AppPreferenceDataStore
import com.dandi.nyummy.settings.domain.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataModule {

    // TODO(server): 알림 설정 API가 생기면 Retrofit ApiService를 만들어 NotificationSettingsDataSource에 넘긴다.
    @Provides
    @Singleton
    fun provideNotificationSettingsDataSource(
        @ApplicationContext context: Context,
        json: Json,
    ): NotificationSettingsDataSource = NotificationSettingsDataSource(context, json)

    @Provides
    @Singleton
    fun provideSettingsLocalDataSource(
        @AppPreferenceDataStore dataStore: DataStore<Preferences>,
    ): SettingsLocalDataSource = SettingsLocalDataSource(dataStore)

    @Provides
    @Singleton
    fun provideSettingsApiService(retrofit: Retrofit): SettingsApiService =
        retrofit.create(SettingsApiService::class.java)

    @Provides
    @Singleton
    fun provideSettingsRemoteDataSource(apiService: SettingsApiService): SettingsRemoteDataSource =
        SettingsRemoteDataSource(apiService)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        notificationDataSource: NotificationSettingsDataSource,
        localDataSource: SettingsLocalDataSource,
        remoteDataSource: SettingsRemoteDataSource,
    ): SettingsRepository = SettingsRepositoryImpl(notificationDataSource, localDataSource, remoteDataSource)
}
