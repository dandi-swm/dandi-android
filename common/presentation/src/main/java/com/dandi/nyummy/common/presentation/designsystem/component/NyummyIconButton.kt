package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyPressScale
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

enum class NyummyIconButtonStyle {
    /** 배경 없음. 툴바, 상단 바에 쓴다. */
    Ghost,

    /** 연회색 원 + 2px 테두리 + soft 그림자. 일러스트 위 HUD에 쓴다. */
    Filled,
}

/**
 * 48dp 원형 아이콘 버튼. 아이콘은 24dp, 색은 [tint](기본 content/primary).
 * 눌리면 Ghost는 선택 배경, Filled는 눌림 배경으로 바뀌고 0.94배로 줄어든다.
 */
@Composable
fun NyummyIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NyummyIconButtonStyle = NyummyIconButtonStyle.Ghost,
    enabled: Boolean = true,
    tint: Color = NyummyTheme.colors.content.primary,
) {
    val theme = NyummyTheme
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val filled = style == NyummyIconButtonStyle.Filled

    val container = when {
        filled && pressed -> theme.colors.bg.actionSecondaryPressed
        filled -> theme.colors.bg.actionSecondary
        pressed -> theme.colors.bg.selected
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .nyummyPressable(
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                pressedScale = NyummyPressScale.Circle,
            )
            .size(theme.size.touchTarget)
            .then(
                if (filled && !pressed) Modifier.nyummyShadow(CircleShape, theme.elevation.soft) else Modifier,
            )
            .background(container, CircleShape)
            .then(
                if (filled) Modifier.border(theme.borderWidth.bold, theme.colors.border.default, CircleShape) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else theme.colors.content.disabled,
            modifier = Modifier.size(theme.size.iconL),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyIconButtonPreview() {
    NyummyTheme {
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {})
            NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {}, style = NyummyIconButtonStyle.Filled)
        }
    }
}
