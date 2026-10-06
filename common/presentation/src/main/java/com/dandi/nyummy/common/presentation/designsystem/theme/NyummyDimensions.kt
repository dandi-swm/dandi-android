package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 공통 치수 토큰(간격, 모서리, 테두리, 크기).
 *
 * 눈금 밖 값은 쓰지 않는다. 한 곳에서만 쓰는 값이어도 아래 눈금 중 하나로 맞춘다.
 */
@Immutable
class NyummySpacing internal constructor(
    val s2: Dp,
    val s4: Dp,
    val s8: Dp,
    val s12: Dp,
    val s16: Dp,
    val s20: Dp,
    val s24: Dp,
    val s32: Dp,
    val s40: Dp,
    val s48: Dp,
    /** 화면 좌우 여백. */
    val gutter: Dp,
)

@Immutable
class NyummyRadius internal constructor(
    /** 칩, 태그. */
    val xs: Dp,
    /** 버튼 M, 입력. */
    val s: Dp,
    /** 버튼 L, 카드, 말풍선. */
    val m: Dp,
    /** 시트, 다이얼로그. */
    val l: Dp,
    /** pill, 원형. */
    val full: Dp,
)

@Immutable
class NyummyBorderWidth internal constructor(
    /** 구분선 전용. */
    val hairline: Dp,
    /** 카드, 입력 기본. */
    val bold: Dp,
)

@Immutable
class NyummySize internal constructor(
    val touchTarget: Dp,
    val iconXs: Dp,
    val iconS: Dp,
    val iconM: Dp,
    val iconL: Dp,
    val characterXs: Dp,
    val characterS: Dp,
    val characterM: Dp,
    val characterL: Dp,
    val characterHero: Dp,
    val buttonM: Dp,
    val buttonL: Dp,
    val bottomNav: Dp,
)

internal val DefaultNyummySpacing = NyummySpacing(
    s2 = 2.dp,
    s4 = 4.dp,
    s8 = 8.dp,
    s12 = 12.dp,
    s16 = 16.dp,
    s20 = 20.dp,
    s24 = 24.dp,
    s32 = 32.dp,
    s40 = 40.dp,
    s48 = 48.dp,
    gutter = 20.dp,
)

internal val DefaultNyummyRadius = NyummyRadius(
    xs = 8.dp,
    s = 12.dp,
    m = 16.dp,
    l = 24.dp,
    full = 999.dp,
)

internal val DefaultNyummyBorderWidth = NyummyBorderWidth(
    hairline = 1.dp,
    bold = 2.dp,
)

internal val DefaultNyummySize = NyummySize(
    touchTarget = 48.dp,
    iconXs = 14.dp,
    iconS = 16.dp,
    iconM = 20.dp,
    iconL = 24.dp,
    characterXs = 48.dp,
    characterS = 72.dp,
    characterM = 120.dp,
    characterL = 160.dp,
    characterHero = 224.dp,
    buttonM = 48.dp,
    buttonL = 56.dp,
    bottomNav = 77.dp,
)
