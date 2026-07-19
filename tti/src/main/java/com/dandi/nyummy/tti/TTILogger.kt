package com.dandi.nyummy.tti

/**
 * 디버깅용 로그 싱크. 빌드 타입에 따라 [DebugTTILogger] / [RemoteTTILogger] 중 하나가 주입된다.
 */
interface TTILogger {
    fun d(tag: String, msg: String)
}

/**
 * Debug 빌드용 sink. println 으로 출력한다.
 *
 * `:tti` 모듈은 플랫폼 비의존이므로 android.util.Log 대신 stdout 을 사용한다.
 * Logcat 에는 stdout/stderr 도 함께 노출되므로 개발 중 문제는 없다.
 */
class DebugTTILogger : TTILogger {
    override fun d(tag: String, msg: String) {
        println("[$tag] $msg")
    }
}

/**
 * Release 빌드용 sink.
 *
 * 추후 사내 모니터링 SDK / Datadog Logs / Sentry 등 외부 로깅 시스템 연동 지점.
 * 현재는 의존성 미연결 상태이므로 메서드 본문은 비워 둔다.
 */
class RemoteTTILogger : TTILogger {
    override fun d(tag: String, msg: String) {
        // TODO: 사내 모니터링 SDK / Datadog Logs / Sentry 등 외부 로깅 연동
    }
}
