package com.dandi.nyummy.meal.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.presentation.R
import kotlinx.coroutines.launch

/**
 * 픽셀화된 음식이 담긴 게임 아이템 카드입니다.
 *
 * 가만히 두면 둥실둥실 부유하고, 드래그하면 손가락을 탄성으로 따라오며 기울어졌다가
 * 놓으면 워블과 함께 제자리로 복귀합니다. [feedTarget] 이 주어진 상태에서 그 근처에
 * 놓으면 목표(냐미)로 빨려 들어가며 사라지고 [onFed] 를 호출합니다.
 *
 * @param dragBounds 드래그 오프셋 허용 범위(px). 초과분은 소프트 저항으로 뻑뻑해진다.
 * @param feedTarget 정지 중심 기준 먹이기 목표 벡터(px). null 이면 먹이기 비활성.
 * @param feedRadiusPx 목표를 중심으로 먹이기가 성립하는 반경(px).
 */
@Composable
internal fun MealFeedItemCard(
    image: ImageBitmap,
    dragBounds: Rect,
    feedTarget: Offset?,
    feedRadiusPx: Float,
    onFed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val radius = DesignSystemThemeImpl.designSystemRadius
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemDescription = stringResource(R.string.meal_record_feed_item_content_description)
    val feedActionLabel = stringResource(R.string.meal_record_feed_action)

    val latestFeedTarget by rememberUpdatedState(feedTarget)
    val latestFeedRadiusPx by rememberUpdatedState(feedRadiusPx)
    val latestDragBounds by rememberUpdatedState(dragBounds)
    val latestOnFed by rememberUpdatedState(onFed)

    val bobAmplitudePx = with(density) { BobAmplitude.toPx() }

    val entranceScale = remember { Animatable(EntranceStartScale) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    // 부유·기울기 주기를 서로 다르게 두어 기계적인 반복으로 보이지 않게 한다.
    val bobTransition = rememberInfiniteTransition(label = "MealFeedItemBob")
    val bobPhase by bobTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BobTranslateMillis), RepeatMode.Reverse),
        label = "bobY",
    )
    val bobRotation by bobTransition.animateFloat(
        initialValue = -BobRotationDegrees,
        targetValue = BobRotationDegrees,
        animationSpec = infiniteRepeatable(tween(BobRotateMillis), RepeatMode.Reverse),
        label = "bobRotation",
    )

    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // 0=정지(둥근 사각 카드) ↔ 1=드래그 중(작아진 원형 알갱이).
    val dragMorph = remember { Animatable(0f) }
    val consume = remember { Animatable(1f) }
    var dragTarget by remember { mutableStateOf(Offset.Zero) }
    var eating by remember { mutableStateOf(false) }
    var inFeedZone by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cardCornerPx = with(density) { radius.radius16.toPx() }
    val proximityRangePx = with(density) { ProximityShrinkRange.toPx() }
    val tiltReferencePx = with(density) { TiltReference.toPx() }

    fun isNearFeedTarget(position: Offset): Boolean {
        val target = latestFeedTarget ?: return false
        return (position - target).getDistance() <= latestFeedRadiusPx
    }

    /** 먹이기를 시작한다. 목표가 없거나 이미 진행 중이면 false. 드래그·접근성 액션 공용 경로. */
    val startEat: () -> Boolean = eat@{
        if (eating) return@eat false
        val target = latestFeedTarget ?: return@eat false
        eating = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            launch {
                offset.animateTo(target, tween(EatTravelMillis, easing = FastOutLinearInEasing))
            }
            launch { dragMorph.animateTo(1f) }
            consume.animateTo(0f, tween(EatConsumeMillis, delayMillis = EatConsumeDelayMillis))
            latestOnFed()
        }
        true
    }

    val releaseDrag: () -> Unit = release@{
        if (eating) return@release
        // 카드는 스프링으로 손가락을 뒤따르므로 판정은 손가락이 의도한 위치 기준으로 한다.
        if (isNearFeedTarget(dragTarget) && startEat()) return@release
        dragTarget = Offset.Zero
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            dragMorph.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        scope.launch { offset.animateTo(Offset.Zero, WobbleSpring) }
    }

    Box(
        modifier = modifier
            .size(FeedItemCardSize)
            .graphicsLayer {
                val morph = dragMorph.value
                // 냐미에게 가까워질수록 연속으로 더 작아져 '갖다 대야 한다'는 어포던스를 준다.
                val target = latestFeedTarget
                val proximity = if (target != null) {
                    (1f - (offset.value - target).getDistance() / proximityRangePx)
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
                translationX = offset.value.x
                translationY = offset.value.y +
                    bobPhase * bobAmplitudePx * consume.value * (1f - morph)
                rotationZ = bobRotation +
                    (offset.value.x / tiltReferencePx).coerceIn(-1f, 1f) * DragTiltDegrees +
                    (offset.velocity.x / VelocityTiltDivisor).coerceIn(-1f, 1f) * VelocityTiltDegrees
                val morphScale = lerp(1f, DraggingScale, morph) *
                    lerp(1f, NearCatScale, proximity * morph)
                val scale = entranceScale.value * consume.value * morphScale
                scaleX = scale
                scaleY = scale
                alpha = consume.value
                // 드래그 중에는 둥근 사각 카드가 원형 알갱이로 말린다.
                clip = true
                shape = RoundedCornerShape(lerp(cardCornerPx, size.width / 2f, morph))
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        if (eating) return@detectDragGestures
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            dragMorph.animateTo(
                                1f,
                                spring(stiffness = Spring.StiffnessMedium),
                            )
                        }
                    },
                    onDrag = { change, amount ->
                        if (eating) return@detectDragGestures
                        change.consume()
                        val bounds = latestDragBounds
                        // 손가락 이동보다 살짝 덜 움직여 묵직한 감도를 만든다.
                        dragTarget = Offset(
                            softClampAxis(
                                dragTarget.x + amount.x * DragInputRatio,
                                bounds.left,
                                bounds.right,
                            ),
                            softClampAxis(
                                dragTarget.y + amount.y * DragInputRatio,
                                bounds.top,
                                bounds.bottom,
                            ),
                        )
                        val nowInZone = isNearFeedTarget(dragTarget)
                        if (nowInZone != inFeedZone) {
                            inFeedZone = nowInZone
                            // 먹이기 존 진입/이탈을 손끝으로 알려준다.
                            if (nowInZone) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        // 리타겟 스프링이 손가락을 러버밴드처럼 따라오게 만든다.
                        scope.launch { offset.animateTo(dragTarget, FollowSpring) }
                    },
                    onDragEnd = releaseDrag,
                    onDragCancel = releaseDrag,
                )
            }
            .semantics {
                // 소비 중/후에는 보이지 않는 잔여 노드가 포커스되지 않도록 비운다.
                if (!eating) {
                    contentDescription = itemDescription
                    // 드래그 제스처는 접근성 트리에 노출되지 않으므로 먹이기를 액션으로 제공한다.
                    if (feedTarget != null) {
                        onClick(label = feedActionLabel) { startEat() }
                    }
                }
            }
            .background(colors.bgSurfaceIvory),
    ) {
        Canvas(Modifier.size(FeedItemCardSize)) {
            val srcSide = minOf(image.width, image.height)
            drawImage(
                image = image,
                srcOffset = IntOffset(
                    (image.width - srcSide) / 2,
                    (image.height - srcSide) / 2,
                ),
                srcSize = IntSize(srcSide, srcSide),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

/** 한계 이내는 그대로, 초과분에는 저항을 곱해 고무줄처럼 뻑뻑해지게 한다. */
private fun softClampAxis(value: Float, min: Float, max: Float): Float = when {
    value < min -> min + (value - min) * DragOverflowResistance
    value > max -> max + (value - max) * DragOverflowResistance
    else -> value
}

internal val FeedItemCardSize = 180.dp
private val BobAmplitude = 5.dp

/** 이 거리 안으로 들어오면 근접 축소가 시작된다. */
private val ProximityShrinkRange = 240.dp

private const val BobTranslateMillis = 2200
private const val BobRotateMillis = 3100
private const val BobRotationDegrees = 1.2f
private const val EntranceStartScale = 0.9f

/** 드래그 중 기본 축소 배율(원형 알갱이). */
private const val DraggingScale = 0.55f

/** 냐미 최근접 시 추가 축소 배율(누적 ≈0.30). */
private const val NearCatScale = 0.55f

/** 손가락 이동 대비 카드 이동 비율. 1보다 작을수록 묵직하다. */
private const val DragInputRatio = 0.9f
private const val DragTiltDegrees = 6f
private val TiltReference = 160.dp
private const val VelocityTiltDegrees = 3f
private const val VelocityTiltDivisor = 9000f
private const val DragOverflowResistance = 0.35f
private const val EatTravelMillis = 220
private const val EatConsumeMillis = 200
private const val EatConsumeDelayMillis = 100

private val FollowSpring = spring<Offset>(dampingRatio = 0.85f, stiffness = 320f)
private val WobbleSpring = spring<Offset>(dampingRatio = 0.45f, stiffness = 300f)

@Preview(showBackground = true)
@Composable
private fun MealFeedItemCardPreview() {
    DesignSystemTheme {
        MealFeedItemCard(
            image = rememberPreviewPixelImage(),
            dragBounds = Rect(-400f, -900f, 400f, 300f),
            feedTarget = null,
            feedRadiusPx = 0f,
            onFed = {},
        )
    }
}

@Composable
private fun rememberPreviewPixelImage(): ImageBitmap {
    val colors = DesignSystemThemeImpl.designSystemColor
    val palette = intArrayOf(
        colors.bgActionRewardDefault.toArgb(),
        colors.bgSurfaceIvory.toArgb(),
        colors.bgActionPrimaryDefault.toArgb(),
    )
    return remember {
        val side = 12
        val bitmap = android.graphics.Bitmap.createBitmap(
            side,
            side,
            android.graphics.Bitmap.Config.ARGB_8888,
        )
        repeat(side) { y ->
            repeat(side) { x ->
                bitmap.setPixel(x, y, palette[(x / 3 + y / 2) % palette.size])
            }
        }
        bitmap.asImageBitmap()
    }
}
