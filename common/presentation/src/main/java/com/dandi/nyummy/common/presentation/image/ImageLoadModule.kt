package com.dandi.nyummy.common.presentation.image

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 빌드 타입에 따라 [DebugImageLoadReport] / [RemoteImageLoadReport] 를 고른다.
 * JankModule 과 같은 기준(`FLAG_DEBUGGABLE`)으로 나눈다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoadModule {
    @Provides
    @Singleton
    fun provideImageLoadReport(
        @ApplicationContext context: Context,
        debugReport: dagger.Lazy<DebugImageLoadReport>,
        remoteReport: dagger.Lazy<RemoteImageLoadReport>,
    ): ImageLoadReport {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable) debugReport.get() else remoteReport.get()
    }
}
