package com.swm.dandi.common.presentation.jank

import androidx.annotation.MainThread
import androidx.metrics.performance.FrameData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JankStats 가 매 frame 마다 호출하는 main-thread 콜백을 받아
 *   - 페이지 단위 누적 통계 (페이지 이동 / 백그라운드 진입 시 flush)
 *   - 스크롤 구간 통계 (스크롤 종료 시 flush)
 *   - 임계치 초과 즉시 보고 (jank 비율 / frozen frame)
 * 세 종류의 보고를 [JankReport] 로 위임한다.
 *
 * 모든 onXxx 메서드는 main thread 에서 호출되어야 한다 (JankStats listener 가 main thread).
 */
@Singleton
class JankReporter @Inject constructor(
    private val report: JankReport,
) {

    private var currentPage: String = PAGE_UNKNOWN
    private val pageBucket = Bucket()

    /** non-null 인 동안 스크롤 구간 통계가 누적된다. */
    private var scrollBucket: Bucket? = null

    @MainThread
    fun onFrame(frame: FrameData) {
        val durationMs = frame.frameDurationUiNanos / NANOS_PER_MS
        val isFrozen = frame.frameDurationUiNanos >= FROZEN_THRESHOLD_NANOS

        pageBucket.add(frame.isJank, isFrozen, durationMs)
        scrollBucket?.add(frame.isJank, isFrozen, durationMs)

        // 1) frozen frame: 누적 통계와 별개로 즉시 별도 보고.
        if (isFrozen) {
            report.report(
                JankSnapshot(
                    page = currentPage,
                    reason = JankSnapshot.Reason.FROZEN_FRAME,
                    totalFrames = 1,
                    jankFrames = if (frame.isJank) 1 else 0,
                    frozenFrames = 1,
                    maxFrameDurationMs = durationMs,
                    sumFrameDurationMs = durationMs,
                    states = frame.statesAsMap(),
                ),
            )
        }

        // 2) jank 비율 임계치 초과: 충분한 표본이 쌓인 뒤에만 검사하고, 보고 후 reset 해 spam 방지.
        if (pageBucket.totalFrames >= MIN_SAMPLES_FOR_THRESHOLD &&
            pageBucket.jankRatio >= JANK_RATIO_THRESHOLD
        ) {
            report.report(pageBucket.toSnapshot(currentPage, JankSnapshot.Reason.THRESHOLD_EXCEEDED, frame.statesAsMap()))
            pageBucket.reset()
        }
    }

    @MainThread
    fun onPageEnter(page: String) {
        // 다른 페이지에 머물던 잔여 통계가 있다면 새 페이지로 섞이지 않도록 먼저 비운다.
        if (currentPage != page && pageBucket.totalFrames > 0) {
            report.report(pageBucket.toSnapshot(currentPage, JankSnapshot.Reason.PAGE_EXIT))
            pageBucket.reset()
        }
        currentPage = page
    }

    @MainThread
    fun onPageExit(page: String) {
        if (currentPage != page) return
        if (pageBucket.totalFrames > 0) {
            report.report(pageBucket.toSnapshot(page, JankSnapshot.Reason.PAGE_EXIT))
            pageBucket.reset()
        }
        currentPage = PAGE_UNKNOWN
    }

    @MainThread
    fun onScrollStart() {
        scrollBucket = Bucket()
    }

    @MainThread
    fun onScrollEnd() {
        val bucket = scrollBucket ?: return
        if (bucket.totalFrames > 0) {
            report.report(bucket.toSnapshot(currentPage, JankSnapshot.Reason.SCROLL_END))
        }
        scrollBucket = null
    }

    @MainThread
    fun onAppBackground() {
        if (pageBucket.totalFrames > 0) {
            report.report(pageBucket.toSnapshot(currentPage, JankSnapshot.Reason.BACKGROUND))
            pageBucket.reset()
        }
        scrollBucket?.let {
            if (it.totalFrames > 0) {
                report.report(it.toSnapshot(currentPage, JankSnapshot.Reason.BACKGROUND))
            }
        }
        scrollBucket = null
    }

    private class Bucket {
        var totalFrames: Int = 0
            private set
        var jankFrames: Int = 0
            private set
        var frozenFrames: Int = 0
            private set
        var maxFrameDurationMs: Long = 0L
            private set
        var sumFrameDurationMs: Long = 0L
            private set
        private var lastStates: Map<String, String> = emptyMap()

        val jankRatio: Float
            get() = if (totalFrames == 0) 0f else jankFrames.toFloat() / totalFrames

        fun add(isJank: Boolean, isFrozen: Boolean, durationMs: Long) {
            totalFrames += 1
            if (isJank) jankFrames += 1
            if (isFrozen) frozenFrames += 1
            if (durationMs > maxFrameDurationMs) maxFrameDurationMs = durationMs
            sumFrameDurationMs += durationMs
        }

        fun reset() {
            totalFrames = 0
            jankFrames = 0
            frozenFrames = 0
            maxFrameDurationMs = 0L
            sumFrameDurationMs = 0L
        }

        fun toSnapshot(
            page: String,
            reason: JankSnapshot.Reason,
            states: Map<String, String> = emptyMap(),
        ) = JankSnapshot(
            page = page,
            reason = reason,
            totalFrames = totalFrames,
            jankFrames = jankFrames,
            frozenFrames = frozenFrames,
            maxFrameDurationMs = maxFrameDurationMs,
            sumFrameDurationMs = sumFrameDurationMs,
            states = states,
        )
    }

    companion object {
        private const val PAGE_UNKNOWN = "unknown"
        private const val NANOS_PER_MS = 1_000_000L

        /** 700ms 이상 걸린 단일 프레임은 "frozen" 으로 분류 (Android Vitals 기준). */
        private const val FROZEN_THRESHOLD_NANOS = 700L * NANOS_PER_MS

        /** 임계치 검사를 시작하기 전 최소 표본 수. 콜드 스타트 직후 false positive 방지. */
        private const val MIN_SAMPLES_FOR_THRESHOLD = 120

        /** 누적 jank 비율이 이 값을 넘으면 즉시 보고 + reset. */
        private const val JANK_RATIO_THRESHOLD = 0.05f
    }
}

internal fun FrameData.statesAsMap(): Map<String, String> =
    if (states.isEmpty()) emptyMap()
    else states.associate { it.key to it.value }
