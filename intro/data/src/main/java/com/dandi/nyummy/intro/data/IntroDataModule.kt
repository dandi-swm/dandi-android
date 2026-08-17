package com.dandi.nyummy.intro.data

import com.dandi.nyummy.intro.domain.IntroRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideIntroRepository(dataSource: IntroDataSource): IntroRepository =
        IntroRepositoryImpl(dataSource)
}
