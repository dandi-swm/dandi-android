package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.BuildConfig
import com.dandi.nyummy.common.data.tti.FirebaseTTIReporter
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
 * - release 빌드는 Firebase Performance 로 보낸다.
 * - debug 빌드는 개발 중 측정이 실사용자 지표에 섞이지 않게 보내지 않는다(Logcat 로그만).
 * - Firebase 를 쓸 수 없으면(초기화 실패 등) [NoOpTTIReporter] 로 떨어진다. 전송 장애가
 *   ViewModel 생성이나 측정을 막으면 안 되기 때문이다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TTIReporterModule {
    @Provides
    @Singleton
    fun provideTTIReporter(): TTIReporter {
        if (BuildConfig.DEBUG) return NoOpTTIReporter
        return runCatching { FirebaseTTIReporter(FirebasePerformance.getInstance()) }
            .getOrDefault(NoOpTTIReporter)
    }
}
