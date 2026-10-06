package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.dandi.nyummy.common.presentation.R

/**
 * Figma 텍스트 스타일 20개. 이름은 Figma `title/l` → `titleL`, `label/m-strong` → `labelMStrong`.
 *
 * 역할별 서체
 * - UI(Asta Sans): 본문, 라벨, 버튼, 입력
 * - Display(Jua): 화면 제목, 축하, 공지와 보상 타이틀. 본문에 쓰지 않는다.
 * - Voice(Gowun Dodum): 냐미 말풍선 전용. 말풍선 밖에서 쓰지 않는다.
 * - Number(Nunito): 코인, 스트릭, 칼로리, 캘린더 숫자
 */
@Immutable
class NyummyTypography internal constructor(
    val displayL: TextStyle,
    val displayM: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val titleS: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val labelMStrong: TextStyle,
    val labelS: TextStyle,
    val labelSStrong: TextStyle,
    val voiceM: TextStyle,
    val voiceS: TextStyle,
    val numberXl: TextStyle,
    val numberL: TextStyle,
    val numberM: TextStyle,
    val numberS: TextStyle,
    val numberXs: TextStyle,
)

internal object NyummyFontFamily {
    val Ui = FontFamily(
        Font(R.font.asta_sans_regular, FontWeight.Normal),
        Font(R.font.asta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.asta_sans_bold, FontWeight.Bold),
    )
    val Display = FontFamily(Font(R.font.jua_regular, FontWeight.Normal))
    val Voice = FontFamily(Font(R.font.gowun_dodum_regular, FontWeight.Normal))
    val Number = FontFamily(
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
        Font(R.font.nunito_black, FontWeight.Black),
    )
}

/**
 * Figma 텍스트 박스와 같은 높이가 되도록 줄 높이를 자르지 않는다(Trim.None).
 * 폰트 패딩도 빼서 줄 높이만으로 박스 크기가 정해지게 한다.
 */
private fun nyummyTextStyle(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

internal val DefaultNyummyTypography = NyummyTypography(
    displayL = nyummyTextStyle(NyummyFontFamily.Display, FontWeight.Normal, 32, 40, -0.5f),
    displayM = nyummyTextStyle(NyummyFontFamily.Display, FontWeight.Normal, 26, 34, -0.3f),
    titleL = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Bold, 22, 30, -0.3f),
    titleM = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Bold, 18, 26, -0.2f),
    titleS = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.SemiBold, 16, 24),
    bodyL = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Normal, 16, 24),
    bodyM = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Normal, 14, 21),
    bodyS = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Normal, 12, 18),
    labelL = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Bold, 17, 24),
    labelM = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.SemiBold, 14, 20),
    labelMStrong = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Bold, 14, 20),
    labelS = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.SemiBold, 12, 16),
    labelSStrong = nyummyTextStyle(NyummyFontFamily.Ui, FontWeight.Bold, 12, 16),
    voiceM = nyummyTextStyle(NyummyFontFamily.Voice, FontWeight.Normal, 16, 24),
    voiceS = nyummyTextStyle(NyummyFontFamily.Voice, FontWeight.Normal, 14, 22),
    numberXl = nyummyTextStyle(NyummyFontFamily.Number, FontWeight.Black, 40, 44),
    numberL = nyummyTextStyle(NyummyFontFamily.Number, FontWeight.Black, 28, 32),
    numberM = nyummyTextStyle(NyummyFontFamily.Number, FontWeight.Black, 20, 24),
    numberS = nyummyTextStyle(NyummyFontFamily.Number, FontWeight.ExtraBold, 16, 20),
    numberXs = nyummyTextStyle(NyummyFontFamily.Number, FontWeight.ExtraBold, 14, 20),
)
