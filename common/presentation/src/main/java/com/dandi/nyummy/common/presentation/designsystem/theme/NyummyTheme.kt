package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 냐미 디자인 시스템 진입점. 화면과 컴포넌트는 `NyummyTheme.colors.bg.canvas`처럼 여기서만 토큰을 읽는다.
 *
 * Light 전용이다. CompositionLocal 기본값이 곧 토큰 값이라 [NyummyTheme] Composable로 감싸지 않은
 * 프리뷰에서도 같은 값을 읽는다.
 */
object NyummyTheme {
    val colors: NyummyColors
        @Composable @ReadOnlyComposable get() = LocalNyummyColors.current

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

@Composable
fun NyummyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNyummyColors provides DefaultNyummyColors,
        LocalNyummySpacing provides DefaultNyummySpacing,
        LocalNyummyRadius provides DefaultNyummyRadius,
        LocalNyummyBorderWidth provides DefaultNyummyBorderWidth,
        LocalNyummySize provides DefaultNyummySize,
        LocalNyummyElevation provides DefaultNyummyElevation,
        content = content,
    )
}

internal val LocalNyummyColors = staticCompositionLocalOf { DefaultNyummyColors }
internal val LocalNyummySpacing = staticCompositionLocalOf { DefaultNyummySpacing }
internal val LocalNyummyRadius = staticCompositionLocalOf { DefaultNyummyRadius }
internal val LocalNyummyBorderWidth = staticCompositionLocalOf { DefaultNyummyBorderWidth }
internal val LocalNyummySize = staticCompositionLocalOf { DefaultNyummySize }
internal val LocalNyummyElevation = staticCompositionLocalOf { DefaultNyummyElevation }
