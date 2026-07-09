package com.swm.dandi.common.presentation.ui.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.swm.dandi.common.presentation.ui.color.DesignSystemSemanticColors
import com.swm.dandi.common.presentation.ui.typo.ArchiStaticTypeScale
import com.swm.dandi.common.presentation.ui.typo.DesignSystemTypeScale
import com.swm.dandi.common.presentation.ui.typo.pretendardTextFont

// =====================================================================================
// 디자인 토큰 단일 정의 파일.
// Figma Variables / Text Styles 를 코드에 반영할 때는 이 파일만 수정한다.
// (슬롯 구조 자체를 바꿀 때만 ArchiSemanticColors / ArchiTypeScale 을 수정)
// 매핑 규칙: docs/architecture/design-system.md
// =====================================================================================

// FIGMA-TOKEN-INJECTION-POINT: palette
// Figma Variable `palette/{name}/{step}` → ArchiPaletteColors.{Name}{Step}
// 예: palette/gray/900 → Gray900, palette/blue/400 → Blue400
@Suppress("Unused")
internal enum class ArchiPaletteColors(val colorValue: Color) {
    Black(Color(0xFF2A1712)),
    Gray300(Color(0xFF3D251D)),
    Gray400(Color(0xFF66504A)),
    Gray500(Color(0xFF8B766D)),
    Gray600(Color(0xFFF3E5DA)),
    Gray700(Color(0xFFE4CBBE)),
    Gray800(Color(0xFFFFEEE7)),
    Gray900(Color(0xFFFFFAF7)),
    White(Color(0xFFFFFFFF)),
    Blue400(Color(0xFFA65A05)),
    Red(Color(0xFFC9473F)),
}

// FIGMA-TOKEN-INJECTION-POINT: semantic-colors
// Figma Variable `{role}/{variant}/{level}` → {role}{Variant}{Level}
// 예: bg/default/level0 → bgDefaultLevel0, content/accent → contentAccent
// 시맨틱 슬롯은 반드시 위 팔레트를 참조한다 (raw hex 직접 사용 금지).
val DefaultDesignSystemColor = DesignSystemSemanticColors(
    bgDefaultLevel0 = ArchiPaletteColors.Gray900.colorValue,
    bgDefaultLevel1 = ArchiPaletteColors.White.colorValue,
    bgDefaultLevel2 = ArchiPaletteColors.Gray600.colorValue,
    borderDefaultLevel0 = ArchiPaletteColors.Gray300.colorValue,
    borderDefaultLevel1 = ArchiPaletteColors.Gray700.colorValue,
    borderDefaultLevel2 = ArchiPaletteColors.Gray800.colorValue,
    contentDefaultLevel0 = ArchiPaletteColors.Black.colorValue,
    contentDefaultLevel1 = ArchiPaletteColors.Gray300.colorValue,
    contentDefaultLevel2 = ArchiPaletteColors.Gray400.colorValue,
    contentDefaultLevel3 = ArchiPaletteColors.Gray500.colorValue,
    contentAccent = ArchiPaletteColors.Blue400.colorValue,
    contentFavorite = ArchiPaletteColors.Red.colorValue,
)

private const val DisplayAndTitleHeightPercent = 1.12f

// FIGMA-TOKEN-INJECTION-POINT: type-scale
// Figma Text Style `{group}/{weight}/{size}` → {group}{Weight}{Size}
// 예: title/strong/L → titleStrongL, text/regular/XS → textRegularXS
internal val DefaultDesignSystemStaticTypeScale = DesignSystemTypeScale(
    // Bold / 18sp — 상세 화면 상단 타이틀
    _titleStrongL = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18,
        lineHeight = 18 * DisplayAndTitleHeightPercent,
        letterSpacing = -0.02f,
    ),
    // Bold / 16sp — 탭 선택 상태
    _textStrongL = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16,
        lineHeight = 16 * 1.35f,
        letterSpacing = -0.015f,
    ),
    // Normal / 16sp — 탭 비선택 상태 / 검색바 입력 텍스트
    _textRegularL = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16,
        lineHeight = 16 * 1.35f,
        letterSpacing = -0.015f,
    ),
    // Bold / 15sp — 리스트 아이템 타이틀
    _textStrongM = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15,
        lineHeight = 15 * 1.35f,
        letterSpacing = -0.015f,
    ),
    // Normal / 14sp — 보조 라벨(카테고리 등)
    _textRegularM = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14,
        lineHeight = 14 * 1.40f,
        letterSpacing = -0.015f,
    ),
    // Normal / 13sp — 보조 텍스트(URL / 날짜)
    _textRegularS = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13,
        lineHeight = 13 * 1.40f,
        letterSpacing = -0.015f,
    ),
    // Normal / 12sp — 캡션(리스트 날짜)
    _textRegularXS = ArchiStaticTypeScale(
        fontFamily = pretendardTextFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12,
        lineHeight = 12 * 1.40f,
        letterSpacing = -0.015f,
    ),
)
