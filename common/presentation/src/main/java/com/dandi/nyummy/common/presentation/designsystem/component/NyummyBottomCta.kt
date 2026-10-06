package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 보조:주 버튼 너비 비율 3:7. */
private const val SecondaryWeight = 3f
private const val PrimaryWeight = 7f

/** 위쪽 흰색 페이드가 끝나는 지점(높이 대비). */
private const val FadeStop = 0.18f

/**
 * 화면 하단 고정 버튼 영역. 좌우 gutter, 위 12, 아래는 내비게이션 바 + 24.
 * 위쪽은 흰색으로 페이드되어 스크롤 콘텐츠가 자연스럽게 가려진다.
 *
 * [secondaryText]가 있으면 보조:주 = 3:7로 나란히 둔다. 화면당 Primary는 1개다.
 */
@Composable
fun NyummyBottomCta(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondaryClick: () -> Unit = {},
) {
    val theme = NyummyTheme
    val canvas = theme.colors.bg.canvas
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to canvas.copy(alpha = 0f),
                    FadeStop to canvas,
                    1f to canvas,
                ),
            )
            .navigationBarsPadding()
            .padding(
                start = theme.spacing.gutter,
                end = theme.spacing.gutter,
                top = theme.spacing.s12,
                bottom = theme.spacing.s24,
            ),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s8),
    ) {
        if (secondaryText != null) {
            NyummyButton(
                text = secondaryText,
                onClick = onSecondaryClick,
                style = NyummyButtonStyle.Secondary,
                modifier = Modifier.weight(SecondaryWeight),
            )
        }
        NyummyButton(
            text = primaryText,
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            modifier = Modifier.weight(if (secondaryText != null) PrimaryWeight else 1f),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun NyummyBottomCtaPreview() {
    NyummyTheme {
        Box(Modifier.fillMaxSize().background(Color.LightGray)) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                NyummyBottomCta(primaryText = "확인", onPrimaryClick = {})
                NyummyBottomCta(primaryText = "확인", onPrimaryClick = {}, secondaryText = "닫기")
            }
        }
    }
}
