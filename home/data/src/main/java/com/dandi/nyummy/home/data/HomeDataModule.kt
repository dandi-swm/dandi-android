package com.dandi.nyummy.home.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.domain.coroutine.IoScope
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.home.domain.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {

    @Provides
    @Singleton
    fun provideHomeApiService(retrofit: Retrofit): HomeApiService =
        retrofit.create(HomeApiService::class.java)

    @Provides
    @Singleton
    fun provideHomeDataSource(apiService: HomeApiService): HomeDataSource =
        HomeDataSource(apiService)

    @Provides
    @Singleton
    fun provideHomeRepository(
        dataSource: HomeDataSource,
        @IoScope ioScope: CoroutineScope,
        remoteConfigHelper: RemoteConfigHelper,
        appPreferenceProvider: AppPreferenceProvider,
        json: Json,
    ): HomeRepository = HomeRepositoryImpl(dataSource, ioScope, remoteConfigHelper, appPreferenceProvider, json)
}
