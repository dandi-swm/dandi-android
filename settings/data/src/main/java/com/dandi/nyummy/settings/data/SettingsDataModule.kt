package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.token.TokenProvider
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
    fun provideSettingsApiService(retrofit: Retrofit): SettingsApiService =
        retrofit.create(SettingsApiService::class.java)

    @Provides
    @Singleton
    fun provideSettingsRemoteDataSource(apiService: SettingsApiService): SettingsRemoteDataSource =
        SettingsRemoteDataSource(apiService)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        remoteDataSource: SettingsRemoteDataSource,
        tokenProvider: TokenProvider,
    ): SettingsRepository = SettingsRepositoryImpl(remoteDataSource, tokenProvider)
}
