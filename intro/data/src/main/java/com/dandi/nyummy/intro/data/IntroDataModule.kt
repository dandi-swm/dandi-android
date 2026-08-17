package com.dandi.nyummy.intro.data

import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.coroutine.IoDispatcher
import com.dandi.nyummy.intro.domain.IntroRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntroDataModule {

    @Provides
    @Singleton
    fun provideIntroApiService(retrofit: Retrofit): IntroApiService =
        retrofit.create(IntroApiService::class.java)

    @Provides
    @Singleton
    fun provideIntroDataSource(apiService: IntroApiService): IntroDataSource =
        IntroDataSource(apiService)

    @Provides
    @Singleton
    fun provideIntroRepository(
        dataSource: IntroDataSource,
        tokenProvider: TokenProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): IntroRepository = IntroRepositoryImpl(dataSource, tokenProvider, ioDispatcher)
}
