package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.dandi.nyummy.common.presentation.designsystem.foundation.LocalNyummySingleClick
import com.dandi.nyummy.common.presentation.designsystem.foundation.collectNyummySingleClicks
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummySingleClickFlow

/**
 * 냐미 디자인 시스템 진입점. 화면과 컴포넌트는 `NyummyTheme.colors.bg.canvas`처럼 여기서만 토큰을 읽는다.
 *
 * Light 전용이다. CompositionLocal 기본값이 곧 토큰 값이라 [NyummyTheme] Composable로 감싸지 않은
 * 프리뷰에서도 같은 값을 읽는다.
 */
object NyummyTheme {
    val colors: NyummyColors
        @Composable @ReadOnlyComposable get() = LocalNyummyColors.current

    val typography: NyummyTypography
        @Composable @ReadOnlyComposable get() = LocalNyummyTypography.current

    val spacing: NyummySpacing
        @Composable @ReadOnlyComposable get() = LocalNyummySpacing.current

    val radius: NyummyRadius
        @Composable @ReadOnlyComposable get() = LocalNyummyRadius.current

    val borderWidth: NyummyBorderWidth
        @Composable @ReadOnlyComposable get() = LocalNyummyBorderWidth.current

    val size: NyummySize
        @Composable @ReadOnlyComposable get() = LocalNyummySize.current

    val elevation: NyummyElevation
        @Composable @ReadOnlyComposable get() = LocalNyummyElevation.current
}

/**
 * @param singleClick 따닥 누름 방지 통로([LocalNyummySingleClick])를 내려 주고 들을지. 앱 화면은 기본값(true)을 쓴다.
 *   서로 다른 행동 버튼을 쉬지 않고 연달아 누르는 화면 테스트만 false로 끈다. 겹친 안쪽 테마가 false면 그 아래는 꺼진다
 */
@Composable
fun NyummyTheme(singleClick: Boolean = true, content: @Composable () -> Unit) {
    // remember로 붙잡지 않으면 재구성 때마다 통로가 새로 생겨 듣는 곳과 어긋난다.
    val clicks = remember { nyummySingleClickFlow() }
    if (singleClick) {
        // 통로 하나에 듣는 곳 하나. 겹친 테마가 둘 다 켜져 있으면 안쪽이 자기 통로와 듣는 곳을 따로 가진다.
        LaunchedEffect(clicks) { collectNyummySingleClicks(clicks) }
    }
    CompositionLocalProvider(
        // 켜고 끄기를 if로 content 호출을 갈라 하지 않고 값으로만 바꿔, 아래 슬롯 구조가 바뀌지 않게 한다.
        LocalNyummySingleClick provides (if (singleClick) clicks else null),
        LocalNyummyColors provides DefaultNyummyColors,
        LocalNyummyTypography provides DefaultNyummyTypography,
        LocalNyummySpacing provides DefaultNyummySpacing,
        LocalNyummyRadius provides DefaultNyummyRadius,
        LocalNyummyBorderWidth provides DefaultNyummyBorderWidth,
        LocalNyummySize provides DefaultNyummySize,
        LocalNyummyElevation provides DefaultNyummyElevation,
    ) {
        // Material3 컴포넌트(시트, 스낵바, 아이콘, 글자 선택 색 등)의 기본값도 냐미 토큰을 따르게 한다.
        MaterialTheme(
            colorScheme = NyummyMaterialColorScheme,
            typography = NyummyMaterialTypography,
            content = content,
        )
    }
}

private val NyummyMaterialColorScheme = with(DefaultNyummyColors) {
    lightColorScheme(
        primary = bg.actionPrimary,
        onPrimary = content.onAction,
        primaryContainer = bg.selected,
        onPrimaryContainer = content.brand,
        secondary = bg.actionNeutral,
        onSecondary = content.primary,
        error = bg.actionDanger,
        onError = content.onAction,
        errorContainer = bg.dangerSubtle,
        onErrorContainer = content.danger,
        background = bg.canvas,
        onBackground = content.primary,
        surface = bg.surface,
        onSurface = content.primary,
        surfaceVariant = bg.surfaceSunken,
        onSurfaceVariant = content.secondary,
        surfaceContainerLowest = bg.surface,
        surfaceContainerLow = bg.surface,
        surfaceContainer = bg.surface,
        surfaceContainerHigh = bg.surfaceSunken,
        surfaceContainerHighest = bg.surfaceSunken,
        surfaceBright = bg.surface,
        surfaceDim = bg.surfaceSunken,
        inverseSurface = bg.sceneDark,
        inverseOnSurface = content.onAction,
        outline = border.default,
        outlineVariant = border.subtle,
        scrim = bg.scrim,
    )
}

private val NyummyMaterialTypography = with(DefaultNyummyTypography) {
    Typography(
        displayLarge = displayL,
        displayMedium = displayM,
        titleLarge = titleL,
        titleMedium = titleM,
        titleSmall = titleS,
        bodyLarge = bodyL,
        bodyMedium = bodyM,
        bodySmall = bodyS,
        labelLarge = labelL,
        labelMedium = labelM,
        labelSmall = labelS,
    )
}

internal val LocalNyummyColors = staticCompositionLocalOf { DefaultNyummyColors }
internal val LocalNyummyTypography = staticCompositionLocalOf { DefaultNyummyTypography }
internal val LocalNyummySpacing = staticCompositionLocalOf { DefaultNyummySpacing }
internal val LocalNyummyRadius = staticCompositionLocalOf { DefaultNyummyRadius }
internal val LocalNyummyBorderWidth = staticCompositionLocalOf { DefaultNyummyBorderWidth }
internal val LocalNyummySize = staticCompositionLocalOf { DefaultNyummySize }
internal val LocalNyummyElevation = staticCompositionLocalOf { DefaultNyummyElevation }
