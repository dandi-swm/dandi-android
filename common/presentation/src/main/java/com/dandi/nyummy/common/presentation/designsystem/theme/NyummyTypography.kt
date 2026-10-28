package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dandi.nyummy.common.presentation.R

/**
 * 텍스트 스타일 20개. 피그마 `v2/` 텍스트 스타일과 1:1로 맞춘다.
 * 디자인 토큰 이름 `title/xl`은 `titleXl`, `label/m`은 `labelM`이 된다.
 *
 * 역할별 서체
 * - UI(Pretendard Medium·Bold): title, body, label. 굵기로 "제목·값인지, 내용인지"를 나눈다.
 * - Accent(AndongKaturi): number(숫자), voice(냐미 말투), display(브랜드 문구).
 */
@Immutable
class NyummyTypography internal constructor(
    /** 브랜드 문구(로그인)와 연출 무대의 큰 글자. 화면 제목에 쓰지 않는다. */
    val displayL: TextStyle,
    /** 브랜드 문구(스플래시)와 연출 무대 글자. */
    val displayM: TextStyle,
    /** 화면 제목. */
    val titleXl: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val titleS: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    /** 버튼. */
    val labelL: TextStyle,
    val labelM: TextStyle,
    val labelS: TextStyle,
    /** 작은 배지. */
    val labelXs: TextStyle,
    val voiceM: TextStyle,
    val voiceS: TextStyle,
    val numberXl: TextStyle,
    val numberL: TextStyle,
    val numberM: TextStyle,
    val numberS: TextStyle,
    val numberXs: TextStyle,
) {
    /** 튜토리얼 스택이 머지될 때까지만 남긴다. v2 라벨은 모두 Bold라 [labelS]와 같다. */
    @Deprecated("v2에서 labelS로 합쳤다.", ReplaceWith("labelS"))
    val labelSStrong: TextStyle get() = labelS
}

internal object NyummyFontFamily {
    val Ui = FontFamily(
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_bold, FontWeight.Bold),
    )
    val Accent = FontFamily(Font(R.font.andong_katuri_regular, FontWeight.Normal))
}

/** UI 글자 자간(-3%). Accent 글자 중 말투만 같은 값을 쓴다. */
private val TightLetterSpacing = (-0.03).em

/**
 * 디자인의 텍스트 박스와 같은 높이가 되도록 줄 높이를 자르지 않는다(Trim.None).
 * 폰트 패딩도 빼서 줄 높이만으로 박스 크기가 정해지게 한다.
 */
private fun nyummyTextStyle(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: TextUnit = TightLetterSpacing,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

private fun uiStyle(weight: FontWeight, size: Int, lineHeight: Int) =
    nyummyTextStyle(NyummyFontFamily.Ui, weight, size, lineHeight)

private fun accentStyle(size: Int, lineHeight: Int, letterSpacing: TextUnit = 0.sp) =
    nyummyTextStyle(NyummyFontFamily.Accent, FontWeight.Normal, size, lineHeight, letterSpacing)

internal val DefaultNyummyTypography = NyummyTypography(
    displayL = accentStyle(24, 29),
    displayM = accentStyle(20, 24),
    titleXl = uiStyle(FontWeight.Bold, 24, 29),
    titleL = uiStyle(FontWeight.Bold, 20, 24),
    titleM = uiStyle(FontWeight.Bold, 18, 21),
    titleS = uiStyle(FontWeight.Bold, 16, 19),
    bodyL = uiStyle(FontWeight.Medium, 16, 24),
    bodyM = uiStyle(FontWeight.Medium, 14, 20),
    bodyS = uiStyle(FontWeight.Medium, 12, 17),
    labelL = uiStyle(FontWeight.Bold, 16, 19),
    labelM = uiStyle(FontWeight.Bold, 14, 17),
    labelS = uiStyle(FontWeight.Bold, 12, 14),
    labelXs = uiStyle(FontWeight.Bold, 10, 12),
    voiceM = accentStyle(16, 24, TightLetterSpacing),
    voiceS = accentStyle(14, 21, TightLetterSpacing),
    numberXl = accentStyle(24, 29),
    numberL = accentStyle(20, 24),
    numberM = accentStyle(18, 21),
    numberS = accentStyle(16, 19),
    numberXs = accentStyle(14, 17),
)
