package com.dandi.nyummy.common.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.dandi.nyummy.common.presentation.ui.color.DesignSystemSemanticColors
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemColor
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemEffects
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemElevation
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemLayout
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemRadius
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemShape
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemSize
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemSpacing
import com.dandi.nyummy.common.presentation.ui.token.DefaultDesignSystemStaticTypeScale
import com.dandi.nyummy.common.presentation.ui.typo.DesignSystemTypeScale

interface DesignSystemTheme {
    val designSystemColor: DesignSystemSemanticColors @Composable get
    val typeScale: DesignSystemTypeScale @Composable get
    val designSystemRadius: DesignSystemRadius @Composable get
    val designSystemSpacing: DesignSystemSpacing @Composable get
    val designSystemSize: DesignSystemSize @Composable get
    val designSystemLayout: DesignSystemLayout @Composable get
    val designSystemShape: DesignSystemShape @Composable get
    val designSystemElevation: DesignSystemElevation @Composable get
    val designSystemEffects: DesignSystemEffects @Composable get
}

/** Nyummy v1 is intentionally Light-only; system dark mode does not alter this theme. */
@Composable
fun DesignSystemTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDesignSystemColor provides DefaultDesignSystemColor,
        LocalDesignSystemTypeScale provides DefaultDesignSystemStaticTypeScale,
        LocalDesignSystemRadius provides DefaultDesignSystemRadius,
        LocalDesignSystemSpacing provides DefaultDesignSystemSpacing,
        LocalDesignSystemSize provides DefaultDesignSystemSize,
        LocalDesignSystemLayout provides DefaultDesignSystemLayout,
        LocalDesignSystemShape provides DefaultDesignSystemShape,
        LocalDesignSystemElevation provides DefaultDesignSystemElevation,
        LocalDesignSystemEffects provides DefaultDesignSystemEffects,
    ) {
        MaterialTheme(
            colorScheme = DesignSystemLightColorScheme,
            typography = DesignSystemMaterialTypography,
            content = content,
        )
    }
}

private val DesignSystemLightColorScheme = lightColorScheme(
    primary = DefaultDesignSystemColor.bgActionPrimaryDefault,
    onPrimary = DefaultDesignSystemColor.contentActionPrimary,
    primaryContainer = DefaultDesignSystemColor.bgSelectionPrimary,
    onPrimaryContainer = DefaultDesignSystemColor.contentSelectionPrimary,
    secondary = DefaultDesignSystemColor.bgActionSecondaryDefault,
    onSecondary = DefaultDesignSystemColor.contentActionSecondary,
    error = DefaultDesignSystemColor.bgDangerDefault,
    onError = DefaultDesignSystemColor.contentInverseDefault,
    errorContainer = DefaultDesignSystemColor.bgDangerSoft,
    onErrorContainer = DefaultDesignSystemColor.contentError,
    background = DefaultDesignSystemColor.bgDefaultLevel0,
    onBackground = DefaultDesignSystemColor.contentDefaultLevel0,
    surface = DefaultDesignSystemColor.bgDefaultLevel1,
    onSurface = DefaultDesignSystemColor.contentDefaultLevel0,
    surfaceVariant = DefaultDesignSystemColor.bgSurfaceSubtle,
    onSurfaceVariant = DefaultDesignSystemColor.contentDefaultLevel2,
    surfaceContainerLowest = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceContainerLow = DefaultDesignSystemColor.bgSurfaceSubtle,
    surfaceContainer = DefaultDesignSystemColor.bgDefaultLevel0,
    surfaceContainerHigh = DefaultDesignSystemColor.bgDefaultLevel2,
    surfaceContainerHighest = DefaultDesignSystemColor.bgSurfaceStrong,
    surfaceBright = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceDim = DefaultDesignSystemColor.bgDefaultLevel2,
    inverseSurface = DefaultDesignSystemColor.bgSurfaceInverse,
    inverseOnSurface = DefaultDesignSystemColor.contentInverseDefault,
    outline = DefaultDesignSystemColor.borderDefaultLevel1,
    outlineVariant = DefaultDesignSystemColor.borderDefaultLevel2,
    scrim = DefaultDesignSystemColor.bgScrimDefault,
)

private val DesignSystemMaterialTypography = Typography(
    displayLarge = DefaultDesignSystemStaticTypeScale.displayRegularXXL,
    displayMedium = DefaultDesignSystemStaticTypeScale.displayRegularXL,
    displaySmall = DefaultDesignSystemStaticTypeScale.displayRegularL,
    headlineLarge = DefaultDesignSystemStaticTypeScale.displayStrongXL,
    headlineMedium = DefaultDesignSystemStaticTypeScale.displayStrongL,
    headlineSmall = DefaultDesignSystemStaticTypeScale.displayStrongM,
    titleLarge = DefaultDesignSystemStaticTypeScale.titleStrongL,
    titleMedium = DefaultDesignSystemStaticTypeScale.textStrongXL,
    titleSmall = DefaultDesignSystemStaticTypeScale.textStrongL,
    bodyLarge = DefaultDesignSystemStaticTypeScale.textRegularL,
    bodyMedium = DefaultDesignSystemStaticTypeScale.textRegularM,
    bodySmall = DefaultDesignSystemStaticTypeScale.textRegularS,
    labelLarge = DefaultDesignSystemStaticTypeScale.textStrongM,
    labelMedium = DefaultDesignSystemStaticTypeScale.labelStrongS,
    labelSmall = DefaultDesignSystemStaticTypeScale.labelRegularXS,
)

internal val LocalDesignSystemColor = staticCompositionLocalOf { DefaultDesignSystemColor }
internal val LocalDesignSystemTypeScale = staticCompositionLocalOf { DefaultDesignSystemStaticTypeScale }
internal val LocalDesignSystemRadius = staticCompositionLocalOf { DefaultDesignSystemRadius }
internal val LocalDesignSystemSpacing = staticCompositionLocalOf { DefaultDesignSystemSpacing }
internal val LocalDesignSystemSize = staticCompositionLocalOf { DefaultDesignSystemSize }
internal val LocalDesignSystemLayout = staticCompositionLocalOf { DefaultDesignSystemLayout }
internal val LocalDesignSystemShape = staticCompositionLocalOf { DefaultDesignSystemShape }
internal val LocalDesignSystemElevation = staticCompositionLocalOf { DefaultDesignSystemElevation }
internal val LocalDesignSystemEffects = staticCompositionLocalOf { DefaultDesignSystemEffects }

object DesignSystemThemeImpl : DesignSystemTheme {
    override val designSystemColor: DesignSystemSemanticColors @Composable @ReadOnlyComposable get() = LocalDesignSystemColor.current
    override val typeScale: DesignSystemTypeScale @Composable @ReadOnlyComposable get() = LocalDesignSystemTypeScale.current
    override val designSystemRadius: DesignSystemRadius @Composable @ReadOnlyComposable get() = LocalDesignSystemRadius.current
    override val designSystemSpacing: DesignSystemSpacing @Composable @ReadOnlyComposable get() = LocalDesignSystemSpacing.current
    override val designSystemSize: DesignSystemSize @Composable @ReadOnlyComposable get() = LocalDesignSystemSize.current
    override val designSystemLayout: DesignSystemLayout @Composable @ReadOnlyComposable get() = LocalDesignSystemLayout.current
    override val designSystemShape: DesignSystemShape @Composable @ReadOnlyComposable get() = LocalDesignSystemShape.current
    override val designSystemElevation: DesignSystemElevation @Composable @ReadOnlyComposable get() = LocalDesignSystemElevation.current
    override val designSystemEffects: DesignSystemEffects @Composable @ReadOnlyComposable get() = LocalDesignSystemEffects.current
}
