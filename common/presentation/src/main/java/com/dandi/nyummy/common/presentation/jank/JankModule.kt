package com.dandi.nyummy.common.presentation.jank

import com.dandi.nyummy.common.domain.buildtype.ReleaseBuild
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 빌드 타입에 따라 [DebugJankReport] / [RemoteJankReport] 를 선택해 주입한다.
 *
 * release 빌드에서만 [RemoteJankReport](Firebase)를 쓴다. benchmark 와 베이스라인 프로파일 수집 빌드는
 * debuggable 이 아니어서 `FLAG_DEBUGGABLE` 로는 release 와 구분되지 않으므로 [ReleaseBuild] 로 정한다.
 * [RemoteJankReport] 는 Firebase 를 쓸 수 없으면 아무것도 보내지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object JankModule {
    @Provides
    @Singleton
    fun provideJankReport(
        @ReleaseBuild isReleaseBuild: Boolean,
        debugReport: dagger.Lazy<DebugJankReport>,
        remoteReport: dagger.Lazy<RemoteJankReport>,
    ): JankReport = if (isReleaseBuild) remoteReport.get() else debugReport.get()
}
