package com.dandi.nyummy.history.data

import com.dandi.nyummy.history.domain.HistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HistoryDataModule {

    @Provides
    @Singleton
    fun provideHistoryApiService(retrofit: Retrofit): HistoryApiService =
        retrofit.create(HistoryApiService::class.java)

    @Provides
    @Singleton
    fun provideHistoryDataSource(apiService: HistoryApiService): HistoryDataSource =
        HistoryDataSource(apiService)

    @Provides
    @Singleton
    fun provideHistoryRepository(dataSource: HistoryDataSource): HistoryRepository =
        HistoryRepositoryImpl(dataSource)
}
