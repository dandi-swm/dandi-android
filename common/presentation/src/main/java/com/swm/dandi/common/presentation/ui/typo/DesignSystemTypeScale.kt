package com.swm.dandi.common.presentation.ui.typo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl

@ConsistentCopyVisibility
@Suppress("ConstructorParameterNaming", "Unused")
@Immutable
data class DesignSystemTypeScale internal constructor(
    private val _titleStrongL: ArchiStaticTypeScale,
    private val _textStrongL: ArchiStaticTypeScale,
    private val _textRegularL: ArchiStaticTypeScale,
    private val _textStrongM: ArchiStaticTypeScale,
    private val _textRegularM: ArchiStaticTypeScale,
    private val _textRegularS: ArchiStaticTypeScale,
    private val _textRegularXS: ArchiStaticTypeScale,
) {
    val titleStrongL: TextStyle @Composable get() = _titleStrongL.textStyle
    val textStrongL: TextStyle @Composable get() = _textStrongL.textStyle
    val textRegularL: TextStyle @Composable get() = _textRegularL.textStyle
    val textStrongM: TextStyle @Composable get() = _textStrongM.textStyle
    val textRegularM: TextStyle @Composable get() = _textRegularM.textStyle
    val textRegularS: TextStyle @Composable get() = _textRegularS.textStyle
    val textRegularXS: TextStyle @Composable get() = _textRegularXS.textStyle

    @Composable
    fun withStringKey(typeScale: String): TextStyle {
        return when (typeScale) {
            "titleStrongL" -> DesignSystemThemeImpl.typeScale.titleStrongL
            "textStrongL" -> DesignSystemThemeImpl.typeScale.textStrongL
            "textRegularL" -> DesignSystemThemeImpl.typeScale.textRegularL
            "textStrongM" -> DesignSystemThemeImpl.typeScale.textStrongM
            "textRegularM" -> DesignSystemThemeImpl.typeScale.textRegularM
            "textRegularS" -> DesignSystemThemeImpl.typeScale.textRegularS
            "textRegularXS" -> DesignSystemThemeImpl.typeScale.textRegularXS
            else -> DesignSystemThemeImpl.typeScale.textRegularM
        }
    }
}

private val ArchiStaticTypeScale.textStyle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
        fontSize = fontSize.textDp,
        lineHeight = lineHeight.textDp,
        letterSpacing = letterSpacing.em,
        fontFeatureSettings = fontFeatureSettings,
    )
