package com.dandi.nyummy.common.presentation.cutscene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 기준 캔버스([CutsceneCanvas])를 화면에 맞춘 좌표계. 캔버스 좌표(dp)를 이 화면의 px 로 바꾼다.
 *
 * @param screen 이 화면 전체 크기(px). 마지막 칸은 여기까지 펼친다.
 * @param origin 캔버스 왼쪽 위가 놓이는 자리(px).
 * @param unit 캔버스 1단위가 몇 px 인지.
 * @param canvasScale 캔버스 1단위가 몇 dp 인지. 글자 크기처럼 dp 로 키울 때 쓴다.
 */
@Stable
class CutsceneFrame internal constructor(
    val screen: Size,
    val origin: Offset,
    val unit: Float,
    val canvasScale: Float,
) {
    fun point(x: Float, y: Float): Offset = Offset(origin.x + x * unit, origin.y + y * unit)

    /** 칸 모양을 px 꼭짓점으로 바꾼다. 홈 너비의 반만큼 안쪽으로 줄여, 이웃 칸 사이에 홈이 생긴다. */
    fun corners(shape: CutsceneShape): List<Offset> = shape.corners.map { point(it.x, it.y) }.inset(CutsceneGutter / 2f * unit)

    /** 칸 먹선 두께(px). */
    val inkPx: Float get() = CutsceneInkWidth * unit

    val screenCorners: List<Offset>
        get() = listOf(Offset.Zero, Offset(screen.width, 0f), Offset(screen.width, screen.height), Offset(0f, screen.height))
}

/**
 * 컷신 무대. 화면 전체에 [backdrop] 바탕을 깔고, 기준 캔버스를 시스템 바를 뺀 안전 영역에 비율 그대로 맞춘 [CutsceneFrame] 을 [content] 에 준다.
 * 바깥에서 이미 시스템 바를 피했으면 그만큼은 다시 빼지 않는다. 큰 화면에서는 [CutsceneCanvas.MAX_SCALE] 배까지만 키운다.
 */
@Composable
fun CutsceneStage(
    backdrop: () -> Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(CutsceneFrame) -> Unit,
) {
    val density = LocalDensity.current
    var screenInRoot by remember { mutableStateOf(Rect.Zero) }
    var safeInRoot by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(backdrop()) }
            .onPlaced { screenInRoot = it.boundsInRoot() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .onPlaced { safeInRoot = it.boundsInRoot() },
        )
        if (screenInRoot.isEmpty || safeInRoot.isEmpty) return@Box
        val frame = remember(screenInRoot, safeInRoot, density) {
            val canvasScale = min(
                safeInRoot.width / (CutsceneCanvas.WIDTH * density.density),
                safeInRoot.height / (CutsceneCanvas.HEIGHT * density.density),
            ).coerceAtMost(CutsceneCanvas.MAX_SCALE)
            val unit = canvasScale * density.density
            CutsceneFrame(
                screen = screenInRoot.size,
                origin = Offset(
                    safeInRoot.left - screenInRoot.left + (safeInRoot.width - CutsceneCanvas.WIDTH * unit) / 2f,
                    safeInRoot.top - screenInRoot.top + (safeInRoot.height - CutsceneCanvas.HEIGHT * unit) / 2f,
                ),
                unit = unit,
                canvasScale = canvasScale,
            )
        }
        content(frame)
    }
}

/**
 * 칸 하나. [corners] 모양 안에 [draw] 로 그림을 그리고 먹선을 두른다.
 * [appear] 가 0에서 1로 가며 살짝 크게 나타났다가 제자리에 탁 박히고, [drift] 면 안의 그림이 천천히 당겨진다.
 *
 * @param draw 칸 안에 그린다. 인자는 칸의 바깥 사각형(px)과 당김 배율이다.
 */
@Composable
fun CutscenePanelView(
    corners: List<Offset>,
    inkPx: Float,
    appear: () -> Float,
    drift: Boolean = true,
    draw: DrawScope.(bounds: Rect, zoom: Float) -> Unit,
) {
    val zoom = remember { Animatable(if (drift) DriftStartScale else 1f) }
    if (drift) LaunchedEffect(Unit) { zoom.animateTo(1f, tween(DriftMillis, easing = LinearEasing)) }
    val bounds = remember(corners) { corners.bounds() }
    val path = remember(corners) { corners.toPath() }
    val ink = NyummyTheme.colors.content.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val a = appear()
                val scale = PanelSlamScale + (1f - PanelSlamScale) * a
                scaleX = scale
                scaleY = scale
                alpha = (a * AppearAlphaSpeed).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(bounds.center.x / size.width, bounds.center.y / size.height)
            },
    ) {
        clipPath(path) { draw(bounds, zoom.value) }
        drawPath(path, ink, style = Stroke(width = inkPx, join = StrokeJoin.Miter))
    }
}

/** [imageSize] 그림이 [bounds] 를 빈틈없이 덮는 자리. 넘치는 쪽은 그림의 [focus] 자리가 [bounds] 가운데 오게 자르고, 그림 밖이 드러나지 않게 막는다. */
fun coverRect(imageSize: Size, bounds: Rect, focus: Offset, zoom: Float = 1f): Rect {
    val scale = max(bounds.width / imageSize.width, bounds.height / imageSize.height) * zoom
    val size = Size(imageSize.width * scale, imageSize.height * scale)
    // 그림이 한쪽에 딱 맞으면 부동소수점 오차로 범위 끝이 뒤집힐 수 있어, 작은 쪽을 아래 끝으로 잡는다.
    val left = (bounds.center.x - focus.x * size.width).coerceIn(min(bounds.right - size.width, bounds.left), bounds.left)
    val top = (bounds.center.y - focus.y * size.height).coerceIn(min(bounds.bottom - size.height, bounds.top), bounds.top)
    return Rect(Offset(left, top), size)
}

fun DrawScope.drawImageIn(image: ImageBitmap, rect: Rect, alpha: Float = 1f) = drawImage(
    image = image,
    dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
    dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
    alpha = alpha,
    filterQuality = FilterQuality.Low,
)

fun DrawScope.drawPainterIn(painter: Painter, rect: Rect, alpha: Float = 1f) {
    translate(rect.left, rect.top) {
        with(painter) { draw(rect.size, alpha = alpha) }
    }
}

val ImageBitmap.sizeF: Size get() = Size(width.toFloat(), height.toFloat())

fun List<Offset>.bounds(): Rect = Rect(minOf { it.x }, minOf { it.y }, maxOf { it.x }, maxOf { it.y })

fun List<Offset>.toPath(path: Path = Path()): Path = path.apply {
    reset()
    moveTo(first().x, first().y)
    drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/** 볼록 다각형을 모든 변에서 [distance] 만큼 안쪽으로 줄인다. 변마다 안쪽으로 평행 이동한 선끼리 만나는 점이 새 꼭짓점이다. */
private fun List<Offset>.inset(distance: Float): List<Offset> {
    val area = indices.sumOf { i -> this[i].cross(this[(i + 1) % size]).toDouble() }
    val sign = if (area >= 0) 1f else -1f
    val edges = indices.map { i ->
        val from = this[i]
        val direction = this[(i + 1) % size] - from
        val length = sqrt(direction.x * direction.x + direction.y * direction.y)
        val inward = Offset(-direction.y, direction.x) * (sign * distance / length)
        from + inward to direction
    }
    return indices.map { i ->
        val (p, r) = edges[(i - 1 + size) % size]
        val (q, s) = edges[i]
        p + r * ((q - p).cross(s) / r.cross(s))
    }
}

private fun Offset.cross(other: Offset) = x * other.y - y * other.x

/** 칸 사이 홈 너비와 칸 먹선 두께(캔버스 단위). */
private const val CutsceneGutter = 8f
private const val CutsceneInkWidth = 2.5f

private const val PanelSlamScale = 1.12f
private const val AppearAlphaSpeed = 3f
private const val DriftStartScale = 1.06f
private const val DriftMillis = 2_400
