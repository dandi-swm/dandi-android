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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentShadows
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

/** 보조:주 버튼 너비 비율 3:7. */
private const val SecondaryWeight = 3f
private const val PrimaryWeight = 7f

/**
 * 화면 하단 고정 버튼 영역. 흰 바탕에 위로 옅은 그림자를 드리워 스크롤 콘텐츠와 나눈다.
 * 좌우 gutter, 위 12, 아래는 내비게이션 바 + 8.
 *
 * [caption]이 있으면 버튼 위에 짧은 안내를 tertiary로 둔다. 안내가 오류나 꼭 알아야 할 정보면 이 자리에 쓰지 않는다.
 * [secondaryText]가 있으면 보조(회색):주 = 3:7로 나란히 둔다. 화면당 Primary는 1개다.
 */
@Composable
fun NyummyBottomCta(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    caption: String? = null,
    secondaryText: String? = null,
    onSecondaryClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nyummyShadow(RectangleShape, NyummyComponentShadows.BottomCta)
            .background(NyummyTheme.colors.bg.surface)
            .navigationBarsPadding()
            .padding(
                start = NyummyTheme.spacing.gutter,
                end = NyummyTheme.spacing.gutter,
                top = NyummyTheme.spacing.s12,
                bottom = NyummyTheme.spacing.s8,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (caption != null) {
            NyummyText(
                text = caption,
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                textAlign = TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            if (secondaryText != null) {
                NyummyButton(
                    text = secondaryText,
                    onClick = onSecondaryClick,
                    style = NyummyButtonStyle.Neutral,
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
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun NyummyBottomCtaPreview() {
    NyummyTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s24),
            ) {
                NyummyBottomCta(primaryText = "확인", onPrimaryClick = {})
                NyummyBottomCta(primaryText = "보내기", onPrimaryClick = {}, caption = "답장은 설정의 문의하기에서 볼 수 있어요")
                NyummyBottomCta(primaryText = "확인", onPrimaryClick = {}, secondaryText = "닫기")
            }
        }
    }
}
