package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.onboarding.domain.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardingDataModule {

    @Provides
    @Singleton
    fun provideOnboardingApiService(retrofit: Retrofit): OnboardingApiService =
        retrofit.create(OnboardingApiService::class.java)

    @Provides
    @Singleton
    fun provideOnboardingDataSource(apiService: OnboardingApiService): OnboardingDataSource =
        OnboardingDataSource(apiService)

    @Provides
    @Singleton
    fun provideOnboardingRepository(
        dataSource: OnboardingDataSource,
        appPreferenceProvider: AppPreferenceProvider,
    ): OnboardingRepository = OnboardingRepositoryImpl(dataSource, appPreferenceProvider)
}
