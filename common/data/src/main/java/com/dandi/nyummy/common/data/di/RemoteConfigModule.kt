package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.remoteconfig.FirebaseRemoteConfigHelper
import com.dandi.nyummy.common.data.remoteconfig.RemoteConfigEnv
import com.dandi.nyummy.common.domain.buildtype.ReleaseBuild
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Firebase 원격 구성 바인딩. 값은 앱 시작 때 인트로가 한 번 받아 두고([RemoteConfigHelper.sync]),
 * 강제 업데이트와 기능 플래그가 같은 값을 읽는다. 어느 키를 읽을지는 빌드 환경([RemoteConfigEnv])이 정한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    @Provides
    @Singleton
    fun provideRemoteConfigEnv(@ReleaseBuild isReleaseBuild: Boolean): RemoteConfigEnv =
        RemoteConfigEnv.of(isReleaseBuild)

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(env: RemoteConfigEnv): FirebaseRemoteConfig = Firebase.remoteConfig.apply {
        setConfigSettingsAsync(
            remoteConfigSettings {
                // 개발 빌드는 바꾼 값을 바로 확인하고, release는 1시간 캐시한다.
                minimumFetchIntervalInSeconds = if (env == RemoteConfigEnv.RELEASE) 3600L else 0L
            },
        )
    }

    @Provides
    @Singleton
    fun provideRemoteConfigHelper(
        remoteConfig: FirebaseRemoteConfig,
        json: Json,
        env: RemoteConfigEnv,
    ): RemoteConfigHelper = FirebaseRemoteConfigHelper(remoteConfig, json, env)
}
