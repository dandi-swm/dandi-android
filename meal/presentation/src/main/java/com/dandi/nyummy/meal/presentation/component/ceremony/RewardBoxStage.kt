package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.meal.presentation.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 냐미가 보낸 답례 상자가 진한 그린 무대 위에 등장하는 장면입니다. 우편함 상자 열기 연출과 같은 문법(무대 딤,
 * 뒤에서 도는 빛줄기, 통통 튀는 빛나는 상자, 섬광과 색종이)을 쓰되, 상자는 여기서 열지 않습니다.
 *
 * 상자는 늘 가장 낮은 등급 모습으로 나와 SILVER 는 한 번, GOLD 는 두 번 섬광과 함께 바뀝니다.
 * 상자의 화면 위치는 [onBoxPositioned] 로 알려 줘 다음 장면의 비행 출발점으로 씁니다.
 *
 * @param leaving true 가 되면 무대가 걷히고 상자만 남는다(상자는 다음 장면이 이어서 그린다).
 */
@Composable
internal fun RewardBoxStage(
    grade: RewardBoxGrade,
    leaving: Boolean,
    reducedMotion: Boolean,
    onBoxPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val latestOnBoxPositioned by rememberUpdatedState(onBoxPositioned)

    val dim = remember { Animatable(0f) }
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val tilt = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    var shownGrade by remember { mutableStateOf(if (reducedMotion) grade else RewardBoxGrade.BRONZE) }
    var showLabel by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(leaving) {
        if (leaving) {
            dim.animateTo(0f, tween(StageLeaveMillis))
            return@LaunchedEffect
        }
        dim.animateTo(1f, tween(StageEnterMillis.toInt()))
        if (reducedMotion) return@LaunchedEffect
        appear.animateTo(1f, spring(dampingRatio = AppearDamping, stiffness = Spring.StiffnessMediumLow))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        repeat(grade.upgradeCount + 1) { index ->
            tilt.animateTo(0f, shakeSpec(ShakeDegrees + index * ShakeDegreesStep))
        }
        repeat(grade.upgradeCount) {
            // 승급: 섬광이 덮이는 순간 다음 등급으로 바뀌고, 색종이가 터지며 펑 커졌다 돌아온다.
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(FlashMillis))
            }
            launch {
                confetti.snapTo(0f)
                confetti.animateTo(1f, tween(ConfettiMillis, easing = FastOutSlowInEasing))
            }
            shownGrade = shownGrade.next()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            pop.snapTo(PopScale)
            pop.animateTo(1f, spring(dampingRatio = PopDamping, stiffness = Spring.StiffnessMedium))
            delay(UpgradeGapMillis)
        }
        showLabel = true
    }

    val stage = NyummyTheme.colors.bg.sceneRewardStage
    val rayTurn = rememberInfiniteTransition(label = "RewardRays")
    val rayDegrees by rayTurn.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RayTurnMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "rewardRayDegrees",
    )
    val idle = rememberInfiniteTransition(label = "RewardIdle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BobMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rewardBob",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val stageWidth = maxWidth
        val stageHeight = maxHeight
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dim.value * StageAlpha }
                .background(stage),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dim.value },
        ) {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_vfx_light_rays),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = BoxOffsetY)
                    .size(RaysSize)
                    .graphicsLayer {
                        rotationZ = rayDegrees
                        alpha = appear.value
                    },
            )
            if (confetti.value > 0f && confetti.value < 1f) {
                Image(
                    painter = painterResource(CommonR.drawable.nyummy_vfx_confetti),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = stageWidth, height = stageHeight)
                        .graphicsLayer {
                            val t = confetti.value
                            val scale = ConfettiStartScale + (1f - ConfettiStartScale) * t
                            scaleX = scale
                            scaleY = scale
                            translationY = with(density) { ConfettiFall.toPx() } * t
                            alpha = 1f - t * t
                        },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = stageHeight * LabelTopFraction)
                    .graphicsLayer { alpha = if (showLabel) 1f else 0f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                OutlinedHeadline(text = stringResource(grade.labelRes))
                NyummyText(
                    text = stringResource(R.string.meal_ceremony_box_badge),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                    modifier = Modifier
                        .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                        .padding(horizontal = NyummyTheme.spacing.s12, vertical = NyummyTheme.spacing.s4),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = BoxOffsetY)
                .size(BoxSize)
                .onGloballyPositioned { latestOnBoxPositioned(it.boundsInRoot().center) }
                .graphicsLayer {
                    val scale = appear.value * pop.value
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt.value
                    translationY = -BobLift.toPx() * bob * appear.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    alpha = if (leaving) 0f else 1f
                },
        ) {
            Image(
                painter = painterResource(shownGrade.boxRes),
                contentDescription = stringResource(R.string.meal_ceremony_box_content_description),
                modifier = Modifier.fillMaxSize(),
            )
            val flashColor = NyummyTheme.colors.bg.surface
            Canvas(Modifier.fillMaxSize()) {
                val a = flash.value
                if (a > 0f) {
                    drawCircle(
                        color = flashColor.copy(alpha = a * FlashAlpha),
                        radius = size.maxDimension * (FlashRadiusEnd - (FlashRadiusEnd - FlashRadiusStart) * a),
                    )
                }
            }
        }
    }
}

/** 상자 무대에서 등급을 다 보여 주기까지 걸리는 시간. */
internal fun RewardBoxGrade.stageMillis(): Long =
    StageEnterMillis + AppearMillis + (upgradeCount + 1) * ShakeMillis +
        upgradeCount * (UpgradeGapMillis + PopSettleMillis) + LabelHoldMillis

/** 흰 글자에 무대색 테두리를 두른 큰 제목. 우편함 상자 연출의 제목과 같은 모양이다. */
@Composable
private fun OutlinedHeadline(text: String) {
    val style = NyummyTheme.typography.displayL
    val strokePx = with(LocalDensity.current) { HeadlineStroke.toPx() }
    Box {
        NyummyText(
            text = text,
            style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
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

internal val RewardBoxGrade.upgradeCount: Int
    get() = when (this) {
        RewardBoxGrade.SILVER -> 1
        RewardBoxGrade.GOLD -> 2
        else -> 0
    }

private fun RewardBoxGrade.next(): RewardBoxGrade = when (this) {
    RewardBoxGrade.BRONZE -> RewardBoxGrade.SILVER
    else -> RewardBoxGrade.GOLD
}

@get:DrawableRes
internal val RewardBoxGrade.boxRes: Int
    get() = when (this) {
        RewardBoxGrade.SILVER -> R.drawable.meal_reward_box_silver
        RewardBoxGrade.GOLD -> R.drawable.meal_reward_box_gold
        else -> R.drawable.meal_reward_box_bronze
    }

@get:StringRes
private val RewardBoxGrade.labelRes: Int
    get() = when (this) {
        RewardBoxGrade.SILVER -> R.string.meal_ceremony_box_silver
        RewardBoxGrade.GOLD -> R.string.meal_ceremony_box_gold
        else -> R.string.meal_ceremony_box_bronze
    }

/** 좌우로 한 번 흔들고 제자리로 온다. */
private fun shakeSpec(degrees: Float) = keyframes {
    durationMillis = ShakeMillis.toInt()
    -degrees at (ShakeMillis / 4).toInt() using LinearEasing
    degrees at (ShakeMillis * 3 / 4).toInt() using LinearEasing
    0f at ShakeMillis.toInt()
}

/** 상자 크기. 다음 장면에서 날아가기 시작할 때도 같은 크기로 시작한다. */
internal val RewardBoxSize = 168.dp
private val BoxSize = RewardBoxSize
private val BoxOffsetY = 24.dp
private val RaysSize = 420.dp
private val BobLift = 6.dp
private val HeadlineStroke = 6.dp
private val ConfettiFall = 120.dp

private const val StageAlpha = 0.88f
private const val StageEnterMillis = 220L
private const val StageLeaveMillis = 260
private const val AppearMillis = 450L
private const val AppearDamping = 0.5f
private const val ShakeMillis = 280L
private const val ShakeDegrees = 6f
private const val ShakeDegreesStep = 3f
private const val PopScale = 1.28f
private const val PopDamping = 0.45f
private const val PopSettleMillis = 220L
private const val UpgradeGapMillis = 260L
private const val FlashMillis = 280
private const val FlashAlpha = 0.9f
private const val FlashRadiusStart = 0.5f
private const val FlashRadiusEnd = 1.3f
private const val ConfettiMillis = 1_000
private const val ConfettiStartScale = 0.55f
private const val LabelHoldMillis = 900L
private const val LabelTopFraction = 0.12f
private const val RayTurnMillis = 12_000
private const val BobMillis = 900
