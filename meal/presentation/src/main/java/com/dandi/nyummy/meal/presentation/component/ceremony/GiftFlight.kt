package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.meal.presentation.R
import kotlin.math.roundToInt

/**
 * 상자가 무대 가운데([from])에서 오른쪽 위 우편 아이콘([to])으로 포물선을 그리며 날아갑니다.
 * 지나간 길에는 금빛 점선과 반짝이가 남고, 상자는 점점 작아지며 살짝 돕니다.
 * 좌표는 화면 기준 px 이고, 닿는 순간 [onArrived] 를 한 번 부릅니다.
 */
@Composable
internal fun GiftFlight(
    grade: RewardBoxGrade,
    from: Offset,
    to: Offset,
    onArrived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnArrived by rememberUpdatedState(onArrived)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(GiftFlightMillis.toInt(), easing = FastOutSlowInEasing))
        latestOnArrived()
    }

    val density = LocalDensity.current
    val liftPx = with(density) { ArcLift.toPx() }
    val control = Offset((from.x + to.x) / 2f - with(density) { ArcBend.toPx() }, minOf(from.y, to.y) - liftPx)
    fun pointAt(t: Float): Offset {
        val u = 1f - t
        return Offset(
            u * u * from.x + 2f * u * t * control.x + t * t * to.x,
            u * u * from.y + 2f * u * t * control.y + t * t * to.y,
        )
    }

    val trail = NyummyTheme.colors.data.coin
    val sparkle = painterResource(R.drawable.ic_meal_sparkle)
    val boxPainter = rememberRewardBoxPainter(grade)
    val startSidePx = with(density) { RewardBoxSize.toPx() }
    val endSidePx = with(density) { EndSize.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // 지나간 길: 점선 + 몇 군데 반짝이.
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            if (t >= 1f) return@Canvas
            val dots = (TrailDots * t).toInt()
            for (i in 0 until dots) {
                val p = pointAt(i / TrailDots.toFloat())
                drawCircle(trail.copy(alpha = TrailAlpha), radius = TrailDotRadius.toPx(), center = p)
            }
            SparkleStops.forEach { stop ->
                if (t > stop) {
                    val age = ((t - stop) / SparkleLife).coerceIn(0f, 1f)
                    drawIcon(
                        painter = sparkle,
                        tint = trail.copy(alpha = 1f - age),
                        center = pointAt(stop) + Offset(0f, -SparkleOffset.toPx()),
                        side = SparkleSize.toPx() * (1f - age * 0.4f),
                    )
                }
            }
        }
        val t = progress.value
        if (t < 1f) {
            val side = startSidePx + (endSidePx - startSidePx) * t
            val center = pointAt(t)
            Image(
                painter = boxPainter,
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset((center.x - side / 2f).roundToInt(), (center.y - side / 2f).roundToInt()) }
                    .size(with(density) { side.toDp() })
                    .graphicsLayer { rotationZ = SpinDegrees * t },
            )
        }
    }
}

/** 상자가 우편 아이콘에 닿기까지 걸리는 시간. */
internal const val GiftFlightMillis = 820L

private val ArcLift = 120.dp
private val ArcBend = 40.dp
private val EndSize = 32.dp
private const val SpinDegrees = 18f
private const val TrailDots = 26
private const val TrailAlpha = 0.85f
private val TrailDotRadius = 3.dp
private val SparkleStops = listOf(0.2f, 0.45f, 0.7f)
private const val SparkleLife = 0.35f
private val SparkleSize = 18.dp
private val SparkleOffset = 14.dp
