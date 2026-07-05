package com.swm.dandi.common.data.mediaSearch

import com.swm.dandi.common.domain.media.MediaSearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaSearchDataModule {

    @Provides
    @Singleton
    fun provideMediaRepository(searchDataSource: MediaSearchDataSource): MediaSearchRepository =
        MediaSearchRepositoryImpl(searchDataSource)

    @Provides
    @Singleton
    fun provideKakaoSearchDataSource(apiService: MediaSearchApiService): MediaSearchDataSource =
        MediaSearchDataSource(apiService)

    @Provides
    @Singleton
    fun provideKakaoSearchApiService(retrofit: Retrofit): MediaSearchApiService =
        retrofit.create(MediaSearchApiService::class.java)
}
