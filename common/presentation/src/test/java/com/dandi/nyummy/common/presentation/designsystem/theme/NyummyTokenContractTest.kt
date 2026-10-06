package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Figma `Nyummy / 2 Semantic`, `3 Dimension`, effect style 값과 코드 토큰이 같은지 확인한다.
 * 기대값은 2026-10-06 Figma 변수 덤프에서 옮겼다(border/focus는 #90 리뷰로 evergreen/600). Figma를 바꾸면 이 표도 함께 바꾼다.
 */
class NyummyTokenContractTest {

    private val colors = DefaultNyummyColors

    @Test
    fun `semantic 색은 Figma 값과 같다`() {
        val expected = mapOf(
            "bg/canvas" to (colors.bg.canvas to "#FFFFFFFF"),
            "bg/surface" to (colors.bg.surface to "#FFFFFFFF"),
            "bg/surface-sunken" to (colors.bg.surfaceSunken to "#FFF9FBF9"),
            "bg/surface-inverse" to (colors.bg.surfaceInverse to "#FF1C1F1D"),
            "bg/scrim" to (colors.bg.scrim to "#661C1F1D"),
            "bg/scrim-strong" to (colors.bg.scrimStrong to "#A31C1F1D"),
            "bg/pressed-overlay" to (colors.bg.pressedOverlay to "#14000000"),
            "bg/action/primary" to (colors.bg.actionPrimary to "#FF547A61"),
            "bg/action/primary-pressed" to (colors.bg.actionPrimaryPressed to "#FF3C664B"),
            "bg/action/secondary" to (colors.bg.actionSecondary to "#FFF3F5F3"),
            "bg/action/secondary-pressed" to (colors.bg.actionSecondaryPressed to "#FFE6E8E7"),
            "bg/action/danger" to (colors.bg.actionDanger to "#FFBC3E4C"),
            "bg/action/danger-pressed" to (colors.bg.actionDangerPressed to "#FF9B2E3B"),
            "bg/action/disabled" to (colors.bg.actionDisabled to "#FFF3F5F3"),
            "bg/selected" to (colors.bg.selected to "#FFE5F5EA"),
            "bg/voice/bubble" to (colors.bg.voiceBubble to "#FFFFFFFF"),
            "bg/voice/coach" to (colors.bg.voiceCoach to "#FFF1FAF3"),
            "bg/success-subtle" to (colors.bg.successSubtle to "#FFE5F5EA"),
            "bg/warning-subtle" to (colors.bg.warningSubtle to "#FFFFF4D4"),
            "bg/danger-subtle" to (colors.bg.dangerSubtle to "#FFFEEBEB"),
            "bg/info-subtle" to (colors.bg.infoSubtle to "#FFF3F5F3"),
            "bg/promo-subtle" to (colors.bg.promoSubtle to "#FFFFF5F5"),
            "bg/scene/room-floor" to (colors.bg.sceneRoomFloor to "#FFC57D33"),
            "content/primary" to (colors.content.primary to "#FF1C1F1D"),
            "content/secondary" to (colors.content.secondary to "#FF535654"),
            "content/tertiary" to (colors.content.tertiary to "#FF6F7370"),
            "content/disabled" to (colors.content.disabled to "#FFB6B8B6"),
            "content/on-action" to (colors.content.onAction to "#FFFFFFFF"),
            "content/on-coin" to (colors.content.onCoin to "#FF1C1F1D"),
            "content/on-inverse" to (colors.content.onInverse to "#FFFFFFFF"),
            "content/brand" to (colors.content.brand to "#FF3C664B"),
            "content/success" to (colors.content.success to "#FF3C664B"),
            "content/warning" to (colors.content.warning to "#FF786101"),
            "content/danger" to (colors.content.danger to "#FFBC3E4C"),
            "content/info" to (colors.content.info to "#FF535654"),
            "content/promo" to (colors.content.promo to "#FFBC3E4C"),
            "border/subtle" to (colors.border.subtle to "#FFF3F5F3"),
            "border/default" to (colors.border.default to "#FFE6E8E7"),
            "border/strong" to (colors.border.strong to "#FFB6B8B6"),
            "border/focus" to (colors.border.focus to "#FF547A61"),
            "border/selected" to (colors.border.selected to "#FF547A61"),
            "border/danger" to (colors.border.danger to "#FFEB616D"),
            "data/progress-fill" to (colors.data.progressFill to "#FF547A61"),
            "data/progress-track" to (colors.data.progressTrack to "#FFE5F5EA"),
            "data/streak" to (colors.data.streak to "#FF547A61"),
            "data/coin" to (colors.data.coin to "#FFF6CB2C"),
            "data/record-marker" to (colors.data.recordMarker to "#FF547A61"),
            "data/nutrient-carb" to (colors.data.nutrientCarb to "#FFD8B00A"),
            "data/nutrient-protein" to (colors.data.nutrientProtein to "#FF6FA381"),
            "data/nutrient-fat" to (colors.data.nutrientFat to "#FFEB616D"),
        )

        assertEquals("Figma Semantic 50개를 모두 대조한다", 50, expected.size)
        expected.forEach { (name, pair) ->
            val (actual, hex) = pair
            assertEquals(name, hex, actual.toHex())
        }
    }

    @Test
    fun `외부 브랜드 색은 Figma 값과 같다`() {
        val external = colors.external
        assertEquals("#FF03C75A", external.naverGreen.toHex())
        assertEquals("#FF4285F4", external.googleBlue.toHex())
        assertEquals("#FF34A853", external.googleGreen.toHex())
        assertEquals("#FFFBBC05", external.googleYellow.toHex())
        assertEquals("#FFEA4335", external.googleRed.toHex())
        assertEquals("#FFFEE500", external.kakaoYellow.toHex())
        assertEquals("#FF000000", external.kakaoSymbol.toHex())
        assertEquals("#D9000000", external.kakaoLabel.toHex())
    }

    @Test
    fun `브랜드 앵커 그린은 547A61 그대로다`() {
        assertEquals("#FF547A61", colors.bg.actionPrimary.toHex())
    }

    @Test
    fun `글자색은 흰 바탕에서 WCAG AA 4_5 대 1 이상이다`() {
        val canvas = colors.bg.canvas
        listOf(
            "content/primary" to colors.content.primary,
            "content/secondary" to colors.content.secondary,
            "content/tertiary" to colors.content.tertiary,
            "content/brand" to colors.content.brand,
            "content/success" to colors.content.success,
            "content/warning" to colors.content.warning,
            "content/danger" to colors.content.danger,
            "content/info" to colors.content.info,
        ).forEach { (name, color) ->
            val ratio = contrast(color, canvas)
            assertTrue("$name 대비 $ratio", ratio >= 4.5)
        }
    }

    @Test
    fun `채움 버튼 위 글자는 WCAG AA 4_5 대 1 이상이다`() {
        listOf(
            "primary" to colors.bg.actionPrimary,
            "primary-pressed" to colors.bg.actionPrimaryPressed,
            "danger" to colors.bg.actionDanger,
            "danger-pressed" to colors.bg.actionDangerPressed,
        ).forEach { (name, background) ->
            val ratio = contrast(colors.content.onAction, background)
            assertTrue("on-action / $name 대비 $ratio", ratio >= 4.5)
        }
        val secondaryRatio = contrast(colors.content.primary, colors.bg.actionSecondary)
        assertTrue("primary / secondary 대비 $secondaryRatio", secondaryRatio >= 4.5)
    }

    @Test
    fun `포커스 테두리는 흰 바탕과 움푹한 바탕에서 비텍스트 대비 3 대 1 이상이다`() {
        listOf(
            "bg/canvas" to colors.bg.canvas,
            "bg/surface-sunken" to colors.bg.surfaceSunken,
        ).forEach { (name, background) ->
            val ratio = contrast(colors.border.focus, background)
            assertTrue("border/focus / $name 대비 $ratio", ratio >= 3.0)
        }
    }

    @Test
    fun `spacing은 Figma 눈금과 같다`() {
        val spacing = DefaultNyummySpacing
        assertDp(
            listOf(2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 20),
            listOf(
                spacing.s2, spacing.s4, spacing.s8, spacing.s12, spacing.s16, spacing.s20,
                spacing.s24, spacing.s32, spacing.s40, spacing.s48, spacing.gutter,
            ),
        )
    }

    @Test
    fun `radius와 border는 Figma 눈금과 같다`() {
        val radius = DefaultNyummyRadius
        assertDp(listOf(8, 12, 16, 24, 999), listOf(radius.xs, radius.s, radius.m, radius.l, radius.full))
        val border = DefaultNyummyBorderWidth
        assertDp(listOf(1, 2), listOf(border.hairline, border.bold))
    }

    @Test
    fun `size는 Figma 값과 같다`() {
        val size = DefaultNyummySize
        assertDp(
            listOf(48, 14, 16, 20, 24, 48, 72, 120, 160, 224, 48, 56, 77),
            listOf(
                size.touchTarget, size.iconXs, size.iconS, size.iconM, size.iconL,
                size.characterXs, size.characterS, size.characterM, size.characterL, size.characterHero,
                size.buttonM, size.buttonL, size.bottomNav,
            ),
        )
    }

    @Test
    fun `elevation은 Figma effect style과 같다`() {
        val elevation = DefaultNyummyElevation
        assertEquals("#1A1C1F1D", elevation.soft.color.toHex())
        assertDp(listOf(12, 4), listOf(elevation.soft.blur, elevation.soft.offsetY))
        assertEquals("#241C1F1D", elevation.float.color.toHex())
        assertDp(listOf(24, 8), listOf(elevation.float.blur, elevation.float.offsetY))
    }

    private fun assertDp(expected: List<Int>, actual: List<Dp>) {
        assertEquals(expected.map { it.toFloat() }, actual.map { it.value })
    }

    private fun Color.toHex(): String = "#%08X".format(toArgb())

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
