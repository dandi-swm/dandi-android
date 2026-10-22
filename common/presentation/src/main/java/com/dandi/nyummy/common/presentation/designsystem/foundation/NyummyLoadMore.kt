package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 마지막 행이 [LOAD_MORE_THRESHOLD]행 안쪽으로 들어오면 [onReachEnd]를 부른다. 다음 묶음을 이어 읽는 목록(우편함, 문의 내역)이 쓴다.
 *
 * 로딩 표시 행은 세지 않는다. 그래야 다음 묶음 읽기가 실패해 로딩 표시가 사라져도 곧바로 다시 부르지 않고,
 * 사용자가 다시 스크롤하거나 목록이 늘었을 때만 부른다. 중복 요청은 ViewModel이 거른다.
 */
@Composable
fun NyummyLoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    onReachEnd: () -> Unit,
) {
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnReachEnd by rememberUpdatedState(onReachEnd)
    LaunchedEffect(listState) {
        snapshotFlow {
            val count = currentItemCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            count to lastVisible.coerceAtMost(count - 1)
        }
            .distinctUntilChanged()
            .filter { (count, lastVisible) -> count > 0 && lastVisible >= count - 1 - LOAD_MORE_THRESHOLD }
            .collect { currentOnReachEnd() }
    }
}

/** 끝에서 몇 행 전에 다음 묶음을 읽기 시작할지. */
private const val LOAD_MORE_THRESHOLD = 3
