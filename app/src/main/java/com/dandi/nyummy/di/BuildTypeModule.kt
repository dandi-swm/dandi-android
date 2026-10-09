package com.dandi.nyummy.di

import com.dandi.nyummy.BuildConfig
import com.dandi.nyummy.common.domain.buildtype.ReleaseBuild
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BuildTypeModule {

    /** app/build.gradle.kts 가 빌드 타입 이름이 정확히 release 일 때만 true 로 넣는다. */
    @Provides
    @ReleaseBuild
    fun provideIsReleaseBuild(): Boolean = BuildConfig.IS_RELEASE_BUILD
}
