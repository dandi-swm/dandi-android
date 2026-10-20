package com.dandi.nyummy.meal.presentation.component.ceremony

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.R
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** 개발자 옵션이나 접근성에서 애니메이션을 끈 기기인지. 끈 기기에는 섬광, 흔들림, 컷인 없이 정지컷만 보여 준다. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * 띠욧과 먹었어요 장면의 배경. 에버그린 바탕 가운데가 흰빛과 민트빛으로 환하게 빛나고, 그 빛 주변 여러 곳에서
 * 길쭉한 사다리꼴 빛줄기가 앞으로 쭉 뻗어 나갑니다.
 *
 * - 빛줄기는 저마다 다른 자리에서 좁게 출발해 화면 끝까지 빠르게 뻗고, 긴 빛줄기로 잠시 머문 뒤 꼬리부터
 *   바깥으로 빠지며 사라진다. 사라진 자리에는 다른 각도와 출발점으로 새 빛줄기가 뻗어 나와, 빛이 쉬지 않고
 *   쏟아져 나오는 것처럼 보인다.
 * - 바깥으로 갈수록 넓어지고 옅어지며, 굵은 빛줄기 사이사이에 가는 빛줄기를 섞는다.
 * - 빛은 바탕 색에 더해 칠해(Plus) 금빛이 녹색과 섞여 탁해지지 않고 밝게 빛난다.
 * - 가장자리는 짙은 그린으로 살짝 눌러 가운데 빛이 살아 보이게 한다.
 *
 * @param focus 빛이 모이는 자리(그리는 영역 크기 대비 비율)
 * @param intensity 빛줄기 세기(0~1). 먹었어요 장면은 띠욧보다 낮춰 차분하게 쓴다.
 */
@Composable
internal fun GlowBurstBackground(
    modifier: Modifier = Modifier,
    focus: Offset = Offset(0.5f, 0.5f),
    intensity: Float = 1f,
) {
    val base = NyummyTheme.colors.bg.actionPrimary
    val deep = NyummyTheme.colors.content.brand
    val mint = NyummyTheme.colors.bg.selected
    val gold = NyummyTheme.colors.data.coin
    val white = NyummyTheme.colors.bg.surface
    val sparkle = painterResource(R.drawable.ic_meal_sparkle)
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (isActive) {
            val now = withFrameMillis { it }
            seconds += (now - last) / 1000f
            last = now
        }
    }

    Spacer(
        modifier.drawWithCache {
            val center = Offset(size.width * focus.x, size.height * focus.y)
            // 빛 중심에서 가장 먼 구석까지. 빛줄기 길이와 굵기, 가장자리 어둡기의 기준이다.
            val reach = maxOf(
                hypot(center.x, center.y),
                hypot(size.width - center.x, center.y),
                hypot(center.x, size.height - center.y),
                hypot(size.width - center.x, size.height - center.y),
            )
            // 빛줄기는 빛 중심에 가까울수록 밝고 멀어질수록 옅다.
            fun rayBrush(color: Color) = Brush.radialGradient(
                0f to color,
                0.35f to color.copy(alpha = 0.75f),
                0.75f to color.copy(alpha = 0.25f),
                1f to color.copy(alpha = 0.08f),
                center = center,
                radius = reach,
            )
            val whiteRay = rayBrush(white)
            val mintRay = rayBrush(mint)
            val goldRay = rayBrush(gold)
            val ray = Path()
            val glowRadius = size.minDimension * GlowRadius
            val glow = Brush.radialGradient(
                0f to white,
                0.25f to white.copy(alpha = 0.85f),
                0.55f to mint.copy(alpha = 0.5f),
                1f to mint.copy(alpha = 0f),
                center = center,
                radius = glowRadius,
            )
            val vignette = Brush.radialGradient(
                GlowVignetteStart to deep.copy(alpha = 0f),
                1f to deep.copy(alpha = GlowVignetteAlpha),
                center = center,
                radius = reach,
            )
            val twinkleSize = GlowTwinkleSize.toPx()

            onDrawBehind {
                drawRect(base)
                GlowRaySlots.forEach { slot ->
                    // 주기마다 새 각도와 출발점으로 뻗는다. 머리가 빠르게 끝까지 가고, 주기 끝에 꼬리가 바깥으로 빠진다.
                    val cycleTime = seconds / slot.periodSeconds + slot.phase
                    val cycle = floor(cycleTime).toInt()
                    val t = cycleTime - cycle
                    val angle = slot.sector + hash01(slot.index, cycle, 3) * slot.sectorWidth * 0.8f
                    val start = slot.startMin + hash01(slot.index, cycle, 4) * slot.startSpread
                    val innerHalf = slot.innerHalfWidth
                    val outerHalf = slot.outerHalfMin + hash01(slot.index, cycle, 5) * slot.outerHalfSpread
                    val strength = slot.strengthMin + hash01(slot.index, cycle, 6) * slot.strengthSpread
                    val headProgress = (t / GlowHeadPortion).coerceAtMost(1f)
                    val head = start + (GlowRayEnd - start) * (1f - (1f - headProgress).let { it * it * it })
                    val tailProgress = ((t - GlowTailLeaveAt) / (1f - GlowTailLeaveAt)).coerceIn(0f, 1f)
                    val tail = start + (GlowTailEnd - start) * tailProgress * tailProgress
                    val life = (t / GlowFadeIn).coerceAtMost(1f) * ((1f - t) / GlowFadeOut).coerceAtMost(1f)
                    val alpha = (strength * intensity * life).coerceIn(0f, 1f)
                    if (head <= tail || alpha <= 0f) return@forEach

                    fun halfAt(r: Float) = innerHalf + (outerHalf - innerHalf) * ((r - start) / (GlowRayEnd - start))
                    val radians = angle * DegreesToRadians
                    val ax = cos(radians)
                    val ay = sin(radians)
                    val tailHalf = halfAt(tail) * reach
                    val headHalf = halfAt(head) * reach
                    val tailX = center.x + ax * tail * reach
                    val tailY = center.y + ay * tail * reach
                    val headX = center.x + ax * head * reach
                    val headY = center.y + ay * head * reach
                    ray.reset()
                    ray.moveTo(tailX - ay * tailHalf, tailY + ax * tailHalf)
                    ray.lineTo(headX - ay * headHalf, headY + ax * headHalf)
                    ray.lineTo(headX + ay * headHalf, headY - ax * headHalf)
                    ray.lineTo(tailX + ay * tailHalf, tailY - ax * tailHalf)
                    ray.close()
                    val brush = when {
                        slot.warm -> goldRay
                        slot.index % 2 == 0 -> whiteRay
                        else -> mintRay
                    }
                    drawPath(ray, brush, alpha = alpha, blendMode = BlendMode.Plus)
                }
                drawRect(vignette)
                drawCircle(brush = glow, radius = glowRadius, center = center)
                // 반짝이: 자리마다 박자를 어긋나게 해 숨 쉬듯 깜빡이고, 뒤에 작은 빛 번짐을 둔다.
                val twinklePhase = (seconds / GlowTwinkleSeconds) % 1f
                GlowTwinkles.forEachIndexed { index, spot ->
                    val t = (twinklePhase + index * 0.17f) % 1f
                    val pulse = (sin(t * 2f * PI.toFloat()) + 1f) / 2f
                    val tint = if (index % 3 == 0) gold else white
                    val spotCenter = Offset(size.width * spot.x, size.height * spot.y)
                    val side = twinkleSize * (0.6f + 0.4f * pulse) * (if (index % 2 == 0) 1f else 0.7f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(tint.copy(alpha = 0.45f * pulse), tint.copy(alpha = 0f)),
                            center = spotCenter,
                            radius = side,
                        ),
                        radius = side,
                        center = spotCenter,
                    )
                    drawIcon(
                        painter = sparkle,
                        tint = tint.copy(alpha = 0.5f + 0.5f * pulse),
                        center = spotCenter,
                        side = side,
                    )
                }
            }
        },
    )
}

/**
 * 빛줄기가 뻗어 나오는 자리 하나. [sector] 부터 [sectorWidth] 안의 각도(도)에서, [periodSeconds] 마다 새로 뻗는다.
 * 출발점, 굵기, 진하기는 빛 중심에서 가장 먼 구석까지 거리 대비 비율이고, 주기마다 범위 안에서 새로 정한다.
 * [warm] 이면 금빛이다.
 */
private data class GlowRaySlot(
    val index: Int,
    val sector: Float,
    val sectorWidth: Float,
    val periodSeconds: Float,
    val phase: Float,
    val startMin: Float,
    val startSpread: Float,
    val innerHalfWidth: Float,
    val outerHalfMin: Float,
    val outerHalfSpread: Float,
    val strengthMin: Float,
    val strengthSpread: Float,
    val warm: Boolean,
)

/** 굵은 빛줄기 자리를 고르게 두르고, 그 사이사이에 가는 빛줄기 자리를 둔다. 뻗는 박자는 자리마다 어긋난다. */
private val GlowRaySlots: List<GlowRaySlot> = List(GlowWideRayCount + GlowThinRayCount) { index ->
    val thin = index >= GlowWideRayCount
    val count = if (thin) GlowThinRayCount else GlowWideRayCount
    val order = if (thin) index - GlowWideRayCount else index
    GlowRaySlot(
        index = index,
        sector = (order + if (thin) 0.5f else 0f) * 360f / count,
        sectorWidth = 360f / count,
        periodSeconds = GlowRayPeriodSeconds + hash01(index, 0, 1) * GlowRayPeriodSpread,
        phase = hash01(index, 0, 2),
        startMin = if (thin) 0.1f else 0.04f,
        startSpread = if (thin) 0.3f else 0.28f,
        innerHalfWidth = if (thin) 0.0012f else 0.004f,
        outerHalfMin = if (thin) 0.004f else 0.025f,
        outerHalfSpread = if (thin) 0.003f else 0.03f,
        strengthMin = if (thin) 0.7f else 0.55f,
        strengthSpread = if (thin) 0.3f else 0.35f,
        warm = !thin && order % 3 == 1,
    )
}

/** [a], [b], [salt] 로 정해지는 0~1 사이 값. 같은 입력이면 늘 같은 값이라 빛줄기 모양이 프레임마다 흔들리지 않는다. */
private fun hash01(a: Int, b: Int, salt: Int): Float {
    var h = (a * 73_856_093) xor (b * 19_349_663) xor (salt * 83_492_791)
    h = (h xor (h ushr 13)) * 1_274_126_177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16_777_216f
}

/** 한 번 터져 나가는 조각 하나. 아이콘 조각과 색종이 조각이 같은 물리를 쓴다. */
private data class BurstPiece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val scale: Float,
    val kind: Int,
)

/**
 * [origin] 에서 아이콘 조각(하트, 별, 반짝이)이 사방으로 튀었다가 떨어지며 사라진다.
 * 아이콘은 라이브러리 아이콘을 [tints] 색으로 칠해 쓴다. [origin] 은 그리는 영역 크기 대비 비율이다.
 */
@Composable
internal fun IconBurst(
    icons: List<Painter>,
    tints: List<Color>,
    modifier: Modifier = Modifier,
    origin: Offset = Offset(0.5f, 0.5f),
    pieceCount: Int = BurstPieceCount,
    iconSize: Dp = BurstIconSize,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(BurstMillis, easing = LinearEasing))
    }
    val pieces = remember(pieceCount, icons.size) {
        List(pieceCount) {
            BurstPiece(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.55f + Random.nextFloat() * 0.45f,
                spin = (Random.nextFloat() - 0.5f) * 540f,
                scale = 0.7f + Random.nextFloat() * 0.6f,
                kind = it % icons.size.coerceAtLeast(1),
            )
        }
    }
    Canvas(modifier) {
        if (icons.isEmpty()) return@Canvas
        val t = progress.value
        val start = Offset(size.width * origin.x, size.height * origin.y)
        val reach = size.minDimension * BurstReach
        val sidePx = iconSize.toPx()
        pieces.forEach { piece ->
            // 처음엔 빠르게 퍼지고 뒤로 갈수록 느려지며, 아래로 조금 떨어진다.
            val eased = 1f - (1f - t) * (1f - t)
            val x = start.x + cos(piece.angle) * reach * piece.speed * eased
            val y = start.y + sin(piece.angle) * reach * piece.speed * eased + BurstGravity * reach * t * t
            val alpha = (1f - t).coerceIn(0f, 1f)
            val side = sidePx * piece.scale
            drawIcon(
                painter = icons[piece.kind],
                tint = tints[piece.kind % tints.size].copy(alpha = alpha),
                center = Offset(x, y),
                side = side,
                degrees = piece.spin * t,
            )
        }
    }
}

/** [painter] 아이콘을 [center] 에 [side] 크기 정사각형으로, [degrees] 만큼 돌려 [tint] 색으로 그린다. */
internal fun DrawScope.drawIcon(
    painter: Painter,
    tint: Color,
    center: Offset,
    side: Float,
    degrees: Float = 0f,
) {
    translate(center.x - side / 2f, center.y - side / 2f) {
        rotate(degrees, Offset(side / 2f, side / 2f)) {
            with(painter) {
                draw(Size(side, side), colorFilter = ColorFilter.tint(tint))
            }
        }
    }
}

private const val GlowWideRayCount = 26
private const val GlowThinRayCount = 18
private const val GlowRayPeriodSeconds = 1.8f
private const val GlowRayPeriodSpread = 1.2f

/** 빛줄기 머리가 끝까지 뻗는 데 쓰는 주기 비율. */
private const val GlowHeadPortion = 0.3f
private const val GlowRayEnd = 1.05f
private const val GlowTailEnd = 0.95f

/** 꼬리가 바깥으로 빠지기 시작하는 주기 비율. 그 전까지는 출발점에 머물러 긴 빛줄기로 보인다. */
private const val GlowTailLeaveAt = 0.6f
private const val GlowFadeIn = 0.05f
private const val GlowFadeOut = 0.2f
private const val GlowRadius = 0.7f
private const val GlowVignetteStart = 0.5f
private const val GlowVignetteAlpha = 0.55f
private const val GlowTwinkleSeconds = 1.8f
private val GlowTwinkleSize = 22.dp
private const val DegreesToRadians = (PI / 180.0).toFloat()

/** 반짝이 자리(영역 크기 대비 비율). */
private val GlowTwinkles = listOf(
    Offset(0.12f, 0.16f), Offset(0.86f, 0.12f), Offset(0.2f, 0.42f), Offset(0.9f, 0.38f),
    Offset(0.08f, 0.66f), Offset(0.82f, 0.62f), Offset(0.32f, 0.08f), Offset(0.66f, 0.3f),
    Offset(0.14f, 0.86f), Offset(0.88f, 0.84f),
)

private const val BurstPieceCount = 16
private const val BurstMillis = 1100
private const val BurstReach = 0.62f
private const val BurstGravity = 0.35f
private val BurstIconSize = 22.dp

