package com.dandi.nyummy.common.data.tti

import com.dandi.nyummy.tti.TTIReporter
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.util.concurrent.ConcurrentHashMap

/**
 * TTI 측정 결과를 Firebase Performance Monitoring 커스텀 트레이스로 보낸다.
 *
 * - 트레이스는 화면별로 하나다(`tti_{pageName}`). 콘솔에서 화면마다 분포(p50, p90)를 본다.
 * - 트레이스 자체 길이는 측정 시작부터 보고까지라 TTI 와 다르다. 콘솔에서는 metric `tti_ms` 를 본다.
 * - 구간 값은 metric(ms), 분류 값은 attribute 로 넣는다. 매핑은 [FirebaseTTITraceValues] 가 정한다.
 */
class FirebaseTTIReporter(
    private val performance: FirebasePerformance,
) : TTIReporter {

    // 측정 인스턴스 키(ttiKey) → 진행 중인 트레이스. 보고되면 지운다.
    private val traces = ConcurrentHashMap<String, Trace>()

    // 전송이 실패해도 측정(TTIHelper)이 멈추면 안 되므로 예외를 밖으로 내보내지 않는다.
    // 예외가 새면 TTIHelper 의 코루틴이 중단돼 시작 시각과 타임아웃 감시가 빠진다.
    override fun startView(key: String, name: String, attributes: Map<String, Any?>) {
        runCatching {
            val trace = performance.newTrace(FirebaseTTITraceValues.traceNameOf(name))
            trace.start()
            traces[key] = trace
        }
    }

    override fun stopView(key: String, attributes: Map<String, Any?>) {
        val trace = traces.remove(key) ?: return
        runCatching {
            val values = FirebaseTTITraceValues.from(attributes)
            values.metrics.forEach { (name, value) -> trace.putMetric(name, value) }
            values.attributes.forEach { (name, value) -> trace.putAttribute(name, value) }
            trace.stop()
        }
    }
}

/**
 * TTI 결과 맵(`tti.` 접두어)을 Firebase 트레이스의 metric 과 attribute 로 바꾼다.
 *
 * - metric: 구간 값(ns)을 ms 로 바꿔 `{구간}_ms` 이름으로 넣는다. 측정하지 않은 구간(-1)은 넣지 않는다.
 * - attribute: 콘솔 필터에 쓸 값만 넣는다(트레이스당 최대 5개). page_name 은 트레이스 이름에,
 *   instance_no 는 값 종류가 계속 늘어나 필터로 쓸 수 없어 넣지 않는다.
 */
data class FirebaseTTITraceValues(
    val metrics: Map<String, Long>,
    val attributes: Map<String, String>,
) {
    companion object {
        private const val TTI_PREFIX = "tti."
        private const val TIME_SUFFIX = "_time"
        private const val NANOS_PER_MILLI = 1_000_000L

        private val ATTRIBUTE_KEYS = listOf("is_bounced", "is_timeout", "user_wait_included", "tti_log_version")

        fun traceNameOf(pageName: String): String = "tti_$pageName"

        fun from(result: Map<String, Any?>): FirebaseTTITraceValues {
            val fields = result.mapKeys { (key, _) -> key.removePrefix(TTI_PREFIX) }
            val metrics = fields
                .filterKeys { it.endsWith(TIME_SUFFIX) }
                .mapNotNull { (name, value) ->
                    val nanos = (value as? Number)?.toLong() ?: return@mapNotNull null
                    if (nanos < 0L) return@mapNotNull null
                    name.removeSuffix(TIME_SUFFIX) + "_ms" to nanos / NANOS_PER_MILLI
                }
                .toMap()
            val attributes = ATTRIBUTE_KEYS
                .mapNotNull { name -> fields[name]?.let { name to it.toString() } }
                .toMap()
            return FirebaseTTITraceValues(metrics = metrics, attributes = attributes)
        }
    }
}
