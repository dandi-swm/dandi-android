package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyShadow
import com.dandi.nyummy.common.presentation.designsystem.token.NyummyPalette

/**
 * 피그마 v2 컴포넌트에 직접 걸린 그림자 중 그림자 토큰(soft, float)에 없는 값.
 *
 * 피그마에서 효과 스타일이 아니라 컴포넌트마다 정한 값이라, [NyummyComponentDimens]처럼 토큰에 넣지 않고 여기에 모은다.
 * 화면 코드에서는 쓰지 않는다.
 */
internal object NyummyComponentShadows {
    /** 하단 고정 CTA가 위로 드리우는 옅은 그림자(검정 6%). */
    val BottomCta = NyummyShadow(color = NyummyPalette.Black.copy(alpha = 0x0F / 255f), blur = 16.dp, offsetY = (-4).dp)

    /** 하단 내비가 위로 드리우는 그림자(검정 10%). */
    val BottomNav = NyummyShadow(color = NyummyPalette.Black.copy(alpha = 0x1A / 255f), blur = 20.dp, offsetY = (-4).dp)

    /** 하단 내비 가운데 초록 버튼 그림자(브랜드 그린 32%). */
    val BottomNavAction = NyummyShadow(color = NyummyPalette.Evergreen600.copy(alpha = 0x52 / 255f), blur = 12.dp, offsetY = 4.dp)

    /** Segmented 고른 칸(검정 8%). */
    val SegmentedOption = NyummyShadow(color = NyummyPalette.Black.copy(alpha = 0x14 / 255f), blur = 4.dp, offsetY = 1.dp)

    /** Snackbar, Toast 아래 딱딱한 그림자(흐림 없이 4 아래로, border/default 색). */
    val Snackbar = NyummyShadow(color = NyummyPalette.Gray150, blur = 0.dp, offsetY = 4.dp)

    /** Switch 흰 썸(검정 15%). */
    val SwitchThumb = NyummyShadow(color = NyummyPalette.Black.copy(alpha = 0x26 / 255f), blur = 3.dp, offsetY = 1.dp)
}
