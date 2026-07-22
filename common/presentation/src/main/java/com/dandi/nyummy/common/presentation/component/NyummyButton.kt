package com.dandi.nyummy.common.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

enum class NyummyButtonStyle {
    Primary,
    Secondary,
    Danger,
    Reward,
}

enum class NyummyButtonSize {
    Medium,
    Large,
}

/**
 * Canonical Android button from Figma node `990:1052`.
 *
 * Width is intentionally controlled by [modifier]: the component is hug-content by default and
 * becomes the Figma `Full` variant when the caller supplies `Modifier.fillMaxWidth()`.
 */
@Composable
fun NyummyButton(
    label: String,
    modifier: Modifier = Modifier,
    style: NyummyButtonStyle = NyummyButtonStyle.Primary,
    size: NyummyButtonSize = NyummyButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    NyummyButtonCore(
        label = label,
        modifier = modifier,
        style = style,
        size = size,
        enabled = enabled,
        loading = loading,
        pressed = pressed,
        focused = focused,
        interactionSource = interactionSource,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onClick = onClick,
    )
}

@Composable
private fun NyummyButtonCore(
    label: String,
    modifier: Modifier,
    style: NyummyButtonStyle,
    size: NyummyButtonSize,
    enabled: Boolean,
    loading: Boolean,
    pressed: Boolean,
    focused: Boolean,
    interactionSource: MutableInteractionSource,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val colors = buttonColors(
        style = style,
        enabled = enabled,
        pressed = pressed,
        focused = focused,
    )
    val metrics = when (size) {
        NyummyButtonSize.Medium -> ButtonMetrics(
            height = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget,
            horizontalPadding = DesignSystemThemeImpl.designSystemSpacing.space16,
            iconSize = MediumButtonIconSize,
            textStyle = DesignSystemThemeImpl.typeScale.textStrongM,
        )

        NyummyButtonSize.Large -> ButtonMetrics(
            height = LargeButtonHeight,
            horizontalPadding = DesignSystemThemeImpl.designSystemSpacing.space20,
            iconSize = LargeButtonIconSize,
            textStyle = DesignSystemThemeImpl.typeScale.textStrongL,
        )
    }

    Surface(
        modifier = modifier
            .height(metrics.height)
            .defaultMinSize(minWidth = ButtonMinimumWidth)
            .semantics {
                if (loading) stateDescription = LoadingStateDescription
            }
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = DesignSystemThemeImpl.designSystemShape.buttonDefault,
        color = colors.container,
        contentColor = colors.content,
        border = colors.border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> ButtonLoadingIndicator(
                    size = metrics.iconSize,
                    color = colors.content,
                )

                leadingIcon != null -> Box(
                    modifier = Modifier.size(metrics.iconSize),
                    contentAlignment = Alignment.Center,
                    content = { leadingIcon() },
                )
            }
            if (loading || leadingIcon != null) {
                Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            }
            DandiText(
                text = label,
                color = colors.content,
                overflow = TextOverflow.Ellipsis,
                style = metrics.textStyle,
            )
            if (!loading && trailingIcon != null) {
                Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                Box(
                    modifier = Modifier.size(metrics.iconSize),
                    contentAlignment = Alignment.Center,
                    content = { trailingIcon() },
                )
            }
        }
    }
}

enum class NyummyIconButtonStyle {
    Ghost,
    Filled,
}

/** Canonical 48 dp icon button from Figma node `990:1079`. */
@Composable
fun NyummyIconButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: NyummyIconButtonStyle = NyummyIconButtonStyle.Ghost,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = iconButtonColors(
        style = style,
        enabled = enabled,
        pressed = pressed,
        focused = focused,
    )

    NyummyIconButtonCore(
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        onClick = onClick,
        icon = icon,
    )
}

@Composable
private fun NyummyIconButtonCore(
    contentDescription: String,
    modifier: Modifier,
    enabled: Boolean,
    colors: NyummyButtonColors,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = colors.container,
        contentColor = colors.content,
        border = colors.border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(IconButtonGlyphSize),
                contentAlignment = Alignment.Center,
                content = { icon() },
            )
        }
    }
}

@Composable
private fun ButtonLoadingIndicator(
    size: Dp,
    color: Color,
) {
    val transition = rememberInfiniteTransition(label = "NyummyButtonLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = FullRotation,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ButtonLoadingRotationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "NyummyButtonLoadingAngle",
    )
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    ) {
        drawArc(
            color = color,
            startAngle = LoadingStartAngle,
            sweepAngle = LoadingSweepAngle,
            useCenter = false,
            style = Stroke(width = ButtonProgressStroke.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun buttonColors(
    style: NyummyButtonStyle,
    enabled: Boolean,
    pressed: Boolean,
    focused: Boolean,
): NyummyButtonColors {
    val color = DesignSystemThemeImpl.designSystemColor
    if (!enabled) {
        return when (style) {
            NyummyButtonStyle.Primary -> NyummyButtonColors(
                container = color.bgActionPrimaryDisabled,
                content = color.contentActionDisabled,
            )

            NyummyButtonStyle.Secondary -> NyummyButtonColors(
                container = color.bgActionSecondaryDisabled,
                content = color.contentActionSecondaryDisabled,
                border = BorderStroke(ButtonBorderWidth, color.borderDefaultLevel1),
            )

            NyummyButtonStyle.Danger -> NyummyButtonColors(
                container = color.bgActionDangerDisabled,
                content = color.contentActionDangerDisabled,
            )

            NyummyButtonStyle.Reward -> NyummyButtonColors(
                container = color.bgActionPrimaryDisabled,
                content = color.contentActionDisabled,
            )
        }
    }

    val base = when (style) {
        NyummyButtonStyle.Primary -> NyummyButtonColors(
            container = if (pressed) color.bgActionPrimaryPressed else color.bgActionPrimaryDefault,
            content = color.contentActionPrimary,
        )

        NyummyButtonStyle.Secondary -> NyummyButtonColors(
            container = if (pressed) color.bgActionSecondaryPressed else color.bgActionSecondaryDefault,
            content = color.contentActionSecondary,
            border = BorderStroke(ButtonBorderWidth, color.borderActionSecondary),
        )

        NyummyButtonStyle.Danger -> NyummyButtonColors(
            container = if (pressed) color.bgActionDangerPressed else color.bgActionDangerDefault,
            content = color.contentActionDanger,
        )

        NyummyButtonStyle.Reward -> NyummyButtonColors(
            container = if (pressed) color.bgActionRewardPressed else color.bgActionRewardDefault,
            content = color.contentActionReward,
        )
    }

    if (!focused) return base
    val focusColor = when (style) {
        NyummyButtonStyle.Primary -> color.contentActionPrimary
        NyummyButtonStyle.Secondary,
        NyummyButtonStyle.Reward,
        -> color.contentDefaultLevel0

        NyummyButtonStyle.Danger -> color.contentError
    }
    return base.copy(border = BorderStroke(ButtonFocusBorderWidth, focusColor))
}

@Composable
private fun iconButtonColors(
    style: NyummyIconButtonStyle,
    enabled: Boolean,
    pressed: Boolean,
    focused: Boolean,
): NyummyButtonColors {
    val color = DesignSystemThemeImpl.designSystemColor
    val container = when {
        style == NyummyIconButtonStyle.Ghost -> color.bgDefaultLevel0.copy(alpha = 0f)
        !enabled -> color.bgActionSecondaryDisabled
        pressed -> color.bgActionSecondaryPressed
        else -> color.bgActionSecondaryDefault
    }
    val content = if (enabled) color.contentIconLevel0 else color.contentDefaultLevel3
    val border = if (enabled && focused) {
        BorderStroke(ButtonFocusBorderWidth, color.contentDefaultLevel0)
    } else {
        null
    }
    return NyummyButtonColors(container = container, content = content, border = border)
}

private data class NyummyButtonColors(
    val container: Color,
    val content: Color,
    val border: BorderStroke? = null,
)

private data class ButtonMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val textStyle: androidx.compose.ui.text.TextStyle,
)

private const val ButtonLoadingRotationMillis = 900
private const val FullRotation = 360f
private const val LoadingStartAngle = -90f
private const val LoadingSweepAngle = 270f
private const val LoadingStateDescription = "로딩 중"
private val LargeButtonHeight = 56.dp
private val ButtonMinimumWidth = 96.dp
private val MediumButtonIconSize = 18.dp
private val LargeButtonIconSize = 20.dp
private val IconButtonGlyphSize = 24.dp
private val ButtonProgressStroke = 2.dp
private val ButtonBorderWidth = 1.dp
private val ButtonFocusBorderWidth = 2.dp

@Preview(showBackground = true, widthDp = 660, heightDp = 360)
@Composable
private fun NyummyButtonMatrixPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
        ) {
            NyummyButtonStyle.entries.forEach { style ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    PreviewButton(style = style, label = "Default")
                    PreviewButton(style = style, label = "Pressed", pressed = true)
                    PreviewButton(style = style, label = "Focused", focused = true)
                    PreviewButton(style = style, label = "Disabled", enabled = false)
                    PreviewButton(style = style, label = "Loading", loading = true)
                }
            }
            NyummyButton(
                label = "Large",
                size = NyummyButtonSize.Large,
                onClick = {},
            )
        }
    }
}

@Composable
private fun PreviewButton(
    style: NyummyButtonStyle,
    label: String,
    pressed: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    NyummyButtonCore(
        label = label,
        modifier = Modifier,
        style = style,
        size = NyummyButtonSize.Medium,
        enabled = enabled,
        loading = loading,
        pressed = pressed,
        focused = focused,
        interactionSource = remember { MutableInteractionSource() },
        leadingIcon = null,
        trailingIcon = null,
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 260, heightDp = 130)
@Composable
private fun NyummyIconButtonMatrixPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
        ) {
            NyummyIconButtonStyle.entries.forEach { style ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    PreviewIconButton(style = style)
                    PreviewIconButton(style = style, pressed = true)
                    PreviewIconButton(style = style, focused = true)
                    PreviewIconButton(style = style, enabled = false)
                }
            }
        }
    }
}

@Composable
private fun PreviewIconButton(
    style: NyummyIconButtonStyle,
    enabled: Boolean = true,
    pressed: Boolean = false,
    focused: Boolean = false,
) {
    NyummyIconButtonCore(
        contentDescription = "미리보기 아이콘",
        modifier = Modifier,
        enabled = enabled,
        colors = iconButtonColors(
            style = style,
            enabled = enabled,
            pressed = pressed,
            focused = focused,
        ),
        interactionSource = remember { MutableInteractionSource() },
        onClick = {},
    ) {
        val glyphColor = LocalContentColor.current
        Canvas(Modifier.size(IconPreviewGlyphSize)) {
            drawCircle(color = glyphColor)
        }
    }
}

private val IconPreviewGlyphSize = 12.dp
