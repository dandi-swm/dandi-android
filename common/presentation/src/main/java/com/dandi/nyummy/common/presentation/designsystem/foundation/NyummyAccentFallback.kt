package com.dandi.nyummy.common.presentation.designsystem.foundation

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyFontFamily

/**
 * 엄마까투리체(accent 글꼴)가 어떤 글자를 그릴 수 있는지 알려 준다.
 * 이 글꼴은 한글이 2,350자뿐이라, 서버 문구나 냐미 이름처럼 앱이 정하지 않은 글자는 빠질 수 있다.
 *
 * 이 글꼴은 한글 11,172자를 모두 문자표에 올려 두고 2,350자에만 모양을 넣었다. 그래서 문자표만 보는
 * `Paint.hasGlyph`는 늘 "있다"고 답하고, 빠진 글자는 기기 글꼴로 대신 그려지지도 않아 빈칸이 된다.
 * 한글은 모양이 실제로 찍히는지로 판정하고, 영문 대소문자 말고 다른 문자는 이 글꼴에 없다고 본다.
 * 글자마다 한 번만 확인하고 기억한다. 컴포지션(메인 스레드)에서만 쓴다.
 */
internal class AccentGlyphCoverage(typeface: Typeface) {

    private val paint = Paint().apply { this.typeface = typeface }
    private val bounds = Rect()
    private val known = HashMap<Int, Boolean>()

    /**
     * [text]의 글자를 모두 이 글꼴로 그릴 수 있는지. 글자(한글, 영문 등)만 본다.
     * 숫자와 문장 부호는 글꼴에 들어 있고, 이모지는 어느 글꼴이든 기기 글꼴로 그려지므로 보지 않는다.
     */
    fun covers(text: CharSequence): Boolean {
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (Character.isLetter(codePoint) && !hasGlyph(codePoint)) return false
            index += Character.charCount(codePoint)
        }
        return true
    }

    private fun hasGlyph(codePoint: Int): Boolean = known.getOrPut(codePoint) {
        when (codePoint) {
            in 'A'.code..'Z'.code, in 'a'.code..'z'.code -> true
            in HangulSyllables, in HangulCompatibilityJamo -> hasInk(codePoint)
            else -> false
        }
    }

    private fun hasInk(codePoint: Int): Boolean {
        val glyph = String(Character.toChars(codePoint))
        paint.getTextBounds(glyph, 0, glyph.length, bounds)
        return !bounds.isEmpty
    }

    companion object {
        private val HangulSyllables = 0xAC00..0xD7A3
        private val HangulCompatibilityJamo = 0x3131..0x318E

        @Volatile
        private var instance: AccentGlyphCoverage? = null

        fun of(context: Context): AccentGlyphCoverage? = instance ?: run {
            val typeface = ResourcesCompat.getFont(context.applicationContext, R.font.andong_katuri_regular)
                ?: return null
            AccentGlyphCoverage(typeface).also { instance = it }
        }
    }
}

@Composable
internal fun rememberAccentGlyphCoverage(): AccentGlyphCoverage? {
    val context = LocalContext.current
    return remember { AccentGlyphCoverage.of(context) }
}

/**
 * accent 글꼴 스타일인데 [text]에 그 글꼴에 없는 글자가 있으면, 한 문장에 글꼴이 섞이지 않도록
 * 같은 크기·줄 높이·자간의 Pretendard Medium으로 바꾼 스타일을 돌려준다(design.md D13).
 */
internal fun TextStyle.accentSafe(text: CharSequence, coverage: AccentGlyphCoverage?): TextStyle =
    if (fontFamily == NyummyFontFamily.Accent && coverage != null && !coverage.covers(text)) {
        copy(fontFamily = NyummyFontFamily.Ui, fontWeight = FontWeight.Medium)
    } else {
        this
    }
