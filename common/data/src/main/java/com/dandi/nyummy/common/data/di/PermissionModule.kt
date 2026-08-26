package com.dandi.nyummy.common.data.di

import android.content.Context
import com.dandi.nyummy.common.data.PermissionHelperImpl
import com.dandi.nyummy.common.domain.helper.PermissionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 런타임 권한 상태 조회 헬퍼 바인딩. */
@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionHelper(
        @ApplicationContext context: Context,
    ): PermissionHelper = PermissionHelperImpl(context)
}
