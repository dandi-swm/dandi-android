package com.swm.dandi.history.data

import android.content.Context
import com.swm.dandi.history.domain.HistoryRepository
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
object HistoryDataModule {

    @Provides
    @Singleton
    fun provideHistoryRepository(dataSource: HistoryDataSource): HistoryRepository =
        HistoryRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideHistoryDataSource(
        apiService: HistoryApiService,
        skeletonResponseLoader: HistorySkeletonResponseLoader,
    ): HistoryDataSource =
        HistoryDataSource(apiService, skeletonResponseLoader)

    @Provides
    @Singleton
    fun provideHistorySkeletonResponseLoader(
        @ApplicationContext context: Context,
        json: Json,
    ): HistorySkeletonResponseLoader =
        HistorySkeletonResponseLoader(context.assets, json)

    @Provides
    @Singleton
    fun provideHistoryApiService(retrofit: Retrofit): HistoryApiService =
        retrofit.create(HistoryApiService::class.java)
}
