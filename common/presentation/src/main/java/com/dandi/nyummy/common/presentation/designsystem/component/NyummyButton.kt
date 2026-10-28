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

/** 버튼 위계. 주 > 톤 > 회색 > 글자 순으로 약해진다. 검정 채움 버튼은 쓰지 않는다. */
enum class NyummyButtonStyle {
    /** 초록 채움. 화면의 주 행동 하나에만 쓴다. */
    Primary,

    /** 연초록 채움, 초록 글자. 목록 행 안의 행동("받기")에 쓴다. */
    Tonal,

    /** 회색 채움, 회색 글자. 보조 행동에 쓴다. */
    Neutral,

    /** 배경 없음, 회색 글자. 가장 약한 행동에 쓴다. */
    Text,

    /** 빨강 채움. 삭제·탈퇴처럼 되돌릴 수 없는 행동을 확정할 때만 쓴다. */
    Danger,
}

enum class NyummyButtonSize {
    /** 큰 버튼. 높이 56, radius 16, 좌우 20, label/l, 아이콘 24. */
    L,

    /**
     * 높이 48, radius 12, 좌우 20, label/m, 아이콘 20.
     * v2 디자인에는 없는 크기다. 쓰는 화면을 v2에 맞출 때 L이나 S로 옮기고, 다 옮기면 지운다.
     */
    M,

    /** 작은 버튼. 높이 32, radius 8, 좌우 12, label/m, 아이콘 16. 목록 행 안의 작은 행동용. */
    S,
}

/**
 * 냐미 기본 버튼. 그림자 없는 플랫 버튼이고, 눌리면 위계마다 정해진 눌림색으로 바뀌고 0.96배로 줄어든다.
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
            horizontalPadding = NyummyTheme.spacing.s20,
            iconSize = NyummyTheme.size.iconL,
            textStyle = NyummyTheme.typography.labelL,
        )
        NyummyButtonSize.M -> ButtonSizeSpec(
            height = NyummyTheme.size.buttonM,
            radius = NyummyTheme.radius.s,
            horizontalPadding = NyummyTheme.spacing.s20,
            iconSize = NyummyTheme.size.iconM,
            textStyle = NyummyTheme.typography.labelM,
        )
        NyummyButtonSize.S -> ButtonSizeSpec(
            height = NyummyComponentDimens.ButtonSmallHeight,
            radius = NyummyTheme.radius.xs,
            horizontalPadding = NyummyTheme.spacing.s12,
            iconSize = NyummyTheme.size.iconS,
            textStyle = NyummyTheme.typography.labelM,
        )
    }
}

private class ButtonColors(val container: Color, val content: Color)

@Composable
private fun buttonColors(style: NyummyButtonStyle, enabled: Boolean, pressed: Boolean): ButtonColors {
    val bg = NyummyTheme.colors.bg
    val content = NyummyTheme.colors.content
    if (!enabled) {
        val container = if (style == NyummyButtonStyle.Text) Color.Transparent else bg.actionDisabled
        return ButtonColors(container = container, content = content.disabled)
    }
    return when (style) {
        NyummyButtonStyle.Primary ->
            ButtonColors(if (pressed) bg.actionPrimaryPressed else bg.actionPrimary, content.onAction)
        NyummyButtonStyle.Tonal ->
            ButtonColors(if (pressed) bg.actionTonalPressed else bg.actionTonal, content.brand)
        NyummyButtonStyle.Neutral ->
            ButtonColors(if (pressed) bg.actionNeutralPressed else bg.actionNeutral, content.secondary)
        NyummyButtonStyle.Text ->
            ButtonColors(if (pressed) bg.surfaceSunken else Color.Transparent, content.secondary)
        NyummyButtonStyle.Danger ->
            ButtonColors(if (pressed) bg.actionDangerPressed else bg.actionDanger, content.onAction)
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
            NyummyButton(text = "사진 추가", onClick = {}, icon = R.drawable.nyummy_ic_image_plus, style = NyummyButtonStyle.Neutral)
        }
    }
}
