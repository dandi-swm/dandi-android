package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.ui.unit.dp

/**
 * Figma 컴포넌트에 직접 적힌 치수 중 `Nyummy / 3 Dimension` 변수로 묶이지 않은 값.
 *
 * 토큰(NyummyTheme)은 Figma 변수와 1:1로 유지하고, 컴포넌트 내부에서만 쓰는 고정 치수는 여기에 모은다.
 * 화면 코드에서는 쓰지 않는다.
 */
internal object NyummyComponentDimens {
    /** Button S, Chip 높이. */
    val CompactControlHeight = 36.dp

    /** Button S, Chip 좌우 여백. */
    val CompactControlHorizontalPadding = 14.dp
}
