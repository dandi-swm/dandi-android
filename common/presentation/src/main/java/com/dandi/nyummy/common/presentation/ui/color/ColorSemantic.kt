package com.dandi.nyummy.common.presentation.ui.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

@Immutable
data class DesignSystemSemanticColors(
    val bgDefaultLevel0: Color,
    val bgDefaultLevel1: Color,
    val bgDefaultLevel2: Color,
    val borderDefaultLevel0: Color,
    val borderDefaultLevel1: Color,
    val borderDefaultLevel2: Color,
    val contentDefaultLevel0: Color,
    val contentDefaultLevel1: Color,
    val contentDefaultLevel2: Color,
    val contentDefaultLevel3: Color,
    val contentAccent: Color,
    val contentError: Color,
) {
    @Composable
    fun withStringKey(key: String): Color {
        return when (key) {
            "bgDefaultLevel0" -> DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0
            "bgDefaultLevel1" -> DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
            "bgDefaultLevel2" -> DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
            "borderDefaultLevel0" -> DesignSystemThemeImpl.designSystemColor.borderDefaultLevel0
            "borderDefaultLevel1" -> DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
            "borderDefaultLevel2" -> DesignSystemThemeImpl.designSystemColor.borderDefaultLevel2
            "contentDefaultLevel0" -> DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0
            "contentDefaultLevel1" -> DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1
            "contentDefaultLevel2" -> DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
            "contentDefaultLevel3" -> DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3
            "contentAccent" -> DesignSystemThemeImpl.designSystemColor.contentAccent
            "contentError" -> DesignSystemThemeImpl.designSystemColor.contentError
            else -> DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3
        }
    }
}
