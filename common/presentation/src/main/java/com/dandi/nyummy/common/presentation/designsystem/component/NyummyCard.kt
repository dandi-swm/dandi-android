package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 카드. 여백 20, radius m, 그림자 없음. 너비는 화면에서 채운다(고정 너비 금지).
 *
 * - 정보 카드([onClick] == null): 2px default 테두리
 * - 누를 수 있는 카드: 1px subtle 테두리, 눌리면 검정 8% 덮기 + 0.96배
 */
@Composable
fun NyummyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.m)
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val pressable = onClick != null

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
            .background(theme.colors.bg.surface)
            .background(if (pressed) theme.colors.bg.pressedOverlay else Color.Transparent)
            .border(
                width = if (pressable) theme.borderWidth.hairline else theme.borderWidth.bold,
                color = if (pressable) theme.colors.border.subtle else theme.colors.border.default,
                shape = shape,
            )
            .padding(theme.spacing.s20),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s4),
        content = content,
    )
}

/** 제목(title/m)과 본문(body/m)만 있는 카드. 아래에 [content]를 더 붙일 수 있다(위 여백 8). */
@Composable
fun NyummyCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    NyummyCard(modifier = modifier, onClick = onClick) {
        NyummyText(text = title, style = NyummyTheme.typography.titleM)
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
