package com.dandi.nyummy.common.presentation.jank

/**
 * 한 번의 flush 단위로 구성된 jank 통계 스냅샷.
 *
 * Debug 빌드는 [DebugJankReport] 가 받아서 Log.d 로 찍고,
 * Release 빌드는 [RemoteJankReport] 가 받아서 외부 시스템(Firebase Performance / Sentry 등)으로 전송한다.
 */
data class JankSnapshot(
    val page: String,
    val reason: Reason,
    val totalFrames: Int,
    val jankFrames: Int,
    val frozenFrames: Int,
    val maxFrameDurationMs: Long,
    val sumFrameDurationMs: Long,
    val states: Map<String, String>,
) {
    val jankRatio: Float
        get() = if (totalFrames == 0) 0f else jankFrames.toFloat() / totalFrames

    enum class Reason {
        /** 페이지를 떠나는 시점. AppNavHost 의 DisposableEffect 에서 호출. */
        PAGE_EXIT,

        /** 앱이 백그라운드로 진입한 시점. MainActivity 의 ON_STOP 에서 호출. */
        BACKGROUND,

        /** 스크롤이 끝난 직후. JankScrollWatcher 에서 호출. */
        SCROLL_END,

        /** 누적 jank 비율이 임계치를 초과한 시점. 즉시 flush 하고 버킷을 reset 한다. */
        THRESHOLD_EXCEEDED,

        /** 단일 frame duration 이 frozen 임계치를 넘은 시점. 누적 통계와 별개로 즉시 보고. */
        FROZEN_FRAME,
    }
}

/**
 * jank 통계의 sink. 빌드 타입에 따라 [DebugJankReport] / [RemoteJankReport] 중 하나가 주입된다.
 */
interface JankReport {
    fun report(snapshot: JankSnapshot)
}
