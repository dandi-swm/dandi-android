package com.dandi.nyummy.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dandi.nyummy.common.data.di.AppPreferenceDataStore
import com.dandi.nyummy.settings.domain.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataModule {

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
        localDataSource: SettingsLocalDataSource,
        remoteDataSource: SettingsRemoteDataSource,
    ): SettingsRepository = SettingsRepositoryImpl(localDataSource, remoteDataSource)
}
