package com.dandi.nyummy.common.presentation.reward

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/*
 * 보상 상자 무대(우편함 상자 열기 등)가 쓰는 효과. 진한 그린 무대 위에서 뒤에서 도는 빛줄기, 상자 뒤 빛무리,
 * 열리는 순간 터지는 빛, 터지는 색종이, 무대색 테두리를 두른 큰 제목.
 */

/** 개발자 옵션이나 접근성에서 애니메이션을 끈 기기인지. 끈 기기에는 섬광, 흔들림, 컷인 없이 정지컷만 보여 준다. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** 상자 뒤에서 천천히 도는 빛줄기. [alpha]와 [scale]은 그릴 때마다 읽어 애니메이션 값을 바로 넘겨도 된다. */
@Composable
fun RewardLightRays(
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    scale: () -> Float = { 1f },
) {
    val turn = rememberInfiniteTransition(label = "RewardRays")
    val degrees by turn.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RayTurnMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "rewardRayDegrees",
    )
    Image(
        painter = painterResource(R.drawable.nyummy_vfx_light_rays),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            rotationZ = degrees
            this.alpha = alpha()
            val s = scale()
            scaleX = s
            scaleY = s
        },
    )
}

/**
 * 한 번 터지는 색종이. [progress]가 0에서 1로 가는 동안 작게 시작해 퍼지며 아래로 조금 떨어지고 흐려진다.
 * 0이거나 1이면 그리지 않는다. 보통 무대 전체 크기로 가운데에 둔다.
 */
@Composable
fun RewardConfetti(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val t = progress()
    if (t <= 0f || t >= 1f) return
    Image(
        painter = painterResource(R.drawable.nyummy_vfx_confetti),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            val now = progress()
            val scale = ConfettiStartScale + (1f - ConfettiStartScale) * now
            scaleX = scale
            scaleY = scale
            translationY = ConfettiFall.toPx() * now
            this.alpha = 1f - now * now
        },
    )
}

/**
 * 상자가 열리는 순간 [center]에서 터져 퍼지는 흰 빛. [progress] 1에서 가장 밝고 작으며, 0으로 가며 넓게 퍼지고 옅어진다.
 * 가장자리로 갈수록 투명해져 둥근 테두리가 보이지 않는다. 보통 무대 전체 크기로 둔다.
 *
 * @param center 빛이 터지는 자리(그리는 영역 안의 위치, px)
 */
@Composable
fun RewardBurstFlash(
    progress: () -> Float,
    center: () -> Offset,
    modifier: Modifier = Modifier,
) {
    val white = NyummyTheme.colors.bg.surface
    Canvas(modifier) {
        val a = progress()
        if (a <= 0f) return@Canvas
        val reach = size.maxDimension * (BurstReachEnd - (BurstReachEnd - BurstReachStart) * a)
        val at = center()
        drawCircle(
            brush = Brush.radialGradient(
                0f to white.copy(alpha = a),
                BurstCoreStop to white.copy(alpha = a * BurstMidAlpha),
                1f to white.copy(alpha = 0f),
                center = at,
                radius = reach,
            ),
            radius = reach,
            center = at,
        )
    }
}

/**
 * 닫힌 상자 뒤에서 숨 쉬듯 밝아졌다 어두워지는 둥근 빛무리. 가운데는 흰빛, 바깥은 금빛으로 번진다.
 * [alpha]는 그릴 때마다 읽는다.
 */
@Composable
fun RewardGlow(
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
) {
    val pulse = rememberInfiniteTransition(label = "RewardGlow")
    val breath by pulse.animateFloat(
        initialValue = GlowLow,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GlowPulseMillis), RepeatMode.Reverse),
        label = "rewardGlowBreath",
    )
    val white = NyummyTheme.colors.bg.surface
    val gold = NyummyTheme.colors.data.coin
    Canvas(modifier) {
        val a = alpha() * breath
        if (a <= 0f) return@Canvas
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                0f to white.copy(alpha = GlowCoreAlpha * a),
                GlowGoldStop to gold.copy(alpha = GlowGoldAlpha * a),
                1f to gold.copy(alpha = 0f),
                center = center,
                radius = radius,
            ),
            radius = radius,
        )
    }
}

/**
 * 흰 글자에 무대색 테두리를 두른 제목. 상자 무대 위의 제목과 받은 보상 이름에 쓴다.
 * 밝은 빛줄기 위에서도 읽히게 테두리를 두른다.
 */
@Composable
fun RewardHeadline(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NyummyTheme.typography.displayL,
    stroke: Dp = HeadlineStroke,
) {
    val strokePx = with(LocalDensity.current) { stroke.toPx() }
    Box(modifier) {
        // 테두리만 그리는 사본이라 화면 읽기에서는 빼고 한 번만 읽힌다.
        NyummyText(
            text = text,
            style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
            modifier = Modifier.clearAndSetSemantics { },
            color = NyummyTheme.colors.bg.sceneRewardStage,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        NyummyText(
            text = text,
            style = style,
            color = NyummyTheme.colors.bg.surface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private val HeadlineStroke = 6.dp
private val ConfettiFall = 120.dp

private const val RayTurnMillis = 12_000
private const val GlowPulseMillis = 900
private const val GlowLow = 0.7f
private const val GlowCoreAlpha = 0.95f
private const val GlowGoldStop = 0.35f
private const val GlowGoldAlpha = 0.7f
private const val ConfettiStartScale = 0.55f
private const val BurstReachStart = 0.45f
private const val BurstReachEnd = 1.1f
private const val BurstCoreStop = 0.35f
private const val BurstMidAlpha = 0.85f
