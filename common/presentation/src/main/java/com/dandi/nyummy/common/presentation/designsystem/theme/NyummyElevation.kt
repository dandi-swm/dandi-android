package com.dandi.nyummy.common.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.token.NyummyPalette

/** Figma 그림자 효과 1개. blur, offset, color를 그대로 옮긴다. */
@Immutable
class NyummyShadow internal constructor(
    val color: Color,
    val blur: Dp,
    val offsetY: Dp,
)

/**
 * Figma effect style `elevation/soft`, `elevation/float`.
 *
 * 버튼과 카드는 그림자를 쓰지 않는다. 떠 있는 요소(하단 내비, 시트, 스낵바)와
 * 일러스트 위 카드에만 쓴다.
 */
@Immutable
class NyummyElevation internal constructor(
    /** 일러스트 위 카드. */
    val soft: NyummyShadow,
    /** 시트, 플로팅 요소. */
    val float: NyummyShadow,
)

internal val DefaultNyummyElevation = NyummyElevation(
    soft = NyummyShadow(color = NyummyPalette.Gray900.copy(alpha = 0x1A / 255f), blur = 12.dp, offsetY = 4.dp),
    float = NyummyShadow(color = NyummyPalette.Gray900.copy(alpha = 0x24 / 255f), blur = 24.dp, offsetY = 8.dp),
)

/** Figma drop shadow를 blur 값 그대로 그린다. `Modifier.shadow`의 elevation 근사를 쓰지 않는다. */
@Stable
fun Modifier.nyummyShadow(shape: Shape, shadow: NyummyShadow): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = shadow.blur,
        offset = DpOffset(0.dp, shadow.offsetY),
        color = shadow.color,
    ),
)
