package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 키보드가 떠 있는 동안 [content]를 접어 둔다. 키보드가 내려가면 다시 펼친다.
 *
 * 하단 고정 버튼이 키보드 위로 따라 올라오면 아직 입력 중인 칸을 가린다. 입력하는 동안에는 버튼을 숨겨
 * 본문이 키보드 위 높이를 다 쓰게 하고, 입력을 마치고 키보드를 내리면 버튼이 제자리에 나온다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NyummyHiddenWhileTyping(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = !WindowInsets.isImeVisible,
        modifier = modifier,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        label = "NyummyHiddenWhileTyping",
    ) {
        Column(content = content)
    }
}

/**
 * 입력칸이 아닌 빈 곳을 탭하면 포커스를 풀어 키보드를 내린다.
 * 입력칸과 버튼은 탭을 먼저 받으므로 그대로 동작한다.
 */
@Composable
fun Modifier.nyummyClearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
