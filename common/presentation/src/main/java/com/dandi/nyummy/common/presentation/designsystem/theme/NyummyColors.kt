package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.dandi.nyummy.common.presentation.designsystem.token.NyummyPalette

/**
 * 화면과 컴포넌트가 쓰는 역할 색. bg, content, border, nav, data, external 그룹으로 나눈다.
 * 피그마 `Nyummy v2 / 2 Semantic` 컬렉션과 1:1로 맞춘다.
 *
 * 디자인 토큰 이름 `bg/action/primary`는 `NyummyTheme.colors.bg.actionPrimary`처럼
 * 첫 마디가 그룹, 나머지가 camelCase 속성이 된다.
 */
@Immutable
class NyummyColors internal constructor(
    val bg: NyummyBackgroundColors,
    val content: NyummyContentColors,
    val border: NyummyBorderColors,
    val nav: NyummyNavColors,
    val data: NyummyDataColors,
    val external: NyummyExternalColors,
)

@Immutable
class NyummyBackgroundColors internal constructor(
    /** 앱 기본 바탕(순백). */
    val canvas: Color,
    /** 카드, 시트 표면. */
    val surface: Color,
    /** 흰 바탕 위 옅은 카드 바탕, 입력 칸. */
    val surfaceSubtle: Color,
    /** 움푹한 영역. */
    val surfaceSunken: Color,
    /** 섹션을 나누는 6px 띠. */
    val sectionBand: Color,
    /** 모달 딤 40%. */
    val scrim: Color,
    /** 눌림 피드백. 표면 위에 덮는다(검정 8%). */
    val pressedOverlay: Color,
    val actionPrimary: Color,
    val actionPrimaryPressed: Color,
    /** 톤 버튼(연초록). 행 안의 버튼에 쓴다. */
    val actionTonal: Color,
    val actionTonalPressed: Color,
    /** 회색 보조 버튼. */
    val actionNeutral: Color,
    val actionNeutralPressed: Color,
    val actionDanger: Color,
    val actionDangerPressed: Color,
    val actionDisabled: Color,
    val selected: Color,
    /** 브랜드 칩, 태그 바탕. */
    val brandSubtle: Color,
    val dangerSubtle: Color,
    val promoSubtle: Color,
    val voiceBubble: Color,
    /** 온보딩 골목, 식사 연출 만화 페이지처럼 어두운 장면 바탕. */
    val sceneDark: Color,
    val sceneRoomFloor: Color,
    /** 보상 연출 무대. 상자가 등장할 때 화면을 덮는 진한 그린(88% 불투명으로 쓴다). */
    val sceneRewardStage: Color,

    /** 튜토리얼 스택이 머지될 때까지만 남긴다. 값은 [brandSubtle]과 같다. */
    @Deprecated("v2에 없는 역할이다. bg.brandSubtle을 쓴다.", ReplaceWith("brandSubtle"))
    val voiceCoach: Color,
)

@Immutable
class NyummyContentColors internal constructor(
    /** 제목, 이름, 값. */
    val primary: Color,
    /** 라벨, 설명. 오류·안내 문구는 이보다 연하게 쓰지 않는다. */
    val secondary: Color,
    /** 메타(부제, 시각, 끼니 태그). 없어도 화면을 이해할 수 있는 정보에만 쓴다(3:1). */
    val tertiary: Color,
    /** 없음·지나감(기록 없는 날, 할인 전 가격). 그 뜻일 때만 쓴다(2.5:1). */
    val quiet: Color,
    /** 비활성(대비 예외). */
    val disabled: Color,
    /** primary, danger 버튼 위 글자. */
    val onAction: Color,
    /** 문장 속 핵심 숫자 강조, 켜진 탭. 작은 글자는 Bold로만 쓴다. */
    val brand: Color,
    /** 냐미 말투, 코인 숫자. */
    val voice: Color,
    val danger: Color,
    val promo: Color,
)

@Immutable
class NyummyBorderColors internal constructor(
    /** 구분선. */
    val subtle: Color,
    /** 흰 바탕 위 흰 카드, 입력 기본 보더. */
    val default: Color,
    val strong: Color,
    /** 입력 포커스 테두리. 흰 바탕과 움푹한 바탕 모두에서 3:1 이상(WCAG 비텍스트 대비). */
    val focus: Color,
    val selected: Color,
    val brandSubtle: Color,
    val danger: Color,
)

/** 하단 탭바 아이콘. */
@Immutable
class NyummyNavColors internal constructor(
    val iconOn: Color,
    val iconOff: Color,
)

@Immutable
class NyummyDataColors internal constructor(
    val progressFill: Color,
    val progressTrack: Color,
    /** 코인 채움 전용. 글자색으로 쓰지 않는다. */
    val coin: Color,
    val recordMarker: Color,
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
        surfaceSubtle = NyummyPalette.Gray50,
        surfaceSunken = NyummyPalette.Gray100,
        sectionBand = NyummyPalette.Gray50,
        scrim = NyummyPalette.Scrim,
        pressedOverlay = NyummyPalette.PressedOverlay,
        actionPrimary = NyummyPalette.Evergreen600,
        actionPrimaryPressed = NyummyPalette.Evergreen700,
        actionTonal = NyummyPalette.Evergreen100,
        actionTonalPressed = NyummyPalette.Evergreen200,
        actionNeutral = NyummyPalette.Gray100,
        actionNeutralPressed = NyummyPalette.Gray200,
        actionDanger = NyummyPalette.Red500,
        actionDangerPressed = NyummyPalette.Red600,
        actionDisabled = NyummyPalette.Gray100,
        selected = NyummyPalette.Evergreen100,
        brandSubtle = NyummyPalette.Evergreen100,
        dangerSubtle = NyummyPalette.Red50,
        promoSubtle = NyummyPalette.Orange50,
        voiceBubble = NyummyPalette.White,
        sceneDark = NyummyPalette.Gray900,
        sceneRoomFloor = NyummyPalette.SceneRoomFloor,
        sceneRewardStage = NyummyPalette.SceneRewardStage,
        voiceCoach = NyummyPalette.Evergreen100,
    ),
    content = NyummyContentColors(
        primary = NyummyPalette.Gray900,
        secondary = NyummyPalette.Gray700,
        tertiary = NyummyPalette.Gray500,
        quiet = NyummyPalette.Gray400,
        disabled = NyummyPalette.Gray300,
        onAction = NyummyPalette.White,
        brand = NyummyPalette.Evergreen600,
        voice = NyummyPalette.EvergreenInk,
        danger = NyummyPalette.Red500,
        promo = NyummyPalette.Orange500,
    ),
    border = NyummyBorderColors(
        subtle = NyummyPalette.Gray150,
        default = NyummyPalette.Gray150,
        strong = NyummyPalette.Gray300,
        focus = NyummyPalette.Evergreen600,
        selected = NyummyPalette.Evergreen600,
        brandSubtle = NyummyPalette.Evergreen300,
        danger = NyummyPalette.Red500,
    ),
    nav = NyummyNavColors(
        iconOn = NyummyPalette.Evergreen600,
        iconOff = NyummyPalette.Sage400,
    ),
    data = NyummyDataColors(
        progressFill = NyummyPalette.Evergreen600,
        progressTrack = NyummyPalette.Gray100,
        coin = NyummyPalette.Gold300,
        recordMarker = NyummyPalette.Evergreen600,
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
