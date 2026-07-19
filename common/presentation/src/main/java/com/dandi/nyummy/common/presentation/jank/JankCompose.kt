package com.dandi.nyummy.common.presentation.jank

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState
import kotlinx.coroutines.flow.distinctUntilChanged

val LocalJankReporter = compositionLocalOf<JankReporter> {
    error("LocalJankReporter is not provided. Wrap with CompositionLocalProvider in MainActivity.")
}

/**
 * 현재 활성 페이지의 식별자를 JankStats state 로 등록하고, 컴포지션 이탈 시
 * 해당 페이지의 누적 통계를 [JankSnapshot.Reason.PAGE_EXIT] 로 flush 한다.
 *
 * 사용 위치: [com.dandi.nyummy.main.presentation.navigation.AppNavHost] 의
 * 백스택 최상위가 변경될 때마다 호출.
 */
@Composable
fun JankPageEffect(pagePath: String) {
    val view = LocalView.current
    val reporter = LocalJankReporter.current
    DisposableEffect(view, pagePath) {
        val holder = PerformanceMetricsState.getHolderForHierarchy(view)
        holder.state?.putState(STATE_PAGE, pagePath)
        reporter.onPageEnter(pagePath)
        onDispose {
            holder.state?.removeState(STATE_PAGE)
            reporter.onPageExit(pagePath)
        }
    }
}

/**
 * Lazy* state 의 `isScrollInProgress` 변화를 감지해 JankStats state 와 [JankReporter] 의
 * 스크롤 버킷에 반영한다. 스크롤 종료 시점에 해당 구간의 jank 통계를
 * [JankSnapshot.Reason.SCROLL_END] 로 flush 한다.
 *
 * LazyListState, LazyGridState 등 [ScrollableState] 구현의 스크롤 상태에 연결한다.
 */
@Composable
fun JankScrollWatcher(scrollableState: ScrollableState) {
    val view = LocalView.current
    val reporter = LocalJankReporter.current
    LaunchedEffect(scrollableState, view) {
        snapshotFlow { scrollableState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                val state = PerformanceMetricsState.getHolderForHierarchy(view).state
                if (isScrolling) {
                    state?.putState(STATE_SCROLLING, "true")
                    reporter.onScrollStart()
                } else {
                    state?.removeState(STATE_SCROLLING)
                    reporter.onScrollEnd()
                }
            }
    }
}

private const val STATE_PAGE = "page"
private const val STATE_SCROLLING = "scrolling"
