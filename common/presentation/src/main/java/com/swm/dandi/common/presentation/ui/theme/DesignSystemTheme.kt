package com.swm.dandi.common.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.swm.dandi.common.presentation.ui.color.DesignSystemSemanticColors
import com.swm.dandi.common.presentation.ui.token.DefaultDesignSystemColor
import com.swm.dandi.common.presentation.ui.token.DefaultDesignSystemStaticTypeScale
import com.swm.dandi.common.presentation.ui.typo.DesignSystemTypeScale

interface DesignSystemTheme {

    val designSystemColor: DesignSystemSemanticColors
        @Composable get

    val typeScale: DesignSystemTypeScale
        @Composable get
}

@Composable
fun DesignSystemTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDesignSystemColor provides DefaultDesignSystemColor,
        LocalDesignSystemTypeScale provides DefaultDesignSystemStaticTypeScale,
    ) {
        MaterialTheme(colorScheme = DesignSystemLightColorScheme) {
            content()
        }
    }
}

// Material3 컴포넌트(Scaffold/AlertDialog 등)가 baseline 기본 컬러스킴으로 폴백하면
// surface/background 가 0xFFFFFBFE(살짝 붉은 흰색)로 칠해진다. 프로젝트 색으로 매핑해 이를 막는다.
private val DesignSystemLightColorScheme = lightColorScheme(
    primary = DefaultDesignSystemColor.contentAccent,
    onPrimary = DefaultDesignSystemColor.bgDefaultLevel1,
    background = DefaultDesignSystemColor.bgDefaultLevel1,
    onBackground = DefaultDesignSystemColor.contentDefaultLevel0,
    surface = DefaultDesignSystemColor.bgDefaultLevel1,
    onSurface = DefaultDesignSystemColor.contentDefaultLevel0,
    surfaceVariant = DefaultDesignSystemColor.bgDefaultLevel0,
    onSurfaceVariant = DefaultDesignSystemColor.contentDefaultLevel2,
    surfaceContainerLowest = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceContainerLow = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceContainer = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceContainerHigh = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceContainerHighest = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceBright = DefaultDesignSystemColor.bgDefaultLevel1,
    surfaceDim = DefaultDesignSystemColor.bgDefaultLevel0,
    error = DefaultDesignSystemColor.contentFavorite,
    outline = DefaultDesignSystemColor.borderDefaultLevel1,
    outlineVariant = DefaultDesignSystemColor.borderDefaultLevel0,
)

internal val LocalDesignSystemColor = staticCompositionLocalOf { DefaultDesignSystemColor }

internal val LocalDesignSystemTypeScale = staticCompositionLocalOf { DefaultDesignSystemStaticTypeScale }

object DesignSystemThemeImpl : DesignSystemTheme {
    override val designSystemColor: DesignSystemSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignSystemColor.current

    override val typeScale: DesignSystemTypeScale
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignSystemTypeScale.current
}
