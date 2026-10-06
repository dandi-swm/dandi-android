package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/** 눌림 축소 배율. 버튼 0.96, 원형 아이콘 버튼 0.94. */
internal object NyummyPressScale {
    const val Default = 0.96f
    const val Circle = 0.94f
    const val DurationMillis = 150
}

/**
 * 냐미 공통 눌림 피드백. 물결(ripple) 대신 축소로 반응한다.
 * 색 변화(검정 8% 덮기 등)는 각 컴포넌트가 [pressed] 값을 보고 직접 그린다.
 */
@Composable
internal fun Modifier.nyummyPressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    role: Role,
    onClick: () -> Unit,
    pressedScale: Float = NyummyPressScale.Default,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(NyummyPressScale.DurationMillis),
        label = "nyummyPressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

@Composable
internal fun rememberNyummyInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }
