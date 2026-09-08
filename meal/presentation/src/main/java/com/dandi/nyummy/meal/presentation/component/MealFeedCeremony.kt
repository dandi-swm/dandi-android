package com.dandi.nyummy.meal.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.presentation.R
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 촬영본이 게임 아이템으로 변환되는 먹이기 세리머니입니다.
 *
 * 차지업(진동) → 계단식 해상도 크런치 → 스프링 축소 → 아이템 카드 부유의 4단계로
 * 진행되며, 체인 디코드에 실패하면 기존 촬영본 표시로 조용히 대체됩니다.
 * 애니메이션 값은 모두 draw/graphicsLayer 단계에서만 읽어 리컴포지션을 만들지 않습니다.
 */
@Composable
internal fun MealFeedCeremony(
    photoPath: String,
    chainResult: MealPixelChainResult,
    showSuccessCaption: Boolean,
    onIdleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val radius = DesignSystemThemeImpl.designSystemRadius
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    val haptic = LocalHapticFeedback.current
    val photoDescription =
        stringResource(R.string.meal_record_captured_photo_content_description)

    val latestChainResult by rememberUpdatedState(chainResult)
    val latestOnIdleChanged by rememberUpdatedState(onIdleChanged)

    var stage by remember { mutableStateOf(CeremonyStage.Charging) }
    val charge = remember { Animatable(0f) }
    val crunchLevel = remember { mutableIntStateOf(0) }
    val pop = remember { Animatable(1f) }
    val shrink = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // ── 1) 차지업: 힘을 모으듯 떨리며 살짝 부푼다 ──
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        charge.animateTo(1f, tween(ChargeDurationMillis, easing = FastOutSlowInEasing))

        // 확인 단계에서 프리웜되므로 보통 즉시 통과한다.
        val ready = snapshotFlow { latestChainResult }
            .first { it !is MealPixelChainResult.Loading }
        if (ready !is MealPixelChainResult.Ready) {
            stage = CeremonyStage.Fallback
            latestOnIdleChanged(true)
            return@LaunchedEffect
        }

        // ── 2) 크런치: 해상도가 '딱딱' 떨어지며 가속, 스텝마다 팝+햅틱 틱 ──
        stage = CeremonyStage.Crunching
        CrunchStepDelaysMillis.forEachIndexed { index, stepDelay ->
            crunchLevel.intValue = index + 1
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            launch {
                pop.snapTo(CrunchPopScale)
                pop.animateTo(
                    1f,
                    spring(dampingRatio = CrunchPopDamping, stiffness = Spring.StiffnessHigh),
                )
            }
            delay(stepDelay)
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // ── 3) 축소: 오버슈트 스프링이 그대로 바운스가 된다 ──
        stage = CeremonyStage.Shrinking
        shrink.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = ShrinkStiffness,
            ),
        )

        // ── 4) 부유: 아이템 카드로 스왑 ──
        stage = CeremonyStage.Idle
        latestOnIdleChanged(true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (stage) {
            CeremonyStage.Fallback -> AsyncImage(
                model = File(photoPath),
                contentDescription = photoDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            CeremonyStage.Idle -> {
                val chain = (chainResult as? MealPixelChainResult.Ready)?.chain
                if (chain != null) {
                    MealFeedItemCard(
                        image = chain.levels.last(),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            else -> {
                val chain = (chainResult as? MealPixelChainResult.Ready)?.chain
                val transformModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val shrinkClamped = shrink.value.coerceIn(0f, 1f)
                        val chargeShake =
                            sin(charge.value * ChargeShakeCycles * 2f * PI.toFloat()) *
                                ChargeShakeMaxDegrees * charge.value
                        val level = crunchLevel.intValue
                        val crunchJitter = when {
                            level == 0 -> 0f
                            level % 2 == 0 -> CrunchJitterDegrees
                            else -> -CrunchJitterDegrees
                        }
                        rotationZ = (chargeShake + crunchJitter) * (1f - shrinkClamped)
                        val scale = pop.value *
                            (1f + ChargeScaleGain * charge.value * (1f - shrinkClamped))
                        scaleX = scale
                        scaleY = scale
                    }
                if (chain == null) {
                    // 디코드 대기 중에도 원본 사진이 같은 변환으로 떨리게 해 끊김을 없앤다.
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = photoDescription,
                        modifier = transformModifier,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Canvas(
                        transformModifier.semantics { contentDescription = photoDescription },
                    ) {
                        drawCeremonyFrame(
                            chain = chain,
                            level = crunchLevel.intValue,
                            shrinkValue = shrink.value,
                            cornerFromPx = radius.radius24.toPx(),
                            cornerToPx = radius.radius16.toPx(),
                            borderColor = colors.bgActionRewardDefault,
                            borderWidthPx = CeremonyBorderWidth.toPx(),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccessCaption,
            enter = fadeIn(tween(SuccessCaptionFadeMillis)) +
                scaleIn(
                    initialScale = SuccessCaptionStartScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(FeedItemCardSize / 2 + spacing.space24)),
        ) {
            DandiText(
                text = stringResource(R.string.meal_record_submit_success),
                // 아이템 카드 위쪽은 밝은 bgMealPhoto 배경이므로 기본 전경색으로 대비를 확보하고,
                // 스낵바를 대체하는 유일한 성공 텍스트라 등장을 접근성으로도 고지한다.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
        }
    }
}

private enum class CeremonyStage { Charging, Crunching, Shrinking, Idle, Fallback }

/** 풀프레임→아이템 카드 사이를 보간하며 현재 크런치 레벨을 커버 크롭으로 그린다. */
private fun DrawScope.drawCeremonyFrame(
    chain: MealPixelChain,
    level: Int,
    shrinkValue: Float,
    cornerFromPx: Float,
    cornerToPx: Float,
    borderColor: Color,
    borderWidthPx: Float,
) {
    val levelIndex = level.coerceIn(0, chain.levels.lastIndex)
    val image = chain.levels[levelIndex]
    val t = shrinkValue.coerceAtMost(MaxShrinkOvershoot)

    val cardSide = FeedItemCardSize.toPx()
    val fullRect = Rect(Offset.Zero, size)
    val half = cardSide / 2f
    val cardRect = Rect(
        center.x - half,
        center.y - half,
        center.x + half,
        center.y + half,
    )
    val dst = lerp(fullRect, cardRect, t)

    val dstAspect = dst.width / dst.height
    val srcAspect = image.width.toFloat() / image.height.toFloat()
    val (srcWidth, srcHeight) = if (srcAspect > dstAspect) {
        (image.height * dstAspect).roundToInt().coerceIn(1, image.width) to image.height
    } else {
        image.width to (image.width / dstAspect).roundToInt().coerceIn(1, image.height)
    }
    val srcOffset = IntOffset((image.width - srcWidth) / 2, (image.height - srcHeight) / 2)

    val corner = lerp(cornerFromPx, cornerToPx, t.coerceIn(0f, 1f))
    val clip = Path().apply { addRoundRect(RoundRect(dst, CornerRadius(corner))) }
    clipPath(clip) {
        drawImage(
            image = image,
            srcOffset = srcOffset,
            srcSize = IntSize(srcWidth, srcHeight),
            dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
            dstSize = IntSize(
                dst.width.roundToInt().coerceAtLeast(1),
                dst.height.roundToInt().coerceAtLeast(1),
            ),
            // 베이스 프레임만 부드럽게, 크런치 레벨은 픽셀이 살아있게 확대한다.
            filterQuality = if (levelIndex == 0) FilterQuality.Low else FilterQuality.None,
        )
    }
    if (t > 0f) {
        drawRoundRect(
            color = borderColor,
            topLeft = dst.topLeft,
            size = Size(dst.width, dst.height),
            cornerRadius = CornerRadius(corner),
            alpha = t.coerceIn(0f, 1f),
            style = Stroke(borderWidthPx),
        )
    }
}

private const val ChargeDurationMillis = 380
private const val ChargeShakeCycles = 4f
private const val ChargeShakeMaxDegrees = 2f
private const val ChargeScaleGain = 0.04f

/** 크런치 비트의 단일 소스. 개수는 [CrunchLongEdgesPx] 와 같아야 한다. */
private val CrunchStepDelaysMillis = longArrayOf(200, 180, 160, 140, 120, 110)
private const val CrunchPopScale = 1.05f
private const val CrunchPopDamping = 0.6f
private const val CrunchJitterDegrees = 0.8f

private const val ShrinkStiffness = 380f
private const val MaxShrinkOvershoot = 1.3f
private val CeremonyBorderWidth = 3.dp

private const val SuccessCaptionFadeMillis = 300
private const val SuccessCaptionStartScale = 0.8f
