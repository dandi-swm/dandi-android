package com.dandi.nyummy.meal.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.presentation.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * 픽셀화된 음식이 담긴 게임 아이템 카드입니다.
 *
 * 가만히 두면 둥실둥실 부유하고, 드래그하면 손가락을 탄성으로 따라오며 기울어졌다가
 * 놓으면 워블과 함께 제자리로 복귀합니다. 응답 대기를 놀이 시간으로 바꾸는 역할입니다.
 */
@Composable
internal fun MealFeedItemCard(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val radius = DesignSystemThemeImpl.designSystemRadius
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemDescription = stringResource(R.string.meal_record_feed_item_content_description)

    val bobAmplitudePx = with(density) { BobAmplitude.toPx() }
    val maxDragXPx = with(density) { MaxDragX.toPx() }
    val maxDragYPx = with(density) { MaxDragY.toPx() }

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
    val pressScale = remember { Animatable(1f) }
    var dragTarget by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val releaseDrag: () -> Unit = {
        dragTarget = Offset.Zero
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { pressScale.animateTo(1f, spring(dampingRatio = ReleaseScaleDamping)) }
        scope.launch { offset.animateTo(Offset.Zero, WobbleSpring) }
    }

    Box(
        modifier = modifier
            .size(FeedItemFrameSize)
            .graphicsLayer {
                translationX = offset.value.x
                translationY = offset.value.y + bobPhase * bobAmplitudePx
                rotationZ = bobRotation +
                    (offset.value.x / maxDragXPx) * DragTiltDegrees +
                    (offset.velocity.x / VelocityTiltDivisor).coerceIn(-1f, 1f) * VelocityTiltDegrees
                scaleX = entranceScale.value * pressScale.value
                scaleY = entranceScale.value * pressScale.value
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            pressScale.animateTo(
                                PressedScale,
                                spring(stiffness = Spring.StiffnessMedium),
                            )
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragTarget = Offset(
                            softClamp(dragTarget.x + amount.x, maxDragXPx),
                            softClamp(dragTarget.y + amount.y, maxDragYPx),
                        )
                        // 리타겟 스프링이 손가락을 러버밴드처럼 따라오게 만든다.
                        scope.launch { offset.animateTo(dragTarget, FollowSpring) }
                    },
                    onDragEnd = releaseDrag,
                    onDragCancel = releaseDrag,
                )
            }
            .semantics {
                contentDescription = itemDescription
            },
    ) {
        SparkleIcon(
            twinkleDelayMillis = 0,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Box(
            modifier = Modifier
                .size(FeedItemCardSize)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(radius.radius16))
                .background(colors.bgSurfaceIvory)
                .border(
                    width = FeedItemBorderWidth,
                    color = colors.bgActionRewardDefault,
                    shape = RoundedCornerShape(radius.radius16),
                ),
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
        SparkleIcon(
            twinkleDelayMillis = SparklePhaseShiftMillis,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun SparkleIcon(
    twinkleDelayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val transition = rememberInfiniteTransition(label = "MealFeedSparkle")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SparkleTwinkleMillis),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(twinkleDelayMillis),
        ),
        label = "twinkle",
    )
    Icon(
        painter = painterResource(R.drawable.ic_meal_sparkle),
        contentDescription = null,
        modifier = modifier
            .size(SparkleSize)
            .graphicsLayer {
                alpha = SparkleMinAlpha + (1f - SparkleMinAlpha) * twinkle
                val scale = SparkleMinScale + (SparkleMaxScale - SparkleMinScale) * twinkle
                scaleX = scale
                scaleY = scale
            },
        tint = colors.bgActionRewardDefault,
    )
}

/** 한계 이내는 그대로, 초과분에는 저항을 곱해 고무줄처럼 뻑뻑해지게 한다. */
private fun softClamp(value: Float, limit: Float): Float =
    if (abs(value) <= limit) {
        value
    } else {
        sign(value) * (limit + (abs(value) - limit) * DragOverflowResistance)
    }

internal val FeedItemCardSize = 180.dp
private val FeedItemFrameSize = 224.dp
private val FeedItemBorderWidth = 3.dp
private val SparkleSize = 24.dp
private val BobAmplitude = 5.dp
private val MaxDragX = 110.dp
private val MaxDragY = 80.dp

private const val BobTranslateMillis = 2200
private const val BobRotateMillis = 3100
private const val BobRotationDegrees = 1.2f
private const val SparkleTwinkleMillis = 900
private const val SparklePhaseShiftMillis = 450
private const val SparkleMinAlpha = 0.3f
private const val SparkleMinScale = 0.85f
private const val SparkleMaxScale = 1.15f
private const val EntranceStartScale = 0.9f
private const val PressedScale = 1.08f
private const val ReleaseScaleDamping = 0.45f
private const val DragTiltDegrees = 10f
private const val VelocityTiltDegrees = 6f
private const val VelocityTiltDivisor = 6000f
private const val DragOverflowResistance = 0.35f

private val FollowSpring = spring<Offset>(dampingRatio = 0.75f, stiffness = 350f)
private val WobbleSpring = spring<Offset>(dampingRatio = 0.45f, stiffness = 300f)

@Preview(showBackground = true)
@Composable
private fun MealFeedItemCardPreview() {
    DesignSystemTheme {
        MealFeedItemCard(image = rememberPreviewPixelImage())
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
