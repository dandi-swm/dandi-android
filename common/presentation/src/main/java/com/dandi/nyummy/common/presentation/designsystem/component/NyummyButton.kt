package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

enum class NyummyButtonStyle {
    /** 에버그린 채움. 화면당 1개만 둔다. */
    Primary,

    /** 연회색 채움, 테두리와 그림자 없음. */
    Secondary,

    /** 빨강 채움. 파괴적 행동을 확정할 때만 쓴다. */
    Danger,

    /** 배경 없음, 브랜드색 글자. */
    Ghost,
}

enum class NyummyButtonSize {
    /** 높이 56, radius 16, label/l, 아이콘 24. */
    L,

    /** 높이 48, radius 12, label/m, 아이콘 20. */
    M,

    /** 높이 36, radius 8, label/m, 아이콘 16. 목록 행 안의 작은 행동용. 터치 영역은 48로 넓힌다. */
    S,
}

/**
 * 냐미 기본 버튼. 그림자 없는 플랫 버튼이고, 눌리면 검정 8%를 덮고(Secondary는 gray/200) 0.96배로 줄어든다.
 * 너비는 내용에 맞춘다. 꽉 채우려면 `Modifier.fillMaxWidth()`를 넘긴다.
 */
@Composable
fun NyummyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NyummyButtonStyle = NyummyButtonStyle.Primary,
    size: NyummyButtonSize = NyummyButtonSize.L,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    interactionSource: MutableInteractionSource = rememberNyummyInteractionSource(),
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val spec = buttonSizeSpec(size)
    val colors = buttonColors(style = style, enabled = enabled, pressed = pressed)
    val shape = RoundedCornerShape(spec.radius)

    Row(
        modifier = modifier
            .nyummyPressable(
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .height(spec.height)
            .clip(shape)
            .background(colors.container)
            .background(colors.overlay)
            .padding(horizontal = spec.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(spec.iconSize),
            )
        }
        NyummyText(text = text, style = spec.textStyle, color = colors.content, maxLines = 1)
    }
}

private class ButtonSizeSpec(
    val height: Dp,
    val radius: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

@Composable
private fun buttonSizeSpec(size: NyummyButtonSize): ButtonSizeSpec {
    return when (size) {
        NyummyButtonSize.L -> ButtonSizeSpec(
            height = NyummyTheme.size.buttonL,
            radius = NyummyTheme.radius.m,
            horizontalPadding = NyummyTheme.spacing.s24,
            iconSize = NyummyTheme.size.iconL,
            textStyle = NyummyTheme.typography.labelL,
        )
        NyummyButtonSize.M -> ButtonSizeSpec(
            height = NyummyTheme.size.buttonM,
            radius = NyummyTheme.radius.s,
            horizontalPadding = NyummyTheme.spacing.s24,
            iconSize = NyummyTheme.size.iconM,
            textStyle = NyummyTheme.typography.labelM,
        )
        NyummyButtonSize.S -> ButtonSizeSpec(
            height = NyummyComponentDimens.CompactControlHeight,
            radius = NyummyTheme.radius.xs,
            horizontalPadding = NyummyComponentDimens.CompactControlHorizontalPadding,
            iconSize = NyummyTheme.size.iconS,
            textStyle = NyummyTheme.typography.labelM,
        )
    }
}

private class ButtonColors(val container: Color, val overlay: Color, val content: Color)

@Composable
private fun buttonColors(style: NyummyButtonStyle, enabled: Boolean, pressed: Boolean): ButtonColors {
    val bg = NyummyTheme.colors.bg
    val content = NyummyTheme.colors.content
    if (!enabled) {
        val container = if (style == NyummyButtonStyle.Ghost) Color.Transparent else bg.actionDisabled
        return ButtonColors(container = container, overlay = Color.Transparent, content = content.disabled)
    }
    val overlay = if (pressed) bg.pressedOverlay else Color.Transparent
    return when (style) {
        NyummyButtonStyle.Primary -> ButtonColors(bg.actionPrimary, overlay, content.onAction)
        NyummyButtonStyle.Danger -> ButtonColors(bg.actionDanger, overlay, content.onAction)
        NyummyButtonStyle.Secondary -> ButtonColors(
            container = if (pressed) bg.actionSecondaryPressed else bg.actionSecondary,
            overlay = Color.Transparent,
            content = content.primary,
        )
        NyummyButtonStyle.Ghost -> ButtonColors(
            container = if (pressed) bg.actionSecondary else Color.Transparent,
            overlay = Color.Transparent,
            content = content.brand,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyButtonPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            NyummyButtonStyle.entries.forEach { style ->
                Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
                    NyummyButtonSize.entries.forEach { size ->
                        NyummyButton(text = "밥 주기", onClick = {}, style = style, size = size)
                    }
                }
            }
            NyummyButton(text = "밥 주기", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            NyummyButton(text = "사진 추가", onClick = {}, icon = R.drawable.nyummy_ic_image_plus, style = NyummyButtonStyle.Secondary)
        }
    }
}
