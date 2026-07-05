package com.swm.dandi.common.presentation.jank

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug 빌드용 sink. 한 줄 요약 + 상세 상태를 Log.d 로 출력한다.
 */
@Singleton
class DebugJankReport @Inject constructor() : JankReport {
    override fun report(snapshot: JankSnapshot) {
        val ratioPct = "%.2f".format(snapshot.jankRatio * 100f)
        val avgMs = if (snapshot.totalFrames == 0) 0L
        else snapshot.sumFrameDurationMs / snapshot.totalFrames

        Log.d(
            TAG,
            "[${snapshot.reason}] page=${snapshot.page} " +
                    "frames=${snapshot.totalFrames} jank=${snapshot.jankFrames} " +
                    "frozen=${snapshot.frozenFrames} ratio=${ratioPct}% " +
                    "avg=${avgMs}ms max=${snapshot.maxFrameDurationMs}ms " +
                    "states=${snapshot.states}",
        )
    }

    companion object {
        private const val TAG = "JankStats"
    }
}
