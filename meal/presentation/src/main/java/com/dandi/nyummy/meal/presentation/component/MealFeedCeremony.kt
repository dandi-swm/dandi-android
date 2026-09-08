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
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.component.NyummySpriteView
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
 * 촬영본이 게임 아이템으로 변환되어 냐미에게 먹여지는 먹이기 세리머니입니다.
 *
 * 차지업(진동) → 계단식 해상도 크런치 → 스프링 축소 → 아이템 카드 부유의 4단계로
 * 진행되며, 축소부터는 상단에 냐미가 등장합니다. 냐미는 응답 대기 중 엎드려 졸다가
 * [feedReady] 가 켜지면 일어나 앉고, 사용자가 아이템을 냐미 근처로 드래그해 놓으면
 * 먹이기가 성립해 [onFedToCat] 으로 화면이 마무리됩니다.
 * 체인 디코드에 실패하면 기존 촬영본 표시로 조용히 대체됩니다.
 * 애니메이션 값은 모두 draw/graphicsLayer 단계에서만 읽어 리컴포지션을 만들지 않습니다.
 */
@Composable
internal fun MealFeedCeremony(
    photoPath: String,
    chainResult: MealPixelChainResult,
    feedReady: Boolean,
    onIdleChanged: (Boolean) -> Unit,
    onFedToCat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val radius = DesignSystemThemeImpl.designSystemRadius
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val photoDescription =
        stringResource(R.string.meal_record_captured_photo_content_description)

    val latestChainResult by rememberUpdatedState(chainResult)
    val latestOnIdleChanged by rememberUpdatedState(onIdleChanged)
    val latestOnFedToCat by rememberUpdatedState(onFedToCat)

    var stage by remember { mutableStateOf(CeremonyStage.Charging) }
    val charge = remember { Animatable(0f) }
    val crunchLevel = remember { mutableIntStateOf(0) }
    val pop = remember { Animatable(1f) }
    val shrink = remember { Animatable(0f) }

    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var catSeated by remember { mutableStateOf(false) }
    var fedToCat by remember { mutableStateOf(false) }
    val catPop = remember { Animatable(1f) }

    // 먹이기 성립: 냐미가 냠냠 튀어오르는 반응을 보여준 뒤 화면을 마무리한다.
    LaunchedEffect(fedToCat) {
        if (!fedToCat) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        catPop.animateTo(
            CatPopScale,
            spring(dampingRatio = CatPopDamping, stiffness = Spring.StiffnessMedium),
        )
        catPop.animateTo(1f, spring())
        delay(FedNavigateDelayMillis)
        latestOnFedToCat()
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { stageSize = it },
    ) {
        when (stage) {
            CeremonyStage.Fallback -> AsyncImage(
                model = File(photoPath),
                contentDescription = photoDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            CeremonyStage.Idle -> {
                val chain = (chainResult as? MealPixelChainResult.Ready)?.chain
                if (chain != null && stageSize != IntSize.Zero) {
                    val cardHalfPx = with(density) { FeedItemCardSize.toPx() } / 2f
                    val itemCenterY = with(density) {
                        itemRestCenterY(stageSize.height.toFloat())
                    }
                    val catCenterY = with(density) {
                        CatTopPadding.toPx() + CatDisplayWidth.toPx() / 2f
                    }
                    val feedEnabled = feedReady && catSeated && !fedToCat
                    MealFeedItemCard(
                        image = chain.levels.last(),
                        // 포토 박스 전역을 소프트 한계로 삼아 냐미까지 자유롭게 오간다.
                        dragBounds = Rect(
                            left = -(stageSize.width / 2f - cardHalfPx / 2f),
                            top = catCenterY - itemCenterY - cardHalfPx / 2f,
                            right = stageSize.width / 2f - cardHalfPx / 2f,
                            bottom = stageSize.height - itemCenterY - cardHalfPx / 2f,
                        ),
                        feedTarget = if (feedEnabled) {
                            Offset(0f, catCenterY - itemCenterY)
                        } else {
                            null
                        },
                        feedRadiusPx = with(density) { FeedAcceptRadius.toPx() },
                        onFed = { fedToCat = true },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset { IntOffset(0, (itemCenterY - cardHalfPx).roundToInt()) },
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
                        )
                    }
                }
            }
        }

        // 냐미: 사진이 카드로 축소되기 시작하면 상단에 등장한다.
        // 응답 대기 중에는 엎드려 졸고, feedReady 가 켜지면 일어나 앉아 먹을 준비를 한다.
        AnimatedVisibility(
            visible = stage == CeremonyStage.Shrinking || stage == CeremonyStage.Idle,
            enter = fadeIn(tween(CatEnterMillis)) +
                scaleIn(
                    initialScale = CatEnterStartScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = CatTopPadding),
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = catPop.value
                    scaleY = catPop.value
                },
            ) {
                if (feedReady) {
                    NyummySpriteView(
                        sheet = CeremonyCatWakeSheet,
                        displayWidth = CatDisplayWidth,
                        iterations = 1,
                        onAnimationEnd = { catSeated = true },
                        contentDescription = stringResource(
                            R.string.meal_record_cat_content_description,
                        ),
                    )
                } else {
                    NyummySpriteView(
                        sheet = CeremonyCatSleepSheet,
                        displayWidth = CatDisplayWidth,
                        contentDescription = stringResource(
                            R.string.meal_record_cat_content_description,
                        ),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = feedReady && catSeated && !fedToCat && stage == CeremonyStage.Idle,
            enter = fadeIn(tween(SuccessCaptionFadeMillis)) +
                scaleIn(
                    initialScale = SuccessCaptionStartScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    // 냐미 하단과 카드 상단 사이의 중간에 앵커해 어떤 박스 높이에서도
                    // 냐미·먹이기 존과 겹치지 않게 한다.
                    val catBottom = CatTopPadding.toPx() + CatDisplayWidth.toPx()
                    val itemTop = itemRestCenterY(stageSize.height.toFloat()) -
                        FeedItemCardSize.toPx() / 2f
                    IntOffset(
                        0,
                        ((catBottom + itemTop) / 2f - CaptionHalfHeight.toPx()).roundToInt(),
                    )
                },
        ) {
            DandiText(
                text = stringResource(R.string.meal_record_feed_to_cat_hint),
                // 밝은 bgMealPhoto 배경 위이므로 기본 전경색으로 대비를 확보하고,
                // 먹이기 가능 시점을 접근성으로도 고지한다.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
        }
    }
}

private enum class CeremonyStage { Charging, Crunching, Shrinking, Idle, Fallback }

/**
 * 풀프레임→아이템 카드 사이를 보간하며 현재 크런치 레벨을 커버 크롭으로 그린다.
 * 카드는 냐미에게 먹일 수 있도록 화면 하단부([ItemRestCenterYFraction])에 착지한다.
 */
private fun DrawScope.drawCeremonyFrame(
    chain: MealPixelChain,
    level: Int,
    shrinkValue: Float,
    cornerFromPx: Float,
    cornerToPx: Float,
) {
    val levelIndex = level.coerceIn(0, chain.levels.lastIndex)
    val image = chain.levels[levelIndex]
    val t = shrinkValue.coerceAtMost(MaxShrinkOvershoot)

    val cardSide = FeedItemCardSize.toPx()
    val fullRect = Rect(Offset.Zero, size)
    val half = cardSide / 2f
    val cardCenterY = itemRestCenterY(size.height)
    val cardRect = Rect(
        center.x - half,
        cardCenterY - half,
        center.x + half,
        cardCenterY + half,
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

/** 아이템 카드가 착지·부유하는 세로 중심(포토 박스 높이 대비 비율). 냐미 아래쪽이다. */
private const val ItemRestCenterYFraction = 0.66f

/**
 * 카드 휴식 세로 중심(px). 기본은 비율 위치지만, 가로모드·분할화면처럼 박스가 짧을 때는
 * 휴식 지점이 먹이기 반경 안에 들어가 즉시 발동하지 않도록 냐미 반경 밖으로 밀어낸다.
 */
private fun Density.itemRestCenterY(boxHeightPx: Float): Float {
    val catCenterY = CatTopPadding.toPx() + CatDisplayWidth.toPx() / 2f
    val minCenterY = catCenterY + (FeedAcceptRadius + MinFeedTravel).toPx()
    return maxOf(boxHeightPx * ItemRestCenterYFraction, minCenterY)
}

/** 휴식 지점에서 먹이기 반경까지 보장되는 최소 이동 거리. */
private val MinFeedTravel = 64.dp

/** 캡션 한 줄 높이의 절반 근사값(앵커 중앙 정렬용). */
private val CaptionHalfHeight = 14.dp

/** 대기 중 상단에서 엎드려 조는 냐미 (홈·프리뷰와 동일 시트). */
private val CeremonyCatSleepSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_sleep_loop_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 8,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

/** 응답 도착 시 일어나 앉는 전환. 마지막 프레임(앉은 자세)에 멈춰 먹이를 기다린다. */
private val CeremonyCatWakeSheet = NyummySpriteSheet(
    imageRes = R.drawable.nyami_wake_grid_136,
    frameWidth = 136,
    frameHeight = 136,
    totalFrames = 17,
    framesPerRow = 4,
    frameDurationMillis = 100,
)

private val CatDisplayWidth = 112.dp
private val CatTopPadding = 16.dp

/** 냐미 중심을 기준으로 먹이기가 성립하는 반경. 근접 축소와 함께 '갖다 대는' 감각을 만든다. */
private val FeedAcceptRadius = 80.dp
private const val CatEnterMillis = 300
private const val CatEnterStartScale = 0.8f
private const val CatPopScale = 1.18f
private const val CatPopDamping = 0.4f
private const val FedNavigateDelayMillis = 350L

private const val SuccessCaptionFadeMillis = 300
private const val SuccessCaptionStartScale = 0.8f
