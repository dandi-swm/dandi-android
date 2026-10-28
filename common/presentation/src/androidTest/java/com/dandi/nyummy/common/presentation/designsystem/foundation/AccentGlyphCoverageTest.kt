package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.DefaultNyummyTypography
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyFontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 엄마까투리체로 그릴 수 있는 글자 판정. 실제 글꼴 파일을 읽어야 해서 기기에서 돈다.
 * 이 글꼴은 한글 11,172자를 문자표에 올리고 2,350자(KS X 1001)에만 모양을 넣었다.
 */
@RunWith(AndroidJUnit4::class)
class AccentGlyphCoverageTest {

    private val coverage = AccentGlyphCoverage.of(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun 글꼴을_읽는다() {
        assertNotNull(coverage)
    }

    @Test
    fun 완성형_2350자_안의_한글은_그릴_수_있다() {
        assertTrue(coverage!!.covers("기다리고 있었어요! 오늘도 냠냠"))
        assertTrue(coverage.covers("냐미"))
    }

    @Test
    fun 완성형_밖의_한글은_그릴_수_없다() {
        assertFalse(coverage!!.covers("똠양꿍"))
        assertFalse(coverage.covers("뷁"))
    }

    @Test
    fun 한글_자모는_그릴_수_있다() {
        assertTrue(coverage!!.covers("ㅋㅋ ㅎㅎ"))
    }

    @Test
    fun 숫자와_영문과_문장_부호는_그릴_수_있다() {
        assertTrue(coverage!!.covers("1,224 kcal"))
        assertTrue(coverage.covers("Nyummy!?.,~"))
    }

    @Test
    fun 영문_대소문자_밖의_글자는_그릴_수_없다() {
        assertFalse(coverage!!.covers("Café"))
        assertFalse(coverage.covers("漢字"))
        assertFalse(coverage.covers("にゃん"))
    }

    @Test
    fun 이모지는_판정에서_뺀다() {
        assertTrue(coverage!!.covers("밥 먹자 🍚"))
    }

    @Test
    fun 그릴_수_없는_글자가_든_accent_문장은_같은_크기의_Pretendard_Medium으로_바꾼다() {
        val voice = DefaultNyummyTypography.voiceM
        val safe = voice.accentSafe("똠양꿍도 맛있게 먹었어요!", coverage)
        assertEquals(NyummyFontFamily.Ui, safe.fontFamily)
        assertEquals(FontWeight.Medium, safe.fontWeight)
        assertEquals(voice.fontSize, safe.fontSize)
        assertEquals(voice.lineHeight, safe.lineHeight)
        assertEquals(voice.letterSpacing, safe.letterSpacing)
    }

    @Test
    fun 그릴_수_있는_accent_문장과_UI_스타일은_그대로_둔다() {
        val voice = DefaultNyummyTypography.voiceM
        assertEquals(voice, voice.accentSafe("기다리고 있었어요!", coverage))
        val body = DefaultNyummyTypography.bodyM
        assertEquals(body, body.accentSafe("똠양꿍", coverage))
    }
}
