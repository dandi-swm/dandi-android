package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 아래로 당겨 새로 고침. [content] 안의 세로 스크롤(LazyColumn 등)이 맨 위일 때 더 당기면 위에서 인디케이터가 내려오고,
 * 놓으면 [onRefresh]를 부른다. [isRefreshing]이 true인 동안 인디케이터가 돈다.
 * 인디케이터는 Material3 기본 모양에 바탕(surface)·브랜드색 토큰만 입힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NyummyPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = NyummyTheme.colors.bg.surface,
                color = NyummyTheme.colors.content.brand,
            )
        },
        content = content,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun NyummyPullToRefreshBoxPreview() {
    NyummyTheme {
        NyummyPullToRefreshBox(isRefreshing = true, onRefresh = {}) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(5) { NyummyText(text = "줄 ${it + 1}", style = NyummyTheme.typography.bodyM) }
            }
        }
    }
}
