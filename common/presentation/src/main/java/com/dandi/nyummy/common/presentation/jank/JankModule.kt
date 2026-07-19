package com.dandi.nyummy.common.presentation.jank

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 빌드 타입에 따라 [DebugJankReport] / [RemoteJankReport] 를 선택해 주입한다.
 *
 * `BuildConfig.DEBUG` 대신 `ApplicationInfo.FLAG_DEBUGGABLE` 을 사용하는 이유:
 * 모듈마다 buildConfig 를 켜지 않고도 동일하게 분기할 수 있고, 사내 다이렉트 빌드(debuggable=true)
 * 같은 비공식 변형도 자동으로 Debug 경로로 흐른다.
 */
@Module
@InstallIn(SingletonComponent::class)
object JankModule {
    @Provides
    @Singleton
    fun provideJankReport(
        @ApplicationContext context: Context,
        debugReport: dagger.Lazy<DebugJankReport>,
        remoteReport: dagger.Lazy<RemoteJankReport>,
    ): JankReport {
        val isDebuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable) debugReport.get() else remoteReport.get()
    }
}
