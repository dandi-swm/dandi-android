package com.dandi.nyummy.common.presentation.ui.typo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

@ConsistentCopyVisibility
@Suppress("ConstructorParameterNaming", "Unused")
@Immutable
data class DesignSystemTypeScale internal constructor(
    private val _displayRegularXXL: ArchiStaticTypeScale,
    private val _displayRegularXL: ArchiStaticTypeScale,
    private val _displayRegularL: ArchiStaticTypeScale,
    private val _displayRegularM: ArchiStaticTypeScale,
    private val _voiceRegularM: ArchiStaticTypeScale,
    private val _textStrongL: ArchiStaticTypeScale,
    private val _textRegularM: ArchiStaticTypeScale,
    private val _labelStrongS: ArchiStaticTypeScale,
    private val _numberStrongL: ArchiStaticTypeScale,
    private val _labelRegularXS: ArchiStaticTypeScale,
    private val _labelStrongXS: ArchiStaticTypeScale,
    private val _textStrongM: ArchiStaticTypeScale,
    private val _textRegularS: ArchiStaticTypeScale,
    private val _titleStrongL: ArchiStaticTypeScale,
    private val _textRegularL: ArchiStaticTypeScale,
    private val _textRegularXS: ArchiStaticTypeScale,
    private val _displayStrongXL: ArchiStaticTypeScale,
    private val _textStrongXL: ArchiStaticTypeScale,
    private val _displayStrongL: ArchiStaticTypeScale,
    private val _displayStrongM: ArchiStaticTypeScale,
    private val _numberStrongM: ArchiStaticTypeScale,
) {
    val displayRegularXXL: TextStyle get() = _displayRegularXXL.textStyle
    val displayRegularXL: TextStyle get() = _displayRegularXL.textStyle
    val displayRegularL: TextStyle get() = _displayRegularL.textStyle
    val displayRegularM: TextStyle get() = _displayRegularM.textStyle
    val voiceRegularM: TextStyle get() = _voiceRegularM.textStyle
    val textStrongL: TextStyle get() = _textStrongL.textStyle
    val textRegularM: TextStyle get() = _textRegularM.textStyle
    val labelStrongS: TextStyle get() = _labelStrongS.textStyle
    val numberStrongL: TextStyle get() = _numberStrongL.textStyle
    val labelRegularXS: TextStyle get() = _labelRegularXS.textStyle
    val labelStrongXS: TextStyle get() = _labelStrongXS.textStyle
    val textStrongM: TextStyle get() = _textStrongM.textStyle
    val textRegularS: TextStyle get() = _textRegularS.textStyle
    val titleStrongL: TextStyle get() = _titleStrongL.textStyle
    val textRegularL: TextStyle get() = _textRegularL.textStyle
    val textRegularXS: TextStyle get() = _textRegularXS.textStyle
    val displayStrongXL: TextStyle get() = _displayStrongXL.textStyle
    val textStrongXL: TextStyle get() = _textStrongXL.textStyle
    val displayStrongL: TextStyle get() = _displayStrongL.textStyle
    val displayStrongM: TextStyle get() = _displayStrongM.textStyle
    val numberStrongM: TextStyle get() = _numberStrongM.textStyle

    @Composable
    fun withStringKey(typeScale: String): TextStyle = when (typeScale) {
        "displayRegularXXL" -> DesignSystemThemeImpl.typeScale.displayRegularXXL
        "displayRegularXL" -> DesignSystemThemeImpl.typeScale.displayRegularXL
        "displayRegularL" -> DesignSystemThemeImpl.typeScale.displayRegularL
        "displayRegularM" -> DesignSystemThemeImpl.typeScale.displayRegularM
        "voiceRegularM" -> DesignSystemThemeImpl.typeScale.voiceRegularM
        "textStrongL" -> DesignSystemThemeImpl.typeScale.textStrongL
        "textRegularM" -> DesignSystemThemeImpl.typeScale.textRegularM
        "labelStrongS" -> DesignSystemThemeImpl.typeScale.labelStrongS
        "numberStrongL" -> DesignSystemThemeImpl.typeScale.numberStrongL
        "labelRegularXS" -> DesignSystemThemeImpl.typeScale.labelRegularXS
        "labelStrongXS" -> DesignSystemThemeImpl.typeScale.labelStrongXS
        "textStrongM" -> DesignSystemThemeImpl.typeScale.textStrongM
        "textRegularS" -> DesignSystemThemeImpl.typeScale.textRegularS
        "titleStrongL" -> DesignSystemThemeImpl.typeScale.titleStrongL
        "textRegularL" -> DesignSystemThemeImpl.typeScale.textRegularL
        "textRegularXS" -> DesignSystemThemeImpl.typeScale.textRegularXS
        "displayStrongXL" -> DesignSystemThemeImpl.typeScale.displayStrongXL
        "textStrongXL" -> DesignSystemThemeImpl.typeScale.textStrongXL
        "displayStrongL" -> DesignSystemThemeImpl.typeScale.displayStrongL
        "displayStrongM" -> DesignSystemThemeImpl.typeScale.displayStrongM
        "numberStrongM" -> DesignSystemThemeImpl.typeScale.numberStrongM
        else -> DesignSystemThemeImpl.typeScale.textRegularM
    }
}

private val ArchiStaticTypeScale.textStyle: TextStyle
    get() = TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.em,
        fontFeatureSettings = fontFeatureSettings,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
