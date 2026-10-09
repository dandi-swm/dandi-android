package com.dandi.nyummy.common.presentation.image

import com.dandi.nyummy.common.domain.buildtype.ReleaseBuild
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 빌드 타입에 따라 [DebugImageLoadReport] / [RemoteImageLoadReport] 를 고른다.
 * TTI, 버벅임과 같은 기준([ReleaseBuild])으로 release 빌드에서만 Firebase 로 보낸다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoadModule {
    @Provides
    @Singleton
    fun provideImageLoadReport(
        @ReleaseBuild isReleaseBuild: Boolean,
        debugReport: dagger.Lazy<DebugImageLoadReport>,
        remoteReport: dagger.Lazy<RemoteImageLoadReport>,
    ): ImageLoadReport = if (isReleaseBuild) remoteReport.get() else debugReport.get()
}
