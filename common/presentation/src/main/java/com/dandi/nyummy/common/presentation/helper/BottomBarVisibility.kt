package com.dandi.nyummy.common.presentation.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 하단 내비를 화면이 잠시 숨길 수 있게 하는 스위치. 앱 루트가 하나 만들어 공급하고, 하단 내비를 그릴 때 읽는다.
 * 홈의 눈 버튼처럼 냐미와 방만 보여 줄 때 쓴다.
 */
@Stable
class BottomBarVisibility {
    var isHidden by mutableStateOf(false)
        internal set
}

val LocalBottomBarVisibility = staticCompositionLocalOf { BottomBarVisibility() }

/** [hidden]인 동안 하단 내비를 숨긴다. 이 화면을 떠나면 다시 보인다. */
@Composable
fun HideBottomBarEffect(hidden: Boolean) {
    val visibility = LocalBottomBarVisibility.current
    DisposableEffect(visibility, hidden) {
        visibility.isHidden = hidden
        onDispose { visibility.isHidden = false }
    }
}
