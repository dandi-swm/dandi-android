package com.dandi.nyummy.common.presentation.jank

import com.google.firebase.perf.FirebasePerformance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Release 빌드용 sink. 화면별 버벅임 통계를 Firebase Performance 커스텀 트레이스로 보낸다.
 *
 * - Firebase 자동 화면 트레이스는 Activity 단위라 단일 Activity(Compose)인 냐미에서는 화면을 구분하지 못한다.
 *   그래서 JankStats 가 화면(route)별로 모은 통계를 `jank_{화면}` 트레이스로 직접 보낸다.
 * - 값은 metric, 분류는 attribute 로 넣는다. 매핑은 [FirebaseJankTraceValues] 가 정한다.
 * - 트레이스 자체 길이는 의미가 없다(바로 시작하고 끝낸다). 콘솔에서는 metric 을 본다.
 * - Firebase 를 쓸 수 없거나 전송이 실패하면 아무것도 보내지 않는다(NoOp). 앱 동작이나 프레임 측정에는 영향이 없다.
 */
@Singleton
class RemoteJankReport @Inject constructor() : JankReport {

    private val performance: FirebasePerformance? by lazy {
        runCatching { FirebasePerformance.getInstance() }.getOrNull()
    }

    override fun report(snapshot: JankSnapshot) {
        val values = FirebaseJankTraceValues.from(snapshot) ?: return
        val performance = performance ?: return
        runCatching {
            val trace = performance.newTrace(values.traceName)
            trace.start()
            values.metrics.forEach { (name, value) -> trace.putMetric(name, value) }
            values.attributes.forEach { (name, value) -> trace.putAttribute(name, value) }
            trace.stop()
        }
    }
}

/**
 * [JankSnapshot] 을 Firebase 트레이스 값으로 바꾼다.
 *
 * - 스크롤 구간 통계(SCROLL_END)는 보내지 않는다. 같은 프레임이 화면 통계에도 들어가 중복이고,
 *   스크롤마다 보내면 SDK 의 전송량 제한에 걸려 TTI 같은 다른 트레이스가 버려질 수 있다.
 * - 트레이스 이름은 화면 경로를 영문, 숫자, 밑줄로 바꿔 만든다(`/meal/record` → `jank_meal_record`).
 *   인트로는 경로가 빈 문자열이라 `jank_intro` 로 둔다.
 */
data class FirebaseJankTraceValues(
    val traceName: String,
    val metrics: Map<String, Long>,
    val attributes: Map<String, String>,
) {
    companion object {
        private const val TRACE_PREFIX = "jank_"

        // 인트로 화면의 경로는 빈 문자열("")이다.
        private const val INTRO_TRACE_NAME = "intro"
        private const val MAX_TRACE_NAME_LENGTH = 100
        private const val STATE_SCROLLING = "scrolling"
        private const val PERMILLE = 1000L

        fun from(snapshot: JankSnapshot): FirebaseJankTraceValues? {
            if (snapshot.reason == JankSnapshot.Reason.SCROLL_END) return null
            if (snapshot.totalFrames == 0) return null
            val avgFrameMs = snapshot.sumFrameDurationMs / snapshot.totalFrames
            return FirebaseJankTraceValues(
                traceName = traceNameOf(snapshot.page),
                metrics = mapOf(
                    "total_frames" to snapshot.totalFrames.toLong(),
                    "jank_frames" to snapshot.jankFrames.toLong(),
                    "frozen_frames" to snapshot.frozenFrames.toLong(),
                    // metric 은 정수만 받으므로 비율은 천분율로 보낸다. 실수 곱셈은 7/10 이 699 가 되는 식으로 내려가 정수로 계산한다.
                    "jank_permille" to snapshot.jankFrames.toLong() * PERMILLE / snapshot.totalFrames,
                    "avg_frame_ms" to avgFrameMs,
                    "max_frame_ms" to snapshot.maxFrameDurationMs,
                ),
                attributes = mapOf(
                    "reason" to snapshot.reason.name.lowercase(),
                    "during_scroll" to (snapshot.states[STATE_SCROLLING] == "true").toString(),
                ),
            )
        }

        fun traceNameOf(page: String): String {
            val name = page.trim('/')
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .trim('_')
                .ifEmpty { INTRO_TRACE_NAME }
            return (TRACE_PREFIX + name).take(MAX_TRACE_NAME_LENGTH)
        }
    }
}
