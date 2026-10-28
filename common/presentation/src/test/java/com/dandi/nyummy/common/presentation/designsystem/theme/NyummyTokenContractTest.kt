package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.designsystem.token.NyummyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Figma `Nyummy v2 / 1 Primitive`, `2 Semantic`, `3 Dimension`, effect style 값과 코드 토큰이 같은지 확인한다.
 * 기대값은 2026-10-28 Figma v2 변수 덤프에서 옮겼다. Figma를 바꾸면 이 표도 함께 바꾼다.
 */
class NyummyTokenContractTest {

    private val colors = DefaultNyummyColors

    @Test
    fun `팔레트는 Figma Primitive 값과 같다`() {
        val expected = mapOf(
            "color/base/white" to (NyummyPalette.White to "#FFFFFFFF"),
            "color/base/black" to (NyummyPalette.Black to "#FF000000"),
            "color/base/scrim" to (NyummyPalette.Scrim to "#66000000"),
            "color/base/pressed-overlay" to (NyummyPalette.PressedOverlay to "#14000000"),
            "color/gray/50" to (NyummyPalette.Gray50 to "#FFF7F7F7"),
            "color/gray/100" to (NyummyPalette.Gray100 to "#FFF0F0F0"),
            "color/gray/150" to (NyummyPalette.Gray150 to "#FFEDEDED"),
            "color/gray/200" to (NyummyPalette.Gray200 to "#FFE4E4E4"),
            "color/gray/300" to (NyummyPalette.Gray300 to "#FFC0C0C0"),
            "color/gray/400" to (NyummyPalette.Gray400 to "#FFA5A5A5"),
            "color/gray/500" to (NyummyPalette.Gray500 to "#FF939393"),
            "color/gray/700" to (NyummyPalette.Gray700 to "#FF686868"),
            "color/gray/900" to (NyummyPalette.Gray900 to "#FF262626"),
            "color/evergreen/50" to (NyummyPalette.Evergreen50 to "#FFF4FAF6"),
            "color/evergreen/100" to (NyummyPalette.Evergreen100 to "#FFECF5F0"),
            "color/evergreen/200" to (NyummyPalette.Evergreen200 to "#FFD2E7DA"),
            "color/evergreen/300" to (NyummyPalette.Evergreen300 to "#FFACCFBA"),
            "color/evergreen/400" to (NyummyPalette.Evergreen400 to "#FF7FB394"),
            "color/evergreen/500" to (NyummyPalette.Evergreen500 to "#FF5C9A74"),
            "color/evergreen/600" to (NyummyPalette.Evergreen600 to "#FF43855D"),
            "color/evergreen/700" to (NyummyPalette.Evergreen700 to "#FF376D4C"),
            "color/evergreen/800" to (NyummyPalette.Evergreen800 to "#FF2B553B"),
            "color/evergreen/900" to (NyummyPalette.Evergreen900 to "#FF1F3E2B"),
            "color/evergreen/ink" to (NyummyPalette.EvergreenInk to "#FF243329"),
            "color/sage/400" to (NyummyPalette.Sage400 to "#FFAFB8AF"),
            "color/red/50" to (NyummyPalette.Red50 to "#FFFDEBED"),
            "color/red/500" to (NyummyPalette.Red500 to "#FFEA3348"),
            "color/red/600" to (NyummyPalette.Red600 to "#FFC92639"),
            "color/orange/50" to (NyummyPalette.Orange50 to "#FFFCF2E8"),
            "color/orange/500" to (NyummyPalette.Orange500 to "#FFDC7E19"),
            "color/gold/300" to (NyummyPalette.Gold300 to "#FFFBC15D"),
            "color/external/kakao-yellow" to (NyummyPalette.ExternalKakaoYellow to "#FFFEE500"),
            "color/external/kakao-symbol" to (NyummyPalette.ExternalKakaoSymbol to "#FF000000"),
            "color/scene/reward-stage" to (NyummyPalette.SceneRewardStage to "#FF173221"),
            "color/scene/room-floor" to (NyummyPalette.SceneRoomFloor to "#FFC57D33"),
        )

        assertEquals("Figma Primitive 35개를 모두 대조한다", 35, expected.size)
        expected.forEach { (name, pair) ->
            val (actual, hex) = pair
            assertEquals(name, hex, actual.toHex())
        }
    }

    @Test
    fun `semantic 색은 Figma 값과 같다`() {
        val expected = mapOf(
            "bg/canvas" to (colors.bg.canvas to "#FFFFFFFF"),
            "bg/surface" to (colors.bg.surface to "#FFFFFFFF"),
            "bg/surface-subtle" to (colors.bg.surfaceSubtle to "#FFF7F7F7"),
            "bg/surface-sunken" to (colors.bg.surfaceSunken to "#FFF0F0F0"),
            "bg/section-band" to (colors.bg.sectionBand to "#FFF7F7F7"),
            "bg/scrim" to (colors.bg.scrim to "#66000000"),
            "bg/pressed-overlay" to (colors.bg.pressedOverlay to "#14000000"),
            "bg/action/primary" to (colors.bg.actionPrimary to "#FF43855D"),
            "bg/action/primary-pressed" to (colors.bg.actionPrimaryPressed to "#FF376D4C"),
            "bg/action/tonal" to (colors.bg.actionTonal to "#FFECF5F0"),
            "bg/action/tonal-pressed" to (colors.bg.actionTonalPressed to "#FFD2E7DA"),
            "bg/action/neutral" to (colors.bg.actionNeutral to "#FFF0F0F0"),
            "bg/action/neutral-pressed" to (colors.bg.actionNeutralPressed to "#FFE4E4E4"),
            "bg/action/danger" to (colors.bg.actionDanger to "#FFEA3348"),
            "bg/action/danger-pressed" to (colors.bg.actionDangerPressed to "#FFC92639"),
            "bg/action/disabled" to (colors.bg.actionDisabled to "#FFF0F0F0"),
            "bg/selected" to (colors.bg.selected to "#FFECF5F0"),
            "bg/brand-subtle" to (colors.bg.brandSubtle to "#FFECF5F0"),
            "bg/danger-subtle" to (colors.bg.dangerSubtle to "#FFFDEBED"),
            "bg/promo-subtle" to (colors.bg.promoSubtle to "#FFFCF2E8"),
            "bg/voice/bubble" to (colors.bg.voiceBubble to "#FFFFFFFF"),
            "bg/scene/dark" to (colors.bg.sceneDark to "#FF262626"),
            "bg/scene/room-floor" to (colors.bg.sceneRoomFloor to "#FFC57D33"),
            "bg/scene/reward-stage" to (colors.bg.sceneRewardStage to "#FF173221"),
            "content/primary" to (colors.content.primary to "#FF262626"),
            "content/secondary" to (colors.content.secondary to "#FF686868"),
            "content/tertiary" to (colors.content.tertiary to "#FF939393"),
            "content/quiet" to (colors.content.quiet to "#FFA5A5A5"),
            "content/disabled" to (colors.content.disabled to "#FFC0C0C0"),
            "content/on-action" to (colors.content.onAction to "#FFFFFFFF"),
            "content/brand" to (colors.content.brand to "#FF43855D"),
            "content/voice" to (colors.content.voice to "#FF243329"),
            "content/danger" to (colors.content.danger to "#FFEA3348"),
            "content/promo" to (colors.content.promo to "#FFDC7E19"),
            "border/subtle" to (colors.border.subtle to "#FFEDEDED"),
            "border/default" to (colors.border.default to "#FFEDEDED"),
            "border/strong" to (colors.border.strong to "#FFC0C0C0"),
            "border/focus" to (colors.border.focus to "#FF43855D"),
            "border/selected" to (colors.border.selected to "#FF43855D"),
            "border/brand-subtle" to (colors.border.brandSubtle to "#FFACCFBA"),
            "border/danger" to (colors.border.danger to "#FFEA3348"),
            "nav/icon-on" to (colors.nav.iconOn to "#FF43855D"),
            "nav/icon-off" to (colors.nav.iconOff to "#FFAFB8AF"),
            "data/progress-fill" to (colors.data.progressFill to "#FF43855D"),
            "data/progress-track" to (colors.data.progressTrack to "#FFF0F0F0"),
            "data/coin" to (colors.data.coin to "#FFFBC15D"),
            "data/record-marker" to (colors.data.recordMarker to "#FF43855D"),
        )

        assertEquals("Figma Semantic 47개를 모두 대조한다", 47, expected.size)
        expected.forEach { (name, pair) ->
            val (actual, hex) = pair
            assertEquals(name, hex, actual.toHex())
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `튜토리얼 스택용으로 남긴 voiceCoach는 brandSubtle과 같다`() {
        assertEquals(colors.bg.brandSubtle.toHex(), colors.bg.voiceCoach.toHex())
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
    fun `브랜드 앵커 그린은 43855D 그대로다`() {
        assertEquals("#FF43855D", colors.bg.actionPrimary.toHex())
    }

    @Test
    fun `본문급 글자는 4_5 대 1 이상이다`() {
        assertContrast(
            minimum = 4.5,
            "content/primary" to (colors.content.primary to colors.bg.canvas),
            "content/secondary" to (colors.content.secondary to colors.bg.canvas),
            "content/voice" to (colors.content.voice to colors.bg.canvas),
            "secondary / action/neutral" to (colors.content.secondary to colors.bg.actionNeutral),
        )
    }

    @Test
    fun `굵은 강조와 버튼 글자는 3_9 대 1 이상이다`() {
        assertContrast(
            minimum = 3.9,
            "content/brand" to (colors.content.brand to colors.bg.canvas),
            "content/danger" to (colors.content.danger to colors.bg.canvas),
            "on-action / action/primary" to (colors.content.onAction to colors.bg.actionPrimary),
            "on-action / action/primary-pressed" to (colors.content.onAction to colors.bg.actionPrimaryPressed),
            "on-action / action/danger" to (colors.content.onAction to colors.bg.actionDanger),
            "on-action / action/danger-pressed" to (colors.content.onAction to colors.bg.actionDangerPressed),
            "brand / action/tonal" to (colors.content.brand to colors.bg.actionTonal),
        )
    }

    @Test
    fun `메타와 태그 글자는 2_9 대 1 이상이다`() {
        assertContrast(
            minimum = 2.9,
            "content/tertiary" to (colors.content.tertiary to colors.bg.canvas),
            "content/promo" to (colors.content.promo to colors.bg.canvas),
        )
    }

    @Test
    fun `없음과 지나감 글자는 2_4 대 1 이상이다`() {
        assertContrast(minimum = 2.4, "content/quiet" to (colors.content.quiet to colors.bg.canvas))
    }

    @Test
    fun `포커스 테두리는 흰 바탕과 움푹한 바탕에서 비텍스트 대비 3 대 1 이상이다`() {
        assertContrast(
            minimum = 3.0,
            "border/focus / bg/canvas" to (colors.border.focus to colors.bg.canvas),
            "border/focus / bg/surface-sunken" to (colors.border.focus to colors.bg.surfaceSunken),
        )
    }

    @Test
    fun `spacing은 Figma 눈금과 같다`() {
        val spacing = DefaultNyummySpacing
        assertDp(
            listOf(2, 4, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 20),
            listOf(
                spacing.s2, spacing.s4, spacing.s6, spacing.s8, spacing.s10, spacing.s12, spacing.s16,
                spacing.s20, spacing.s24, spacing.s32, spacing.s40, spacing.s48, spacing.gutter,
            ),
        )
    }

    @Test
    fun `radius와 border는 Figma 눈금과 같다`() {
        val radius = DefaultNyummyRadius
        assertDp(
            listOf(8, 12, 16, 20, 24, 999),
            listOf(radius.xs, radius.s, radius.m, radius.ml, radius.l, radius.full),
        )
        val border = DefaultNyummyBorderWidth
        assertDp(listOf(1, 2), listOf(border.hairline, border.bold))
    }

    @Test
    fun `size는 Figma 값과 같다`() {
        val size = DefaultNyummySize
        assertDp(
            listOf(48, 14, 16, 20, 24, 48, 72, 120, 160, 224, 48, 56, 77, 6),
            listOf(
                size.touchTarget, size.iconXs, size.iconS, size.iconM, size.iconL,
                size.characterXs, size.characterS, size.characterM, size.characterL, size.characterHero,
                size.buttonM, size.buttonL, size.bottomNav, size.sectionBand,
            ),
        )
    }

    @Test
    fun `elevation은 Figma effect style과 같다`() {
        val elevation = DefaultNyummyElevation
        assertEquals("#1A262626", elevation.soft.color.toHex())
        assertDp(listOf(12, 4), listOf(elevation.soft.blur, elevation.soft.offsetY))
        assertEquals("#24262626", elevation.float.color.toHex())
        assertDp(listOf(24, 8), listOf(elevation.float.blur, elevation.float.offsetY))
    }

    private fun assertContrast(minimum: Double, vararg pairs: Pair<String, Pair<Color, Color>>) {
        pairs.forEach { (name, colorsPair) ->
            val (foreground, background) = colorsPair
            val ratio = contrast(foreground, background)
            assertTrue("$name 대비 $ratio (하한 $minimum)", ratio >= minimum)
        }
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
