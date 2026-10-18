package com.dandi.nyummy.common.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.dandi.nyummy.common.data.featureflag.FirebaseFeatureFlagHelper
import com.dandi.nyummy.common.domain.featureflag.FeatureFlagHelper
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase 원격 구성과 기능 플래그 바인딩.
 * 값은 앱 시작 때 인트로가 한 번 받아 두고(`RemoteConfigHelper.sync`), 버전 확인과 기능 플래그가 같은 값을 읽는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(
        @ApplicationContext context: Context,
    ): FirebaseRemoteConfig = Firebase.remoteConfig.apply {
        setConfigSettingsAsync(
            remoteConfigSettings {
                // debug/QA 는 즉시 최신값을 확인, 릴리스는 1시간 캐시.
                minimumFetchIntervalInSeconds = if (context.isDebuggable()) 0L else 3600L
            },
        )
    }

    @Provides
    @Singleton
    fun provideFeatureFlagHelper(
        remoteConfig: FirebaseRemoteConfig,
        @ApplicationContext context: Context,
    ): FeatureFlagHelper = FirebaseFeatureFlagHelper(
        readBoolean = remoteConfig::getBoolean,
        isDebuggable = context.isDebuggable(),
    )

    private fun Context.isDebuggable(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
