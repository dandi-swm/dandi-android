package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 냐미가 한 입 먹는 과정을 만화 컷으로 보여 주는 "띠욧!" 컷신입니다. 화면 전체를 덮습니다.
 *
 * 데이브 더 다이브의 맛보기 컷신처럼 검은 바탕 위에 비스듬한 컷이 하나씩 탁탁 박힙니다.
 * 숟가락으로 뜨기 → 들어 올리기 → 반짝이는 눈 → "앙!" 한 입 → 별눈 띠욧 순서이고, 세로 화면에 맞춰 위에서 아래로 쌓입니다.
 * 마지막 컷은 띠욧 장면을 컷 모양 창으로 들여다보는 것이라, 창이 화면 가득 펼쳐지면 그대로 띠욧 화면이 됩니다.
 * 마지막 컷이 박히는 순간 [onPop] 을 불러 부모가 섬광과 화면 흔들림을 맡습니다.
 */
@Composable
internal fun TasteReactionCutIn(
    art: CeremonyArt,
    onPop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val latestOnPop by rememberUpdatedState(onPop)
    val panelIn = remember { ComicPanels.map { Animatable(0f) } }
    val climaxIn = remember { Animatable(0f) }
    val expand = remember { Animatable(0f) }
    val wordPop = remember { Animatable(0f) }
    val biteWordPop = remember { Animatable(0f) }
    var climaxShown by remember { mutableStateOf(false) }
    var artFrame by remember { mutableIntStateOf(StarFrames.first()) }

    LaunchedEffect(Unit) {
        // 1~4번 컷이 하나씩 박힌다.
        ComicPanels.forEachIndexed { index, panel ->
            delay(panel.delayMillis)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            launch { panelIn[index].animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium)) }
            if (panel.hasBiteWord) {
                launch {
                    delay(BiteWordDelayMillis)
                    biteWordPop.animateTo(1f, spring(dampingRatio = WordPopDamping, stiffness = Spring.StiffnessMedium))
                }
            }
        }
        // 5번 컷: 별눈 띠욧이 박힌다.
        delay(ClimaxDelayMillis)
        climaxShown = true
        latestOnPop()
        launch { climaxIn.animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium)) }
        launch { wordPop.animateTo(1f, spring(dampingRatio = WordPopDamping, stiffness = Spring.StiffnessMedium)) }
        launch {
            var index = 0
            while (isActive) {
                artFrame = StarFrames[index % StarFrames.size]
                index++
                delay(StarFrameMillis)
            }
        }
        // 마지막 컷이 화면 가득 펼쳐진다.
        delay(ExpandDelayMillis)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        expand.animateTo(1f, tween(ExpandMillis, easing = FastOutSlowInEasing))
    }

    val page = NyummyTheme.colors.bg.surfaceInverse
    val white = NyummyTheme.colors.bg.surface
    val outline = NyummyTheme.colors.content.brand

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(page),
    ) {
        val density = LocalDensity.current
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val screen = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val padPx = with(density) { PagePadding.toPx() }
        val pageOrigin = Offset(padPx, padPx)
        val pageSize = Size(screen.width - padPx * 2f, screen.height - padPx * 2f)
        fun toPx(p: Offset) = Offset(pageOrigin.x + p.x * pageSize.width, pageOrigin.y + p.y * pageSize.height)
        val gutterPx = with(density) { Gutter.toPx() }

        // 1~4번 컷.
        ComicPanels.forEachIndexed { index, panel ->
            val points = panel.points.map(::toPx)
            ComicPanel(
                points = points,
                imageRes = panel.imageRes,
                appear = panelIn[index].value,
                gutterColor = page,
                gutterPx = gutterPx,
            )
        }
        val biteBounds = bounds(ComicPanels.first { it.hasBiteWord }.points.map(::toPx))
        val biteWord = stringResource(R.string.meal_ceremony_bite_word)
        OutlinedWord(
            text = biteWord,
            fill = white,
            outline = outline,
            modifier = Modifier
                .offset { IntOffset((biteBounds.right - biteBounds.width * 0.3f).roundToInt(), (biteBounds.top + biteBounds.height * 0.18f).roundToInt()) }
                .graphicsLayer {
                    val scale = BiteWordScale * biteWordPop.value
                    scaleX = scale
                    scaleY = scale
                    rotationZ = BiteWordTiltDegrees
                    alpha = biteWordPop.value.coerceIn(0f, 1f) * (1f - expand.value)
                },
        )

        // 5번 컷: 띠욧 화면을 컷 모양 창으로 보여 주다가, 창이 화면 가득 펼쳐진다.
        if (climaxShown) {
            val t = expand.value
            val fullRect = listOf(Offset.Zero, Offset(screen.width, 0f), Offset(screen.width, screen.height), Offset(0f, screen.height))
            val window = ClimaxPanel.map(::toPx).zip(fullRect) { from, to -> lerp(from, to, t) }
            val windowBounds = bounds(window)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(polygonShape(window))
                    .graphicsLayer {
                        val slam = ClimaxSlamScale + (1f - ClimaxSlamScale) * climaxIn.value
                        scaleX = slam
                        scaleY = slam
                        transformOrigin = TransformOrigin(0.5f, windowBounds.center.y / screen.height)
                    },
            ) {
                GlowBurstBackground(
                    modifier = Modifier.fillMaxSize(),
                    focus = Offset(0.5f, lerp(ClimaxFocusInPanel, FullFocusY, t)),
                    intensity = lerp(PanelGlowIntensity, FullGlowIntensity, t),
                )
                CeremonyArtView(
                    art = art,
                    width = screenWidth * ArtWidthFraction,
                    contentDescription = stringResource(R.string.meal_record_cat_content_description),
                    frame = artFrame,
                    // 화면보다 조금 넓게 그려 좌우로 넘치게 하고, 아래 끝은 화면 밖에 둬 그림 끝이 보이지 않게 한다.
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = screenHeight * lerp(ArtSinkInPanel, ArtSinkFull, t))
                        .requiredWidth(screenWidth * ArtWidthFraction),
                )
                if (t > BurstStart) {
                    IconBurst(
                        icons = listOf(
                            painterResource(CommonR.drawable.nyummy_ic_heart),
                            painterResource(CommonR.drawable.nyummy_ic_star_filled),
                            painterResource(R.drawable.ic_meal_sparkle),
                        ),
                        tints = listOf(white, NyummyTheme.colors.data.coin, white),
                        modifier = Modifier.fillMaxSize(),
                        origin = Offset(0.5f, FullFocusY),
                        pieceCount = BurstPieces,
                        iconSize = BurstIconSize,
                    )
                }
            }
            // 띠욧 글자: 컷 위쪽에 박혔다가, 펼쳐지면 화면 위쪽 자리로 올라간다.
            val word = stringResource(R.string.meal_ceremony_reaction_word)
            OutlinedWord(
                text = word,
                fill = white,
                outline = outline,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * lerp(WordTopInPanel, WordTopFull, t))
                    .graphicsLayer {
                        val scale = lerp(WordScaleInPanel, WordScaleFull, t) * wordPop.value
                        scaleX = scale
                        scaleY = scale
                        rotationZ = WordTiltDegrees
                        alpha = wordPop.value.coerceIn(0f, 1f)
                    },
            )
        }
    }
}

/**
 * 만화 컷 하나. [points] 모양으로 그림을 잘라 넣고, 테두리에 검은 칸막이를 그린다.
 * 들어올 때는 살짝 크게 나타났다가 제자리에 쾅 박히고, 안의 그림은 천천히 당겨져 살아 있는 느낌을 준다.
 */
@Composable
private fun ComicPanel(
    points: List<Offset>,
    @DrawableRes imageRes: Int,
    appear: Float,
    gutterColor: Color,
    gutterPx: Float,
) {
    if (appear <= 0f) return
    val box = bounds(points)
    val density = LocalDensity.current
    val drift = remember { Animatable(DriftStartScale) }
    LaunchedEffect(Unit) { drift.animateTo(1f, tween(DriftMillis, easing = LinearEasing)) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = PanelSlamScale + (1f - PanelSlamScale) * appear
                scaleX = scale
                scaleY = scale
                alpha = (appear * 3f).coerceAtMost(1f)
                transformOrigin = TransformOrigin(box.center.x / size.width, box.center.y / size.height)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(polygonShape(points)),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset { IntOffset(box.left.roundToInt(), box.top.roundToInt()) }
                    .size(with(density) { box.width.toDp() }, with(density) { box.height.toDp() })
                    .graphicsLayer {
                        scaleX = drift.value
                        scaleY = drift.value
                    },
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, gutterColor, style = Stroke(width = gutterPx, join = StrokeJoin.Miter))
        }
    }
}

/** 흰 글자에 그린 테두리를 두른 만화 효과음 글자. 화면 낭독기는 한 번만 읽는다. */
@Composable
private fun OutlinedWord(
    text: String,
    fill: Color,
    outline: Color,
    modifier: Modifier = Modifier,
) {
    val style = NyummyTheme.typography.displayL
    val strokePx = with(LocalDensity.current) { WordStroke.toPx() }
    Box(modifier = modifier.clearAndSetSemantics { contentDescription = text }) {
        NyummyText(
            text = text,
            style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
            color = outline,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        NyummyText(text = text, style = style, color = fill, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** 꼭짓점 목록으로 만든 다각형 모양(px). */
private fun polygonShape(points: List<Offset>): Shape = GenericShape { _, _ ->
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

private fun bounds(points: List<Offset>): Rect = Rect(
    left = points.minOf { it.x },
    top = points.minOf { it.y },
    right = points.maxOf { it.x },
    bottom = points.maxOf { it.y },
)

/**
 * 컷 하나의 정의. [points] 는 페이지(화면에서 여백을 뺀 영역) 크기 대비 비율이고, [delayMillis] 는 앞 컷 다음 쉬는 시간이다.
 * [hasBiteWord] 인 컷은 박힌 뒤 "앙!" 효과음 글자가 튀어나온다.
 */
@Immutable
private data class ComicPanelSpec(
    val points: List<Offset>,
    @DrawableRes val imageRes: Int,
    val delayMillis: Long,
    val hasBiteWord: Boolean = false,
)

/**
 * 세로 페이지의 컷 배치. 줄마다 기울기를 엇갈려 만화 페이지처럼 보이게 한다.
 * 1줄 = 숟가락으로 뜨기, 2줄 = 들어 올리기(왼쪽)와 반짝이는 눈(오른쪽), 3줄 = 앙!, 4줄 = 띠욧(마지막 컷).
 */
private val Row1Bottom = Offset(0.285f, 0.235f)
private val Row2Bottom = Offset(0.47f, 0.50f)
private val Row3Bottom = Offset(0.70f, 0.655f)
private const val Row2DividerTop = 0.60f
private const val Row2DividerBottom = 0.55f

private fun rowY(row: Offset, x: Float) = row.x + (row.y - row.x) * x

private val ComicPanels = listOf(
    // 숟가락으로 크게 뜨기
    ComicPanelSpec(
        points = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, Row1Bottom.y), Offset(0f, Row1Bottom.x)),
        imageRes = R.drawable.meal_ceremony_panel_scoop,
        delayMillis = 300L,
    ),
    // 들어 올리며 침 꼴깍
    ComicPanelSpec(
        points = listOf(
            Offset(0f, Row1Bottom.x),
            Offset(Row2DividerTop, rowY(Row1Bottom, Row2DividerTop)),
            Offset(Row2DividerBottom, rowY(Row2Bottom, Row2DividerBottom)),
            Offset(0f, Row2Bottom.x),
        ),
        imageRes = R.drawable.meal_ceremony_panel_lift,
        delayMillis = 550L,
    ),
    // 반짝이는 눈
    ComicPanelSpec(
        points = listOf(
            Offset(Row2DividerTop, rowY(Row1Bottom, Row2DividerTop)),
            Offset(1f, Row1Bottom.y),
            Offset(1f, Row2Bottom.y),
            Offset(Row2DividerBottom, rowY(Row2Bottom, Row2DividerBottom)),
        ),
        imageRes = R.drawable.meal_ceremony_panel_eyes,
        delayMillis = 500L,
    ),
    // 앙!
    ComicPanelSpec(
        points = listOf(Offset(0f, Row2Bottom.x), Offset(1f, Row2Bottom.y), Offset(1f, Row3Bottom.y), Offset(0f, Row3Bottom.x)),
        imageRes = R.drawable.meal_ceremony_panel_bite,
        delayMillis = 550L,
        hasBiteWord = true,
    ),
)

/** 마지막 컷(띠욧) 자리. */
private val ClimaxPanel = listOf(Offset(0f, Row3Bottom.x), Offset(1f, Row3Bottom.y), Offset(1f, 1f), Offset(0f, 1f))

private val PagePadding = 10.dp
private val Gutter = 8.dp
private const val PanelSlamScale = 1.12f
private const val ClimaxSlamScale = 1.08f
private const val SlamDamping = 0.6f
private const val DriftStartScale = 1.06f
private const val DriftMillis = 2_400

private const val BiteWordDelayMillis = 160L
private const val BiteWordScale = 1.6f
private const val BiteWordTiltDegrees = 8f

private const val ClimaxDelayMillis = 800L
private const val ExpandDelayMillis = 900L
private const val ExpandMillis = 450
private const val HoldAfterExpandMillis = 1_500L

/** 별눈 반짝 칸(띠욧 시트). */
private val StarFrames = listOf(3, 4, 5, 4)
private const val StarFrameMillis = 170L

private const val ClimaxFocusInPanel = 0.82f
private const val FullFocusY = 0.5f
private const val PanelGlowIntensity = 0.85f
private const val FullGlowIntensity = 1f

private const val ArtWidthFraction = 1.12f

/** 컷 안에서는 얼굴이 마지막 컷 창 안에 오도록 아래로 내려 두고, 펼쳐지면 띠욧 화면 자리로 올라간다. */
private const val ArtSinkInPanel = 0.08f
private const val ArtSinkFull = 0.02f
private const val BurstStart = 0.6f
private const val BurstPieces = 16
private val BurstIconSize = 22.dp

private const val WordTopInPanel = 0.66f
private const val WordTopFull = 0.14f
private const val WordScaleInPanel = 2.4f
private const val WordScaleFull = 3.6f
private const val WordTiltDegrees = -6f
private const val WordPopDamping = 0.42f
private val WordStroke = 5.dp

/** 컷신이 시작해서 띠욧 화면에서 머물다 끝날 때까지 걸리는 시간(약 5.4초). */
internal val TasteReactionTotalMillis: Long =
    ComicPanels.sumOf { it.delayMillis } + ClimaxDelayMillis + ExpandDelayMillis + ExpandMillis + HoldAfterExpandMillis
