package com.dandi.nyummy.common.presentation.designsystem.foundation

import android.os.SystemClock
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

/** 첫 누름을 실행한 뒤 이 시간 동안 같은 통로로 들어온 다른 누름은 버린다. */
internal const val NyummySingleClickPeriodMillis = 400L

/**
 * [com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme]이 내려 주는 누름 통로.
 * 없으면(테마 밖, 또는 `NyummyTheme(singleClick = false)`) 걸러 내지 않고 바로 실행한다.
 * 대상은 `nyummyPressable`·`nyummyClickable`에 `singleClick = true`를 준 행동 버튼뿐이다.
 * 기본 clickable을 직접 쓰는 컴포넌트(`NyummyListRow` 등)는 통로를 거치지 않는다.
 */
internal val LocalNyummySingleClick = staticCompositionLocalOf<MutableSharedFlow<() -> Unit>?> { null }

/** 행동 버튼의 누름을 모으는 통로. 버튼이 달라도 같은 통로라 서로 다른 행동 버튼을 연달아 눌러도 먼저 누른 것만 실행된다. */
internal fun nyummySingleClickFlow() = MutableSharedFlow<() -> Unit>(
    replay = 0,
    extraBufferCapacity = 1,
    // 받는 쪽이 꺼내 가기 전에 또 들어오면 먼저 누른 것을 남기고 나중 것을 버린다.
    onBufferOverflow = BufferOverflow.DROP_LATEST,
)

/** 통로를 듣는 한 곳. 받은 누름을 [throttleFirst]로 걸러 실행한다. */
internal suspend fun collectNyummySingleClicks(clicks: Flow<() -> Unit>) {
    clicks
        .throttleFirst(NyummySingleClickPeriodMillis)
        .collect { onClick ->
            try {
                onClick()
            } catch (e: CancellationException) {
                // onClick 안에서 난 취소 예외로 듣는 코루틴이 죽으면 앱 전체 버튼이 먹통이 된다.
                // 이 코루틴 자신이 취소된 경우에만 다시 던지고, 아니면 삼키고 계속 듣는다.
                currentCoroutineContext().ensureActive()
            }
        }
}

/**
 * 첫 값은 바로 내보내고, 그 뒤 [periodMillis] 동안 들어온 값은 버린다.
 * 내보낼 때만 시각을 적는다. 버릴 때도 적으면 연타하는 동안 한 번도 통과하지 못한다.
 *
 * @param now 경과 시간을 잴 시계. 기본은 뒤로 가지 않는 [SystemClock.uptimeMillis]
 */
internal fun <T> Flow<T>.throttleFirst(periodMillis: Long, now: () -> Long = SystemClock::uptimeMillis): Flow<T> = flow {
    var lastEmittedAt: Long? = null
    collect { value ->
        val current = now()
        val last = lastEmittedAt
        if (last == null || current - last >= periodMillis) {
            lastEmittedAt = current
            emit(value)
        }
    }
}
