package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 화면 위에 까는 딤(bg/scrim 40%). 아래 화면으로 가는 탭, 드래그, 스크롤을 모두 막는다.
 *
 * [onClick]을 주면 딤을 눌러 닫을 수 있고 접근성 서비스에는 "닫기" 동작으로 알린다.
 * 없으면(고지, 처리 중 로딩 등) 입력만 막는다.
 */
@Composable
fun NyummyScrim(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val input = if (onClick != null) {
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClickLabel = stringResource(R.string.nyummy_scrim_dismiss_label),
            onClick = onClick,
        )
    } else {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.scrim)
            .then(input),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun NyummyScrimPreview() {
    NyummyTheme {
        NyummyScrim()
    }
}
