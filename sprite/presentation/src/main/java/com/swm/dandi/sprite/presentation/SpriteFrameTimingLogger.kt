package com.swm.dandi.sprite.presentation

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.max

/**
 * SpritePage sample 화면에서 frame index 진행 간격을 debug log 로 기록한다.
 *
 * JankStats 가 UI frame deadline miss 를 본다면, 이 logger 는 sprite frame index 가 기대한
 * 시간 간격대로 증가했는지 확인한다. release 빌드에서는 동작하지 않는다.
 */
@Composable
internal fun SpriteFrameTimingLogEffect(uiState: SpriteUIState) {
    val logger = remember { SpriteFrameTimingLogger() }

    LaunchedEffect(
        uiState.selectedAnimation,
        uiState.selectedAlignElement,
        uiState.frameDurationMillis,
        uiState.loop,
    ) {
        logger.reset()
    }

    LaunchedEffect(uiState.currentFrame, uiState.isPlaying) {
        if (!uiState.isPlaying) {
            logger.reset()
            return@LaunchedEffect
        }

        logger.onFrameChanged(
            context = SpriteFrameTimingContext(
                animationName = uiState.selectedAnimation.name,
                alignElementName = uiState.selectedAlignElement.name,
                expectedDeltaMillis = uiState.frameDurationMillis,
                frameIndex = uiState.currentFrame,
                totalFrames = uiState.selectedSpec.totalFrames,
            ),
        )
    }
}

internal class SpriteFrameTimingLogger(
    private val enabled: Boolean = BuildConfig.DEBUG,
    private val nowNanos: () -> Long = { SystemClock.elapsedRealtimeNanos() },
    private val log: (String) -> Unit = { message -> Log.d(TAG, message) },
) {
    private var lastFrameTimeNanos: Long? = null
    private var sampleCount: Int = 0
    private var lateFrames: Int = 0
    private var missedFrames: Int = 0
    private var frozenFrames: Int = 0
    private var maxActualDeltaMillis: Long = 0L
    private var sumActualDeltaMillis: Long = 0L

    fun reset() {
        lastFrameTimeNanos = null
        resetWindowStats()
    }

    fun onFrameChanged(context: SpriteFrameTimingContext) {
        if (!enabled) return

        val now = nowNanos()
        val previous = lastFrameTimeNanos
        lastFrameTimeNanos = now
        if (previous == null) return

        val actualDeltaMillis = ((now - previous) / NANOS_PER_MILLIS)
            .coerceAtLeast(0L)
        val expectedDeltaMillis = context.expectedDeltaMillis
        if (expectedDeltaMillis <= 0L) return

        val missedFrameCount = (actualDeltaMillis / expectedDeltaMillis - 1)
            .coerceAtLeast(0L)
        val isLate = actualDeltaMillis * 10L > expectedDeltaMillis * 15L
        val isFrozen = actualDeltaMillis >= max(
            SpriteFrozenFrameMillis,
            expectedDeltaMillis * SpriteFrozenFrameMultiplier,
        )

        sampleCount += 1
        sumActualDeltaMillis += actualDeltaMillis
        maxActualDeltaMillis = max(maxActualDeltaMillis, actualDeltaMillis)
        if (isLate) lateFrames += 1
        if (missedFrameCount > 0L) missedFrames += 1
        if (isFrozen) frozenFrames += 1

        when {
            isFrozen -> reportFrameTiming(
                reason = SpriteFrameTimingReason.SPRITE_FROZEN,
                context = context,
                actualDeltaMillis = actualDeltaMillis,
                missedFrameCount = missedFrameCount,
            )

            missedFrameCount > 0L -> reportFrameTiming(
                reason = SpriteFrameTimingReason.SPRITE_FRAME_MISSED,
                context = context,
                actualDeltaMillis = actualDeltaMillis,
                missedFrameCount = missedFrameCount,
            )

            isLate -> reportFrameTiming(
                reason = SpriteFrameTimingReason.SPRITE_FRAME_LATE,
                context = context,
                actualDeltaMillis = actualDeltaMillis,
                missedFrameCount = missedFrameCount,
            )
        }

        if (sampleCount >= SummarySampleCount) {
            reportSummary(context)
            resetWindowStats()
        }
    }

    private fun reportFrameTiming(
        reason: SpriteFrameTimingReason,
        context: SpriteFrameTimingContext,
        actualDeltaMillis: Long,
        missedFrameCount: Long,
    ) {
        log(
            "[${reason.name}] animation=${context.animationName} " +
                    "align=${context.alignElementName} " +
                    "target=${context.expectedDeltaMillis}ms actual=${actualDeltaMillis}ms " +
                    "late=${actualDeltaMillis - context.expectedDeltaMillis}ms " +
                    "missed=$missedFrameCount frame=${context.frameIndex + 1}/${context.totalFrames}",
        )
    }

    private fun reportSummary(context: SpriteFrameTimingContext) {
        val avgActualDeltaMillis = if (sampleCount == 0) 0L
        else sumActualDeltaMillis / sampleCount
        val lateRatioPct = if (sampleCount == 0) 0f
        else lateFrames.toFloat() / sampleCount * 100f

        log(
            "[${SpriteFrameTimingReason.SPRITE_SUMMARY.name}] " +
                    "animation=${context.animationName} align=${context.alignElementName} " +
                    "target=${context.expectedDeltaMillis}ms samples=$sampleCount " +
                    "late=$lateFrames missed=$missedFrames frozen=$frozenFrames " +
                    "lateRatio=${"%.2f".format(lateRatioPct)}% " +
                    "avg=${avgActualDeltaMillis}ms max=${maxActualDeltaMillis}ms",
        )
    }

    private fun resetWindowStats() {
        sampleCount = 0
        lateFrames = 0
        missedFrames = 0
        frozenFrames = 0
        maxActualDeltaMillis = 0L
        sumActualDeltaMillis = 0L
    }

    private companion object {
        private const val TAG = "SpriteFrameStats"
        private const val NANOS_PER_MILLIS = 1_000_000L
        private const val SummarySampleCount = 30
        private const val SpriteFrozenFrameMillis = 300L
        private const val SpriteFrozenFrameMultiplier = 5L
    }
}

internal data class SpriteFrameTimingContext(
    val animationName: String,
    val alignElementName: String,
    val expectedDeltaMillis: Long,
    val frameIndex: Int,
    val totalFrames: Int,
)

internal enum class SpriteFrameTimingReason {
    SPRITE_FRAME_LATE,
    SPRITE_FRAME_MISSED,
    SPRITE_FROZEN,
    SPRITE_SUMMARY,
}
