package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Figma `v2/` 텍스트 스타일 20개와 코드 스타일이 같은지 확인한다.
 * 기대값은 2026-10-28 Figma v2 텍스트 스타일 덤프(`name: family/style/size/lineHeight/letterSpacing`)에서 옮겼다.
 * 자간은 em 단위(-3% = -0.03em)다.
 */
class NyummyTypographyContractTest {

    private data class Spec(
        val family: FontFamily,
        val weight: FontWeight,
        val size: Float,
        val lineHeight: Float,
        val letterSpacingEm: Float = 0f,
    )

    private val typography = DefaultNyummyTypography

    private val ui = NyummyFontFamily.Ui
    private val accent = NyummyFontFamily.Accent
    private val tight = -0.03f

    private val specs: Map<String, Pair<TextStyle, Spec>> = mapOf(
        "display/l" to (typography.displayL to Spec(accent, FontWeight.Normal, 24f, 29f)),
        "display/m" to (typography.displayM to Spec(accent, FontWeight.Normal, 20f, 24f)),
        "title/xl" to (typography.titleXl to Spec(ui, FontWeight.Bold, 24f, 29f, tight)),
        "title/l" to (typography.titleL to Spec(ui, FontWeight.Bold, 20f, 24f, tight)),
        "title/m" to (typography.titleM to Spec(ui, FontWeight.Bold, 18f, 21f, tight)),
        "title/s" to (typography.titleS to Spec(ui, FontWeight.Bold, 16f, 19f, tight)),
        "body/l" to (typography.bodyL to Spec(ui, FontWeight.Medium, 16f, 24f, tight)),
        "body/m" to (typography.bodyM to Spec(ui, FontWeight.Medium, 14f, 20f, tight)),
        "body/s" to (typography.bodyS to Spec(ui, FontWeight.Medium, 12f, 17f, tight)),
        "label/l" to (typography.labelL to Spec(ui, FontWeight.Bold, 16f, 19f, tight)),
        "label/m" to (typography.labelM to Spec(ui, FontWeight.Bold, 14f, 17f, tight)),
        "label/s" to (typography.labelS to Spec(ui, FontWeight.Bold, 12f, 14f, tight)),
        "label/xs" to (typography.labelXs to Spec(ui, FontWeight.Bold, 10f, 12f, tight)),
        "voice/m" to (typography.voiceM to Spec(accent, FontWeight.Normal, 16f, 24f, tight)),
        "voice/s" to (typography.voiceS to Spec(accent, FontWeight.Normal, 14f, 21f, tight)),
        "number/xl" to (typography.numberXl to Spec(accent, FontWeight.Normal, 24f, 29f)),
        "number/l" to (typography.numberL to Spec(accent, FontWeight.Normal, 20f, 24f)),
        "number/m" to (typography.numberM to Spec(accent, FontWeight.Normal, 18f, 21f)),
        "number/s" to (typography.numberS to Spec(accent, FontWeight.Normal, 16f, 19f)),
        "number/xs" to (typography.numberXs to Spec(accent, FontWeight.Normal, 14f, 17f)),
    )

    @Test
    fun `Figma 텍스트 스타일 20개를 모두 대조한다`() {
        assertEquals(20, specs.size)
    }

    @Test
    fun `서체와 굵기는 Figma 스타일과 같다`() {
        specs.forEach { (name, pair) ->
            val (style, spec) = pair
            assertEquals("$name family", spec.family, style.fontFamily)
            assertEquals("$name weight", spec.weight, style.fontWeight)
        }
    }

    @Test
    fun `UI 글자는 Medium과 Bold 두 굵기만 쓴다`() {
        specs.values.filter { (style, _) -> style.fontFamily == ui }.forEach { (style, _) ->
            assertTrue("${style.fontWeight}", style.fontWeight == FontWeight.Medium || style.fontWeight == FontWeight.Bold)
        }
    }

    @Test
    fun `크기 줄높이 자간은 Figma 스타일과 같다`() {
        specs.forEach { (name, pair) ->
            val (style, spec) = pair
            assertEquals("$name size", spec.size, style.fontSize.value, 0.001f)
            assertEquals("$name lineHeight", spec.lineHeight, style.lineHeight.value, 0.001f)
            assertEquals("$name letterSpacing", spec.letterSpacingEm, style.letterSpacing.value, 0.0001f)
            if (spec.letterSpacingEm != 0f) assertTrue("$name letterSpacing은 em", style.letterSpacing.isEm)
        }
    }

    @Test
    fun `줄높이를 자르지 않아 Figma 텍스트 박스 높이와 같다`() {
        specs.forEach { (name, pair) ->
            assertEquals(name, LineHeightStyle.Trim.None, pair.first.lineHeightStyle?.trim)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `튜토리얼 스택용으로 남긴 labelSStrong은 labelS와 같다`() {
        assertEquals(typography.labelS, typography.labelSStrong)
    }
}
