package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.domain.CatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatDataModule {

    @Provides
    @Singleton
    fun provideCatApiService(retrofit: Retrofit): CatApiService =
        retrofit.create(CatApiService::class.java)

    @Provides
    @Singleton
    fun provideCatDataSource(apiService: CatApiService): CatDataSource =
        CatDataSource(apiService)

    @Provides
    @Singleton
    fun provideCatRepository(dataSource: CatDataSource): CatRepository =
        CatRepositoryImpl(dataSource)
}
