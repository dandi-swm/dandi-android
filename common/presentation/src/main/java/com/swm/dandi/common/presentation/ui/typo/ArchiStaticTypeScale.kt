package com.swm.dandi.common.presentation.ui.typo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

@Immutable
class ArchiStaticTypeScale internal constructor(
    val fontFamily: FontFamily,
    val fontWeight: FontWeight,
    val fontSize: Int,
    val lineHeight: Float,
    val letterSpacing: Float = 0f,
    val fontFeatureSettings: String? = null,
    val textDecoration: TextDecoration = TextDecoration.None,
)
