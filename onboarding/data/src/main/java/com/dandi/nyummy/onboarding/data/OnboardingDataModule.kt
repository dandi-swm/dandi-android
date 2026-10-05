package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.onboarding.domain.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardingDataModule {

    // TODO(server): 고양이 API 가 배포되면 Retrofit 을 주입받아
    //  `retrofit.create(OnboardingApiService::class.java)` 로 교체하고 MockOnboardingApiService 를 지운다.
    @Provides
    @Singleton
    fun provideOnboardingApiService(json: Json): OnboardingApiService =
        MockOnboardingApiService(json)

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
