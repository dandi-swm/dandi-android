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
 *
 * @param singleClick true면 누름을 [NyummyTheme]이 내려 준 통로([LocalNyummySingleClick])에 넣는다. 첫 누름을 실행한 뒤
 *   400ms 동안 같은 통로로 들어온 다른 누름은 버린다. 기본값이 없어 부르는 컴포넌트가 반드시 정한다.
 *   화면 이동·제출·시트 열기 같은 행동 버튼만 true, 고르기·토글·수량 조절·넘기기는 false
 */
@Composable
internal fun Modifier.nyummyPressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    role: Role,
    onClick: () -> Unit,
    pressedScale: Float = NyummyPressScale.Default,
    singleClick: Boolean,
): Modifier {
    val clicks = LocalNyummySingleClick.current
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
            // 통로가 없으면(테마 밖·꺼짐) 그냥 실행한다.
            onClick = { if (singleClick && clicks != null) clicks.tryEmit(onClick) else onClick() },
        )
}

@Composable
internal fun rememberNyummyInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * feature 화면에서 직접 그리는 누를 수 있는 요소(브랜드 버튼, 소셜 원형 버튼 등)에 냐미 눌림 피드백을 준다.
 * 물결 없이 눌리는 동안 [pressedScale]배로 줄어든다.
 * 눌린 동안 바탕색을 바꾸려면 [interactionSource]를 넘기고 `collectIsPressedAsState()`로 읽는다.
 *
 * @param singleClick 따닥 누름 방지. 기본은 끔. 화면 이동·제출·구매·삭제 확정·시트 열기처럼
 *   한 번만 실행돼야 하는 행동에만 true로 준다. 고르기·토글·수량 조절·넘기기는 기본값(false)을 쓴다
 */
@Composable
fun Modifier.nyummyClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    pressedScale: Float = NyummyPressScale.Default,
    interactionSource: MutableInteractionSource? = null,
    singleClick: Boolean = false,
): Modifier = nyummyPressable(
    interactionSource = interactionSource ?: rememberNyummyInteractionSource(),
    enabled = enabled,
    role = role,
    onClick = onClick,
    pressedScale = pressedScale,
    singleClick = singleClick,
)
