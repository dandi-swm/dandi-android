package com.swm.dandi.common.presentation.jank

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Release 빌드용 sink.
 *
 * 추후 Firebase Performance / Sentry / 사내 모니터링 SDK 연동 지점.
 * 현재는 의존성 미연결 상태이므로 메서드 본문은 비워 둔다.
 */
@Singleton
class RemoteJankReport @Inject constructor() : JankReport {
    override fun report(snapshot: JankSnapshot) {
        // TODO: Firebase Performance / Sentry 등 외부 모니터링 연동
        // 예시:
        //   FirebasePerformance.getInstance()
        //       .newTrace("jank_${snapshot.page}")
        //       .apply {
        //           putMetric("totalFrames", snapshot.totalFrames.toLong())
        //           putMetric("jankFrames", snapshot.jankFrames.toLong())
        //           putAttribute("reason", snapshot.reason.name)
        //       }
        //       .stop()
    }
}
