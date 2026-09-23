package com.dandi.nyummy.common.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/**
 * 하단 내비게이션의 배치 형태.
 *
 * - [Compact]: 외곽선이 있는 기본 형태
 * - [Floating]: 화면 위에 떠 있는 것처럼 그림자가 적용된 형태
 * - [FullWidth]: 부모 영역의 가로 폭을 모두 사용하는 형태
 */
enum class NyummyBottomNavigationStyle {
    Compact,
    Floating,
    FullWidth,
}

/**
 * 하단 내비게이션에 표시되는 화면 목록.
 *
 * 각 항목은 사용자에게 보여 줄 [label]과 아이콘 리소스인 [iconRes]를 가진다.
 * 선언된 순서는 실제 하단 내비게이션의 표시 순서로 사용되며,
 * 가운데(두 번째와 세 번째 사이)에는 식사 기록용 카메라 버튼 슬롯이 끼어든다.
 */
enum class NyummyNavigationDestination(
    val label: String,
    @DrawableRes internal val iconRes: Int,
) {
    Home("홈", R.drawable.nyummy_navigation_home),
    History("기록", R.drawable.nyummy_navigation_history),
    Collection("컬렉션", R.drawable.nyummy_navigation_collection),
    Shop("상점", R.drawable.nyummy_navigation_shop),
}

/**
 * 앱의 주요 화면으로 이동할 때 사용하는 하단 내비게이션.
 *
 * [NyummyNavigationDestination]에 정의된 4개 탭을 순서대로 표시하고,
 * 중앙에는 식사 기록으로 진입하는 원형 카메라 버튼을 바 위로 살짝 띄워 배치한다.
 * 카메라 버튼은 탭(선택 상태)이 아니라 액션이므로 [onCameraClick]으로만 전달된다.
 *
 * 화면 이동 상태는 호출부에서 관리한다. 사용자가 항목을 누르면
 * [onDestinationSelected]로 선택 결과를 전달하므로, 호출부에서 해당 화면으로 이동하면 된다.
 * 배치 형태는 [style]로 지정할 수 있다.
 */
@Composable
fun NyummyBottomNavigation(
    selectedDestination: NyummyNavigationDestination,
    onCameraClick: () -> Unit,
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
    val sizedModifier = when (style) {
        NyummyBottomNavigationStyle.FullWidth -> Modifier.fillMaxWidth()
        NyummyBottomNavigationStyle.Compact,
        NyummyBottomNavigationStyle.Floating,
        -> Modifier
            .widthIn(max = InsetNavigationWidth)
            .fillMaxWidth()
    }
    val barShadowModifier = if (style == NyummyBottomNavigationStyle.Floating) {
        Modifier.designSystemDropShadow(
            shape = shape,
            shadow = DesignSystemThemeImpl.designSystemElevation.navigationFloating,
        )
    } else {
        Modifier
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

    Box(
        modifier = modifier
            .then(sizedModifier)
            .height(height + CameraButtonProtrusion),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height)
                .then(barShadowModifier)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .selectableGroup(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyNavigationDestination.entries.forEachIndexed { index, destination ->
                    if (index == CameraSlotIndex) {
                        Spacer(modifier = Modifier.width(CameraSlotWidth))
                    }
                    NyummyBottomNavigationItem(
                        destination = destination,
                        selected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
        NyummyNavigationCameraButton(
            onClick = onCameraClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.contentAccent else colors.contentIconLevel1,
        animationSpec = tween(
            durationMillis = NavigationSelectionMotionDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "NyummyBottomNavigationIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.contentAccent else colors.contentDefaultLevel1,
        animationSpec = tween(
            durationMillis = NavigationSelectionMotionDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "NyummyBottomNavigationLabelColor",
    )
    val pillProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(
            durationMillis = NavigationSelectionMotionDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "NyummyBottomNavigationPillProgress",
    )
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = pillProgress
                        scaleX = NavigationPillMinScale + (1f - NavigationPillMinScale) * pillProgress
                        scaleY = NavigationPillMinScale + (1f - NavigationPillMinScale) * pillProgress
                    }
                    .background(
                        color = colors.bgBrandSoft,
                        shape = DesignSystemThemeImpl.designSystemShape.pill,
                    )
                    .then(
                        if (selected) Modifier.testTag(NavigationIndicatorTestTag) else Modifier,
                    ),
            )
            Icon(
                painter = painterResource(destination.iconRes),
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

/**
 * 하단 내비게이션 중앙의 식사 기록 진입 버튼.
 *
 * 탭이 아니라 액션이므로 선택 상태를 갖지 않고, 바 위로 [CameraButtonProtrusion]만큼 돌출된다.
 */
@Composable
private fun NyummyNavigationCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) CameraButtonPressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "NyummyNavigationCameraButtonScale",
    )
    val cameraDescription = stringResource(R.string.nav_camera_button)

    Box(
        modifier = modifier
            .size(CameraButtonSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .designSystemDropShadow(
                shape = CircleShape,
                shadow = DesignSystemThemeImpl.designSystemElevation.floatingAction,
            )
            .clip(CircleShape)
            .background(colors.bgActionPrimaryDefault)
            .border(CameraButtonRingWidth, colors.bgNavigationFloating, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(NavigationCameraButtonTestTag)
            .semantics { contentDescription = cameraDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.nyummy_navigation_camera),
            contentDescription = null,
            modifier = Modifier.size(CameraButtonIconSize),
            tint = colors.contentInverseDefault,
        )
    }
}

/**
 * 홈 화면에서 오늘의 식사 현황으로 이동할 때 사용하는 플로팅 버튼.
 *
 * [leadingIcon]을 전달하면 라벨 앞에 아이콘을 표시하며, 버튼을 누르면 [onClick]을 호출한다.
 */
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
                leadingIcon?.invoke()
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

private val NavigationBorderWidth = 1.dp
private val InsetNavigationWidth = 370.dp
private val NavigationItemWidth = 70.dp
private val NavigationItemHeight = 58.dp
private val NavigationIndicatorWidth = 44.dp
private val NavigationIndicatorHeight = 30.dp
private val NavigationPressIndicationRadius = 28.dp
private val NavigationLabelAreaHeight = 24.dp
private val NavigationIconSize = 24.dp
private const val NavigationSelectionMotionDurationMillis = 160
private const val NavigationPillMinScale = 0.8f
private const val NavigationIndicatorTestTag = "nyummy_bottom_navigation_indicator"
private const val NavigationItemTestTagPrefix = "nyummy_bottom_navigation_item_"
private const val NavigationCameraButtonTestTag = "nyummy_bottom_navigation_camera"
private const val CameraSlotIndex = 2
private val CameraSlotWidth = 72.dp
private val CameraButtonSize = 64.dp
private val CameraButtonIconSize = 28.dp
private val CameraButtonRingWidth = 4.dp
private val CameraButtonProtrusion = 20.dp
private const val CameraButtonPressedScale = 0.94f
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

@Preview(widthDp = 390, heightDp = 120, showBackground = true)
@Composable
private fun NyummyBottomNavigationPreview() {
    DesignSystemTheme {
        Box(Modifier.width(390.dp), contentAlignment = Alignment.Center) {
            NyummyBottomNavigation(
                selectedDestination = NyummyNavigationDestination.History,
                onCameraClick = {},
            ) {}
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
