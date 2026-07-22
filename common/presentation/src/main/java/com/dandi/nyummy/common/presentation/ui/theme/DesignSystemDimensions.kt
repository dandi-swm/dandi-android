package com.dandi.nyummy.common.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

@ConsistentCopyVisibility
@Immutable
data class DesignSystemRadius internal constructor(
    val radius0: Dp,
    val radius8: Dp,
    val radius12: Dp,
    val radius16: Dp,
    val radius20: Dp,
    val radius24: Dp,
    val radius32: Dp,
    val radiusFull: Dp,
    val radius22: Dp,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemSpacing internal constructor(
    val space4: Dp,
    val space8: Dp,
    val space12: Dp,
    val space16: Dp,
    val space20: Dp,
    val space24: Dp,
    val space32: Dp,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemSize internal constructor(
    val characterPixelMaster: Dp,
    val characterHome: Dp,
    val character3dHero: Dp,
    val bottomNavigation: Dp,
    val bottomNavigationCompact: Dp,
    val bottomNavigationFloating: Dp,
    val bottomNavigationFullWidth: Dp,
    val calendarFoodIcon: Dp,
    val calendarStatusMarker: Dp,
    val minimumTouchTarget: Dp,
    val mealRowMinHeight: Dp,
    val mealLeadingIcon: Dp,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemLayout internal constructor(
    val mobileGutter: Dp,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemShape internal constructor(
    val buttonDefault: Shape,
    val inputDefault: Shape,
    val calendarDay: Shape,
    val cardDefault: Shape,
    val coachBubble: Shape,
    val dialogDefault: Shape,
    val sheetDefault: Shape,
    val navigationFloating: Shape,
    val pill: Shape,
    val progress: Shape,
    val handle: Shape,
    val toast: Shape,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemShadow internal constructor(
    val color: Color,
    val blurRadius: Dp,
    val spreadRadius: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemElevation internal constructor(
    val navigationFloating: DesignSystemShadow,
    val surfaceLow: DesignSystemShadow,
    val dialogStandard: DesignSystemShadow,
    val sheetStandard: DesignSystemShadow,
    val floatingAction: DesignSystemShadow,
)

@ConsistentCopyVisibility
@Immutable
data class DesignSystemEffects internal constructor(
    val modalBackgroundBlur: Dp,
)

/**
 * Renders a Figma drop-shadow effect without converting its blur radius into Material elevation.
 *
 * Figma and Compose 1.11 share radius, spread, offset, and color primitives, so keeping this
 * adapter centralized avoids the lossy `Modifier.shadow(blur / 2)` approximation.
 */
@Stable
fun Modifier.designSystemDropShadow(
    shape: Shape,
    shadow: DesignSystemShadow,
): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = shadow.blurRadius,
        spread = shadow.spreadRadius,
        offset = DpOffset(shadow.offsetX, shadow.offsetY),
        color = shadow.color,
    ),
)
