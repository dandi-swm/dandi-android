package com.dandi.nyummy.intro.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.coroutine.IoDispatcher
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.intro.domain.IntroRepository
import com.dandi.nyummy.intro.domain.RemoteConfigHelper
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntroDataModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(
        @ApplicationContext context: Context,
    ): FirebaseRemoteConfig {
        val isDebuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    // debug/QA 는 즉시 최신값을 확인, 릴리스는 1시간 캐시.
                    minimumFetchIntervalInSeconds = if (isDebuggable) 0L else 3600L
                },
            )
        }
    }

    @Provides
    @Singleton
    fun provideRemoteConfigHelper(
        remoteConfig: FirebaseRemoteConfig,
        deviceHelper: DeviceHelper,
        @ApplicationContext context: Context,
    ): RemoteConfigHelper = RemoteConfigHelperImpl(remoteConfig, deviceHelper, context)

    @Provides
    @Singleton
    fun provideIntroRepository(
        tokenProvider: TokenProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): IntroRepository = IntroRepositoryImpl(tokenProvider, ioDispatcher)
}
