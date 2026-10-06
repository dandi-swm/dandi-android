package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.dandi.nyummy.common.presentation.designsystem.token.NyummyPalette

/**
 * 화면과 컴포넌트가 쓰는 역할 색. bg, content, border, data, external 그룹으로 나눈다.
 *
 * 디자인 토큰 이름 `bg/action/primary`는 `NyummyTheme.colors.bg.actionPrimary`처럼
 * 첫 마디가 그룹, 나머지가 camelCase 속성이 된다.
 */
@Immutable
class NyummyColors internal constructor(
    val bg: NyummyBackgroundColors,
    val content: NyummyContentColors,
    val border: NyummyBorderColors,
    val data: NyummyDataColors,
    val external: NyummyExternalColors,
)

@Immutable
class NyummyBackgroundColors internal constructor(
    /** 앱 기본 바탕(순백). */
    val canvas: Color,
    /** 카드, 시트 표면. */
    val surface: Color,
    /** 움푹한 영역, 입력 배경. */
    val surfaceSunken: Color,
    /** 스낵바 등 반전 표면. */
    val surfaceInverse: Color,
    /** 모달 딤 40%. */
    val scrim: Color,
    val scrimStrong: Color,
    /** 눌림 피드백. 표면 위에 덮는다(검정 8%). */
    val pressedOverlay: Color,
    val actionPrimary: Color,
    val actionPrimaryPressed: Color,
    val actionSecondary: Color,
    val actionSecondaryPressed: Color,
    val actionDanger: Color,
    val actionDangerPressed: Color,
    val actionDisabled: Color,
    val selected: Color,
    val voiceBubble: Color,
    val voiceCoach: Color,
    val successSubtle: Color,
    val warningSubtle: Color,
    val dangerSubtle: Color,
    val infoSubtle: Color,
    val promoSubtle: Color,
    val sceneRoomFloor: Color,
)

@Immutable
class NyummyContentColors internal constructor(
    /** 본문, 제목. */
    val primary: Color,
    val secondary: Color,
    /** 힌트, 캡션(4.5:1 하한). */
    val tertiary: Color,
    /** 비활성(대비 예외). */
    val disabled: Color,
    /** primary, danger 버튼 위 글자. */
    val onAction: Color,
    val onCoin: Color,
    val onInverse: Color,
    /** 링크, 브랜드 강조. */
    val brand: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val promo: Color,
)

@Immutable
class NyummyBorderColors internal constructor(
    /** 구분선. */
    val subtle: Color,
    /** 카드, 입력 기본 보더. */
    val default: Color,
    val strong: Color,
    /** 입력 포커스 테두리. 흰 바탕과 움푹한 바탕 모두에서 3:1 이상(WCAG 비텍스트 대비). */
    val focus: Color,
    val selected: Color,
    val danger: Color,
)

@Immutable
class NyummyDataColors internal constructor(
    val progressFill: Color,
    val progressTrack: Color,
    val streak: Color,
    /** 코인 채움 전용. 글자색으로 쓰지 않는다. */
    val coin: Color,
    val recordMarker: Color,
    val nutrientCarb: Color,
    val nutrientProtein: Color,
    val nutrientFat: Color,
)

/** 소셜 로그인 버튼처럼 외부 브랜드 가이드를 따라야 하는 곳에서만 쓴다. */
@Immutable
class NyummyExternalColors internal constructor(
    val naverGreen: Color,
    val googleBlue: Color,
    val googleGreen: Color,
    val googleYellow: Color,
    val googleRed: Color,
    val kakaoYellow: Color,
    val kakaoSymbol: Color,
    val kakaoLabel: Color,
)

internal val DefaultNyummyColors = NyummyColors(
    bg = NyummyBackgroundColors(
        canvas = NyummyPalette.White,
        surface = NyummyPalette.White,
        surfaceSunken = NyummyPalette.Gray50,
        surfaceInverse = NyummyPalette.Gray900,
        scrim = NyummyPalette.Scrim,
        scrimStrong = NyummyPalette.ScrimStrong,
        pressedOverlay = NyummyPalette.PressedOverlay,
        actionPrimary = NyummyPalette.Evergreen600,
        actionPrimaryPressed = NyummyPalette.Evergreen700,
        actionSecondary = NyummyPalette.Gray100,
        actionSecondaryPressed = NyummyPalette.Gray200,
        actionDanger = NyummyPalette.Berry600,
        actionDangerPressed = NyummyPalette.Berry700,
        actionDisabled = NyummyPalette.Gray100,
        selected = NyummyPalette.Evergreen100,
        voiceBubble = NyummyPalette.White,
        voiceCoach = NyummyPalette.Evergreen50,
        successSubtle = NyummyPalette.Evergreen100,
        warningSubtle = NyummyPalette.Gold100,
        dangerSubtle = NyummyPalette.Berry100,
        infoSubtle = NyummyPalette.Gray100,
        promoSubtle = NyummyPalette.Berry50,
        sceneRoomFloor = NyummyPalette.SceneRoomFloor,
    ),
    content = NyummyContentColors(
        primary = NyummyPalette.Gray900,
        secondary = NyummyPalette.Gray700,
        tertiary = NyummyPalette.Gray600,
        disabled = NyummyPalette.Gray400,
        onAction = NyummyPalette.White,
        onCoin = NyummyPalette.Gray900,
        onInverse = NyummyPalette.White,
        brand = NyummyPalette.Evergreen700,
        success = NyummyPalette.Evergreen700,
        warning = NyummyPalette.Gold700,
        danger = NyummyPalette.Berry600,
        info = NyummyPalette.Gray700,
        promo = NyummyPalette.Berry600,
    ),
    border = NyummyBorderColors(
        subtle = NyummyPalette.Gray100,
        default = NyummyPalette.Gray200,
        strong = NyummyPalette.Gray400,
        focus = NyummyPalette.Evergreen600,
        selected = NyummyPalette.Evergreen600,
        danger = NyummyPalette.Berry500,
    ),
    data = NyummyDataColors(
        progressFill = NyummyPalette.Evergreen600,
        progressTrack = NyummyPalette.Evergreen100,
        streak = NyummyPalette.Evergreen600,
        coin = NyummyPalette.Gold400,
        recordMarker = NyummyPalette.Evergreen600,
        nutrientCarb = NyummyPalette.Gold500,
        nutrientProtein = NyummyPalette.Evergreen500,
        nutrientFat = NyummyPalette.Berry500,
    ),
    external = NyummyExternalColors(
        naverGreen = NyummyPalette.ExternalNaverGreen,
        googleBlue = NyummyPalette.ExternalGoogleBlue,
        googleGreen = NyummyPalette.ExternalGoogleGreen,
        googleYellow = NyummyPalette.ExternalGoogleYellow,
        googleRed = NyummyPalette.ExternalGoogleRed,
        kakaoYellow = NyummyPalette.ExternalKakaoYellow,
        kakaoSymbol = NyummyPalette.ExternalKakaoSymbol,
        kakaoLabel = NyummyPalette.ExternalKakaoLabel,
    ),
)
