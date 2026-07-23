package com.dandi.nyummy.common.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow

enum class NyummyBottomNavigationStyle {
    Compact,
    Floating,
    FullWidth,
}

enum class NyummyNavigationDestination(
    val label: String,
    internal val icon: ImageVector,
) {
    Home("홈", NyummyNavigationIcons.Home),
    History("히스토리", NyummyNavigationIcons.History),
    Quest("퀘스트", NyummyNavigationIcons.Quest),
    Collection("컬렉션", NyummyNavigationIcons.Collection),
    Shop("상점", NyummyNavigationIcons.Shop),
}

/**
 * Canonical rc.3 five-destination navigation.
 *
 * Compact and Floating occupy the 370dp inset width from the 390dp reference viewport;
 * FullWidth consumes its parent width. The selected destination is the only state input so an
 * arbitrary tab order or an unsupported destination cannot accidentally drift from Figma.
 */
@Composable
fun NyummyBottomNavigation(
    selectedDestination: NyummyNavigationDestination,
    modifier: Modifier = Modifier,
    style: NyummyBottomNavigationStyle = NyummyBottomNavigationStyle.Floating,
    onDestinationSelected: (NyummyNavigationDestination) -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = when (style) {
        NyummyBottomNavigationStyle.Compact,
        NyummyBottomNavigationStyle.Floating,
        -> RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius22)

        NyummyBottomNavigationStyle.FullWidth -> RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius0)
    }
    val containerColor = when (style) {
        NyummyBottomNavigationStyle.FullWidth -> colors.bgNavigationFullWidth
        NyummyBottomNavigationStyle.Compact,
        NyummyBottomNavigationStyle.Floating,
        -> colors.bgNavigationFloating
    }
    val height = when (style) {
        NyummyBottomNavigationStyle.Compact -> DesignSystemThemeImpl.designSystemSize.bottomNavigationCompact
        NyummyBottomNavigationStyle.Floating -> DesignSystemThemeImpl.designSystemSize.bottomNavigationFloating
        NyummyBottomNavigationStyle.FullWidth -> DesignSystemThemeImpl.designSystemSize.bottomNavigationFullWidth
    }
    val horizontalInset = when (style) {
        NyummyBottomNavigationStyle.FullWidth -> FullWidthNavigationInset
        NyummyBottomNavigationStyle.Compact,
        NyummyBottomNavigationStyle.Floating,
        -> InsetNavigationInset
    }
    val verticalInset = when (style) {
        NyummyBottomNavigationStyle.Compact -> CompactNavigationInset
        NyummyBottomNavigationStyle.Floating -> FloatingNavigationInset
        NyummyBottomNavigationStyle.FullWidth -> FullWidthNavigationVerticalInset
    }
    val selectedIndex = NyummyNavigationDestination.entries.indexOf(selectedDestination)
    val indicatorOffset by animateDpAsState(
        targetValue = NavigationIndicatorHorizontalOffset + NavigationItemWidth * selectedIndex,
        animationSpec = tween(
            durationMillis = NavigationIndicatorMotionDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "NyummyBottomNavigationIndicatorOffset",
    )
    val sizedModifier = when (style) {
        NyummyBottomNavigationStyle.FullWidth -> Modifier.fillMaxWidth()
        NyummyBottomNavigationStyle.Compact,
        NyummyBottomNavigationStyle.Floating,
        -> Modifier
            .widthIn(max = InsetNavigationWidth)
            .fillMaxWidth()
    }.height(height)
    val effectModifier = if (style == NyummyBottomNavigationStyle.Floating) {
        modifier.designSystemDropShadow(
            shape = shape,
            shadow = DesignSystemThemeImpl.designSystemElevation.navigationFloating,
        )
    } else {
        modifier
    }
    val dividerColor = colors.borderNavigationFullWidthDivider
    val fullWidthDividerModifier = if (style == NyummyBottomNavigationStyle.FullWidth) {
        Modifier.drawBehind {
            val dividerY = NavigationBorderWidth.toPx() / 2f
            drawLine(
                color = dividerColor,
                start = androidx.compose.ui.geometry.Offset(0f, dividerY),
                end = androidx.compose.ui.geometry.Offset(size.width, dividerY),
                strokeWidth = NavigationBorderWidth.toPx(),
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = effectModifier
            .then(sizedModifier)
            .then(fullWidthDividerModifier),
        shape = shape,
        color = containerColor,
        contentColor = colors.contentIconLevel0,
        border = if (style == NyummyBottomNavigationStyle.Compact) {
            BorderStroke(NavigationBorderWidth, colors.borderNavigationDefault)
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = horizontalInset, y = verticalInset),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(NavigationIndicatorWidth, NavigationIndicatorHeight)
                    .background(
                        color = colors.bgBrandSoft,
                        shape = DesignSystemThemeImpl.designSystemShape.pill,
                    )
                    .testTag(NavigationIndicatorTestTag),
            )
            Row(
                modifier = Modifier.selectableGroup(),
            ) {
                NyummyNavigationDestination.entries.forEach { destination ->
                    NyummyBottomNavigationItem(
                        destination = destination,
                        selected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NyummyBottomNavigationItem(
    destination: NyummyNavigationDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val iconColor = colors.contentIconLevel0
    val labelColor = if (selected) colors.contentAccent else colors.contentDefaultLevel1
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .size(NavigationItemWidth, NavigationItemHeight)
            .clip(DesignSystemThemeImpl.designSystemShape.pill)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Tab,
            )
            .testTag(NavigationItemTestTagPrefix + destination.name)
            .semantics { contentDescription = destination.label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(NavigationIndicatorWidth, NavigationIndicatorHeight)
                .clip(DesignSystemThemeImpl.designSystemShape.pill)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = false,
                        radius = NavigationPressIndicationRadius,
                        color = iconColor,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier.size(NavigationIconSize),
                tint = iconColor,
            )
        }
        Box(
            modifier = Modifier
                .width(NavigationItemWidth)
                .height(NavigationLabelAreaHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            DandiText(
                text = destination.label,
                modifier = Modifier.fillMaxWidth(),
                color = labelColor,
                style = if (selected) {
                    DesignSystemThemeImpl.typeScale.labelStrongXS
                } else {
                    DesignSystemThemeImpl.typeScale.labelRegularXS
                },
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/** Home quick action mapped to LIVE / Component / Home / Floating Today Meals. */
@Composable
fun NyummyFloatingTodayMeals(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20)

    Surface(
        onClick = onClick,
        modifier = modifier
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.floatingAction,
            )
            .size(FloatingMealsWidth, FloatingMealsHeight),
        shape = shape,
        color = colors.bgSurfaceIvory,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(FloatingMealsBorderWidth, colors.borderCardSubtle),
    ) {
        Box(Modifier.size(FloatingMealsWidth, FloatingMealsHeight)) {
            Box(
                modifier = Modifier
                    .offset(FloatingMealsIconOffset, FloatingMealsIconOffset)
                    .size(FloatingMealsIconSize),
                contentAlignment = Alignment.Center,
            ) {
                if (leadingIcon == null) {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.nyummy_food_salad),
                        contentDescription = null,
                        modifier = Modifier.size(FloatingMealsIconSize),
                        filterQuality = FilterQuality.None,
                    )
                } else {
                    leadingIcon()
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = FloatingMealsLabelOffset)
                    .width(FloatingMealsLabelWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                DandiText(
                    text = label,
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .offset(x = FloatingMealsDotOffsetX, y = FloatingMealsDotOffsetY)
                    .size(FloatingMealsDotSize),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = colors.dataCalendarToday,
                border = BorderStroke(FloatingMealsDotBorderWidth, colors.borderCardSubtle),
            ) {}
        }
    }
}

private object NyummyNavigationIcons {
    // Icon applies the semantic tint at render time; black supplies only the vector alpha mask.
    private val NavStroke = SolidColor(Color.Black)
    private const val NavStrokeWidth = 2f

    val Home: ImageVector = navVector {
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 10.5f)
            lineTo(12f, 3f)
            lineTo(21f, 10.5f)
            lineTo(21f, 21f)
            lineTo(15f, 21f)
            lineTo(15f, 15f)
            lineTo(9f, 15f)
            lineTo(9f, 21f)
            lineTo(3f, 21f)
            close()
        }
    }

    val History: ImageVector = navVector {
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(6f, 4f)
            lineTo(18f, 4f)
            curveTo(19.6569f, 4f, 21f, 5.3431f, 21f, 7f)
            lineTo(21f, 18f)
            curveTo(21f, 19.6569f, 19.6569f, 21f, 18f, 21f)
            lineTo(6f, 21f)
            curveTo(4.3431f, 21f, 3f, 19.6569f, 3f, 18f)
            lineTo(3f, 7f)
            curveTo(3f, 5.3431f, 4.3431f, 4f, 6f, 4f)
            close()
        }
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8f, 2f)
            lineTo(8f, 6f)
            moveTo(16f, 2f)
            lineTo(16f, 6f)
            moveTo(3f, 9f)
            lineTo(21f, 9f)
            moveTo(7f, 13f)
            lineTo(10f, 13f)
            moveTo(14f, 13f)
            lineTo(17f, 13f)
            moveTo(7f, 17f)
            lineTo(10f, 17f)
        }
    }

    val Quest: ImageVector = navVector {
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(21f, 12f)
            curveTo(21f, 16.9706f, 16.9706f, 21f, 12f, 21f)
            curveTo(7.0294f, 21f, 3f, 16.9706f, 3f, 12f)
            curveTo(3f, 7.0294f, 7.0294f, 3f, 12f, 3f)
            curveTo(16.9706f, 3f, 21f, 7.0294f, 21f, 12f)
            close()
        }
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8f, 12f)
            lineTo(10.5f, 14.5f)
            lineTo(16f, 9f)
        }
    }

    val Collection: ImageVector = navVector {
        roundedCollectionSquare(3f, 3f)
        roundedCollectionSquare(14f, 3f)
        roundedCollectionSquare(3f, 14f)
        roundedCollectionSquare(14f, 14f)
    }

    val Shop: ImageVector = navVector {
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(5f, 8f)
            lineTo(19f, 8f)
            lineTo(18f, 21f)
            lineTo(6f, 21f)
            close()
        }
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9f, 10f)
            lineTo(9f, 6f)
            curveTo(9f, 4.3431f, 10.3431f, 3f, 12f, 3f)
            curveTo(13.6569f, 3f, 15f, 4.3431f, 15f, 6f)
            lineTo(15f, 10f)
        }
    }

    private fun ImageVector.Builder.roundedCollectionSquare(x: Float, y: Float) {
        path(stroke = NavStroke, fill = null, strokeLineWidth = NavStrokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(x + 2f, y)
            lineTo(x + 5f, y)
            curveTo(x + 6.1046f, y, x + 7f, y + 0.8954f, x + 7f, y + 2f)
            lineTo(x + 7f, y + 5f)
            curveTo(x + 7f, y + 6.1046f, x + 6.1046f, y + 7f, x + 5f, y + 7f)
            lineTo(x + 2f, y + 7f)
            curveTo(x + 0.8954f, y + 7f, x, y + 6.1046f, x, y + 5f)
            lineTo(x, y + 2f)
            curveTo(x, y + 0.8954f, x + 0.8954f, y, x + 2f, y)
            close()
        }
    }

    private fun navVector(content: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            defaultWidth = NavigationIconSize,
            defaultHeight = NavigationIconSize,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(content).build()

}

private val NavigationBorderWidth = 1.dp
private val InsetNavigationWidth = 370.dp
private val InsetNavigationInset = 10.dp
private val FullWidthNavigationInset = 20.dp
private val CompactNavigationInset = 4.dp
private val FloatingNavigationInset = 8.dp
private val FullWidthNavigationVerticalInset = 10.dp
private val NavigationItemWidth = 70.dp
private val NavigationItemHeight = 58.dp
private val NavigationIndicatorWidth = 44.dp
private val NavigationIndicatorHeight = 30.dp
private val NavigationIndicatorHorizontalOffset = 13.dp
private val NavigationPressIndicationRadius = 28.dp
private val NavigationLabelAreaHeight = 24.dp
private val NavigationIconSize = 24.dp
private const val NavigationIndicatorMotionDurationMillis = 280
private const val NavigationIndicatorTestTag = "nyummy_bottom_navigation_indicator"
private const val NavigationItemTestTagPrefix = "nyummy_bottom_navigation_item_"
private val FloatingMealsWidth = 122.dp
private val FloatingMealsHeight = 56.dp
private val FloatingMealsBorderWidth = 1.dp
private val FloatingMealsIconOffset = 9.dp
private val FloatingMealsIconSize = 38.dp
private val FloatingMealsLabelOffset = 52.dp
private val FloatingMealsLabelWidth = 60.dp
private val FloatingMealsDotOffsetX = 106.dp
private val FloatingMealsDotOffsetY = 8.dp
private val FloatingMealsDotSize = 8.dp
private val FloatingMealsDotBorderWidth = 2.dp

@Preview(widthDp = 390, showBackground = true)
@Composable
private fun NyummyBottomNavigationPreview() {
    DesignSystemTheme {
        Box(Modifier.width(390.dp), contentAlignment = Alignment.Center) {
            NyummyBottomNavigation(selectedDestination = NyummyNavigationDestination.History) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyFloatingTodayMealsPreview() {
    DesignSystemTheme {
        NyummyFloatingTodayMeals(label = "오늘 현황", onClick = {})
    }
}
