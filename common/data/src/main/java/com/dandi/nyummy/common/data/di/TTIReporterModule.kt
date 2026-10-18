package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.tti.FirebaseTTIReporter
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

/**
 * 주입할 [TTIReporter] 를 고른다. Firebase 없이 단위 테스트할 수 있도록 모듈 밖 함수로 둔다.
 *
 * - release 가 아니면 [NoOpTTIReporter]. 개발, 벤치마크 측정이 실사용자 지표에 섞이지 않게 한다(로그는 TTILogger 가 남긴다).
 * - release 면 [createRemote] 로 만든 외부 전송 reporter. 만들다가 예외가 나면(Firebase 초기화 실패 등)
 *   [NoOpTTIReporter] 로 떨어진다. 관측 도구 장애로 앱 시작이나 화면 생성이 죽으면 안 되기 때문이다.
 */
internal fun selectTTIReporter(isReleaseBuild: Boolean, createRemote: () -> TTIReporter): TTIReporter {
    if (!isReleaseBuild) return NoOpTTIReporter
    return runCatching(createRemote).getOrDefault(NoOpTTIReporter)
}
