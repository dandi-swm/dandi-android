package com.dandi.nyummy.intro.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.coroutine.IoDispatcher
import com.dandi.nyummy.intro.domain.IntroRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntroDataModule {

    @Provides
    @Singleton
    fun provideIntroRepository(
        tokenProvider: TokenProvider,
        appPreferenceProvider: AppPreferenceProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): IntroRepository = IntroRepositoryImpl(tokenProvider, appPreferenceProvider, ioDispatcher)
}
