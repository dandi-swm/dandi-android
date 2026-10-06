package com.dandi.nyummy.common.presentation.designsystem.token

import androidx.compose.ui.graphics.Color

/**
 * Figma `Nyummy / 1 Primitive` 컬렉션 원본 값.
 *
 * 화면과 컴포넌트는 이 값을 직접 쓰지 않고 [com.dandi.nyummy.common.presentation.designsystem.theme.NyummyColors]
 * Semantic 역할만 참조한다. Figma 값이 바뀌면 이 파일만 갱신한다.
 */
internal object NyummyPalette {
    val Gray50 = Color(0xFFF9FBF9)
    val Gray100 = Color(0xFFF3F5F3)
    val Gray200 = Color(0xFFE6E8E7)
    val Gray300 = Color(0xFFD4D6D5)
    val Gray400 = Color(0xFFB6B8B6)
    val Gray500 = Color(0xFF919592)
    val Gray600 = Color(0xFF6F7370)
    val Gray700 = Color(0xFF535654)
    val Gray800 = Color(0xFF383B39)
    val Gray900 = Color(0xFF1C1F1D)

    val Evergreen50 = Color(0xFFF1FAF3)
    val Evergreen100 = Color(0xFFE5F5EA)
    val Evergreen200 = Color(0xFFCFE9D7)
    val Evergreen300 = Color(0xFFB0D8BC)
    val Evergreen400 = Color(0xFF90C2A1)
    val Evergreen500 = Color(0xFF6FA381)

    /** 브랜드 앵커. 다른 값으로 스냅하지 않는다. */
    val Evergreen600 = Color(0xFF547A61)
    val Evergreen700 = Color(0xFF3C664B)
    val Evergreen800 = Color(0xFF294B36)
    val Evergreen900 = Color(0xFF173221)

    val Gold50 = Color(0xFFFFFBEF)
    val Gold100 = Color(0xFFFFF4D4)
    val Gold200 = Color(0xFFFFEAAC)
    val Gold300 = Color(0xFFFFDB69)
    val Gold400 = Color(0xFFF6CB2C)
    val Gold500 = Color(0xFFD8B00A)
    val Gold600 = Color(0xFF9A7D03)
    val Gold700 = Color(0xFF786101)
    val Gold800 = Color(0xFF584600)
    val Gold900 = Color(0xFF3C2F02)

    val Berry50 = Color(0xFFFFF5F5)
    val Berry100 = Color(0xFFFEEBEB)
    val Berry200 = Color(0xFFFED6D6)
    val Berry300 = Color(0xFFFEB7B8)
    val Berry400 = Color(0xFFFF8F93)
    val Berry500 = Color(0xFFEB616D)
    val Berry600 = Color(0xFFBC3E4C)
    val Berry700 = Color(0xFF9B2E3B)
    val Berry800 = Color(0xFF761D29)
    val Berry900 = Color(0xFF530B17)

    val Sky50 = Color(0xFFF0F9FE)
    val Sky100 = Color(0xFFE1F3FF)
    val Sky200 = Color(0xFFC2E8FF)
    val Sky300 = Color(0xFF96D5FB)
    val Sky400 = Color(0xFF6DBFED)
    val Sky500 = Color(0xFF46A0CF)
    val Sky600 = Color(0xFF207AA5)
    val Sky700 = Color(0xFF116287)
    val Sky800 = Color(0xFF034866)
    val Sky900 = Color(0xFF022F45)

    val White = Color(0xFFFFFFFF)
    val Scrim = Color(0x661C1F1D)
    val ScrimStrong = Color(0xA31C1F1D)
    val PressedOverlay = Color(0x14000000)

    val SceneRoomFloor = Color(0xFFC57D33)

    val ExternalNaverGreen = Color(0xFF03C75A)
    val ExternalGoogleBlue = Color(0xFF4285F4)
    val ExternalGoogleGreen = Color(0xFF34A853)
    val ExternalGoogleYellow = Color(0xFFFBBC05)
    val ExternalGoogleRed = Color(0xFFEA4335)
    val ExternalKakaoYellow = Color(0xFFFEE500)
    val ExternalKakaoSymbol = Color(0xFF000000)
    val ExternalKakaoLabel = Color(0xD9000000)
}
