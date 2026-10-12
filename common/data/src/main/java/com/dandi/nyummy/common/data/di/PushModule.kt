package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.push.DeviceTokenApiService
import com.dandi.nyummy.common.data.push.DeviceTokenDataSource
import com.dandi.nyummy.common.data.push.PushTokenHelperImpl
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.coroutine.IoScope
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import retrofit2.Retrofit
import javax.inject.Singleton

/** 푸시 토큰 등록, 삭제 바인딩. 앱 시작, 로그인, 토큰 갱신, 로그아웃에서 같이 쓴다. */
@Module
@InstallIn(SingletonComponent::class)
object PushModule {

    @Provides
    @Singleton
    fun provideDeviceTokenApiService(retrofit: Retrofit): DeviceTokenApiService =
        retrofit.create(DeviceTokenApiService::class.java)

    @Provides
    @Singleton
    fun provideDeviceTokenDataSource(apiService: DeviceTokenApiService): DeviceTokenDataSource =
        DeviceTokenDataSource(apiService)

    @Provides
    @Singleton
    fun providePushTokenHelper(
        dataSource: DeviceTokenDataSource,
        tokenProvider: TokenProvider,
        @IoScope ioScope: CoroutineScope,
    ): PushTokenHelper = PushTokenHelperImpl(dataSource, tokenProvider, ioScope)
}
