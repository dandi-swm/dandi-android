package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 카드. 테두리 대신 옅은 바탕(surface-subtle)으로 흰 화면과 나눈다. 여백 20, radius m, 그림자 없음.
 * 너비는 화면에서 채운다(고정 너비 금지). 누를 수 있으면([onClick]) 눌릴 때 검정 8%를 덮고 0.96배로 줄어든다.
 *
 * @param containerColor 바탕색. 숫자를 크게 보이는 요약 카드(내 코인)처럼 한 단계 진한 바탕이 필요할 때 바꾼다
 * @param contentPadding 안쪽 여백. 안의 줄(List Row)이 자기 좌우 여백을 가질 때는 위아래만 둔다
 * @param spacing 내용 사이 간격
 */
@Composable
fun NyummyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = NyummyTheme.colors.bg.surfaceSubtle,
    contentPadding: PaddingValues = PaddingValues(NyummyTheme.spacing.s20),
    spacing: Dp = NyummyTheme.spacing.s4,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.nyummyPressable(
                        interactionSource = interactionSource,
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(containerColor)
            .background(if (pressed) NyummyTheme.colors.bg.pressedOverlay else Color.Transparent)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** 제목(title/s)과 본문(body/m)만 있는 카드. 아래에 [content]를 더 붙일 수 있다(위 여백 8). */
@Composable
fun NyummyCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    NyummyCard(modifier = modifier, onClick = onClick) {
        NyummyText(text = title, style = NyummyTheme.typography.titleS)
        NyummyText(text = body, style = NyummyTheme.typography.bodyM, color = NyummyTheme.colors.content.secondary)
        if (content != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s8), content = content)
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyCardPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyCard(title = "오늘 1개 기록했어요", body = "칼로리와 탄단지는 참고로만 보여 줄게요.", modifier = Modifier.fillMaxWidth())
            NyummyCard(title = "오늘 1개 기록했어요", body = "칼로리와 탄단지는 참고로만 보여 줄게요.", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
