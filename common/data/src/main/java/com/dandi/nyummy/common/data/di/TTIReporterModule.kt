package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.tti.FirebaseTTIReporter
import com.dandi.nyummy.common.data.tti.selectTTIReporter
import com.dandi.nyummy.common.domain.buildtype.ReleaseBuild
import com.dandi.nyummy.tti.NoOpTTIReporter
import com.dandi.nyummy.tti.TTIReporter
import com.google.firebase.perf.FirebasePerformance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * TTI 측정 결과의 외부 전송 경로.
 * - 기본은 [NoOpTTIReporter] 다. release 빌드에서만 [FirebaseTTIReporter] 를 주입한다.
 * - release 라도 Firebase 를 쓸 수 없으면(초기화 실패 등) [NoOpTTIReporter] 로 떨어진다.
 * - 주입된 뒤 전송 중 장애는 [FirebaseTTIReporter] 안에서 삼키고, TTIHelper 도 reporter 예외를 막는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TTIReporterModule {
    @Provides
    @Singleton
    fun provideTTIReporter(@ReleaseBuild isReleaseBuild: Boolean): TTIReporter =
        selectTTIReporter(isReleaseBuild) { FirebaseTTIReporter(FirebasePerformance.getInstance()) }
}
