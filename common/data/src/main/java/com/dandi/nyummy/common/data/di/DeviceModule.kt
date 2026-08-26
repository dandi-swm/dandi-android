package com.dandi.nyummy.common.data.di

import android.content.Context
import com.dandi.nyummy.common.data.DeviceHelperImpl
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 기기/앱 환경 정보 제공자 바인딩. */
@Module
@InstallIn(SingletonComponent::class)
object DeviceModule {

    @Provides
    @Singleton
    fun provideDeviceHelper(
        @ApplicationContext context: Context,
    ): DeviceHelper = DeviceHelperImpl(context)
}
