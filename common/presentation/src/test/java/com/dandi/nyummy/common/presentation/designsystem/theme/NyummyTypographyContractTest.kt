package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Figma 텍스트 스타일 20개와 코드 스타일이 같은지 확인한다.
 * 기대값은 2026-10-06 Figma 텍스트 스타일 덤프(`name: family/style/size/lineHeight/letterSpacing`)에서 옮겼다.
 */
class NyummyTypographyContractTest {

    private data class Spec(
        val family: FontFamily,
        val weight: FontWeight,
        val size: Float,
        val lineHeight: Float,
        val letterSpacing: Float = 0f,
    )

    private val typography = DefaultNyummyTypography

    private val ui = NyummyFontFamily.Ui
    private val display = NyummyFontFamily.Display
    private val voice = NyummyFontFamily.Voice
    private val number = NyummyFontFamily.Number

    private val specs: Map<String, Pair<TextStyle, Spec>> = mapOf(
        "display/l" to (typography.displayL to Spec(display, FontWeight.Normal, 32f, 40f, -0.5f)),
        "display/m" to (typography.displayM to Spec(display, FontWeight.Normal, 26f, 34f, -0.3f)),
        "title/l" to (typography.titleL to Spec(ui, FontWeight.Bold, 22f, 30f, -0.3f)),
        "title/m" to (typography.titleM to Spec(ui, FontWeight.Bold, 18f, 26f, -0.2f)),
        "title/s" to (typography.titleS to Spec(ui, FontWeight.SemiBold, 16f, 24f)),
        "body/l" to (typography.bodyL to Spec(ui, FontWeight.Normal, 16f, 24f)),
        "body/m" to (typography.bodyM to Spec(ui, FontWeight.Normal, 14f, 21f)),
        "body/s" to (typography.bodyS to Spec(ui, FontWeight.Normal, 12f, 18f)),
        "label/l" to (typography.labelL to Spec(ui, FontWeight.Bold, 17f, 24f)),
        "label/m" to (typography.labelM to Spec(ui, FontWeight.SemiBold, 14f, 20f)),
        "label/m-strong" to (typography.labelMStrong to Spec(ui, FontWeight.Bold, 14f, 20f)),
        "label/s" to (typography.labelS to Spec(ui, FontWeight.SemiBold, 12f, 16f)),
        "label/s-strong" to (typography.labelSStrong to Spec(ui, FontWeight.Bold, 12f, 16f)),
        "voice/m" to (typography.voiceM to Spec(voice, FontWeight.Normal, 16f, 24f)),
        "voice/s" to (typography.voiceS to Spec(voice, FontWeight.Normal, 14f, 22f)),
        "number/xl" to (typography.numberXl to Spec(number, FontWeight.Black, 40f, 44f)),
        "number/l" to (typography.numberL to Spec(number, FontWeight.Black, 28f, 32f)),
        "number/m" to (typography.numberM to Spec(number, FontWeight.Black, 20f, 24f)),
        "number/s" to (typography.numberS to Spec(number, FontWeight.ExtraBold, 16f, 20f)),
        "number/xs" to (typography.numberXs to Spec(number, FontWeight.ExtraBold, 14f, 20f)),
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
    fun `크기 줄높이 자간은 Figma 스타일과 같다`() {
        specs.forEach { (name, pair) ->
            val (style, spec) = pair
            assertEquals("$name size", spec.size, style.fontSize.value, 0.001f)
            assertEquals("$name lineHeight", spec.lineHeight, style.lineHeight.value, 0.001f)
            assertEquals("$name letterSpacing", spec.letterSpacing, style.letterSpacing.value, 0.001f)
        }
    }

    @Test
    fun `줄높이를 자르지 않아 Figma 텍스트 박스 높이와 같다`() {
        specs.forEach { (name, pair) ->
            assertEquals(name, LineHeightStyle.Trim.None, pair.first.lineHeightStyle?.trim)
        }
    }
}
