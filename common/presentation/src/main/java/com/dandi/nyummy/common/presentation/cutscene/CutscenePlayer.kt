package com.dandi.nyummy.common.presentation.cutscene

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * [CutsceneSpec] 한 편을 재생하는 만화 컷신입니다. 화면 전체를 덮습니다.
 *
 * 페이지 바탕 위에 겹치지 않는 칸이 하나씩 박히고, 칸마다 그림을 잘라 넣고 먹선을 두릅니다.
 * 움직임은 세 가지뿐입니다. 칸이 탁 박히고, 효과음 글자가 튀어나오고, 마지막 칸이 화면 가득 펼쳐집니다.
 * 섬광, 빛줄기, 파티클은 쓰지 않고 칸이 박힐 때 진동을, 마지막 칸이 박힐 때만 짧은 흔들림을 더합니다.
 *
 * 끝나는 순간의 화면은 마지막 그림이 화면 전체를 덮은 모습이라, 다음 장면이 같은 그림([CutsceneFinale.image],
 * [CutsceneFinale.focus])을 화면 전체에 깔고 시작하면 이음매 없이 이어집니다. 마지막 글자는 끝나기 직전에 걷습니다.
 * 애니메이션 값은 그리기와 배치 단계에서만 읽어, 재생하는 동안 다시 구성하지 않습니다.
 */
@Composable
fun CutscenePlayer(
    spec: CutsceneSpec,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val panelIn = remember(spec) { spec.panels.map { Animatable(0f) } }
    val panelShown = remember(spec) { spec.panels.map { mutableStateOf(false) } }
    val wordIn = remember(spec) { spec.words.map { Animatable(0f) } }
    val wordShown = remember(spec) { spec.words.map { mutableStateOf(false) } }
    val finaleIn = remember(spec) { Animatable(0f) }
    val finaleWordIn = remember(spec) { Animatable(0f) }
    val finaleWordOut = remember(spec) { Animatable(0f) }
    val expand = remember(spec) { Animatable(0f) }
    val shake = remember(spec) { Animatable(0f) }
    var finaleShown by remember(spec) { mutableStateOf(false) }
    var backdrop by remember(spec) { mutableStateOf(spec.backdrop) }
    val backdropColor = animateColorAsState(backdrop.color(), tween(BackdropMillis), label = "cutsceneBackdrop")

    CutsceneStage(backdrop = { backdropColor.value }, modifier = modifier) { frame ->
        val panelCorners = remember(spec, frame) { spec.panels.map { frame.corners(it.shape) } }
        val finaleCorners = remember(spec, frame) { frame.corners(spec.finale.shape) }
        val requests = remember(spec, frame) {
            spec.panels.mapIndexed { index, panel -> CutsceneImageRequest(panel.image, panelCorners[index].bounds().size) } +
                CutsceneImageRequest(spec.finale.image, frame.screen)
        }
        val images = rememberCutsceneImages(requests)
        val latestImages by rememberUpdatedState(images)
        val finaleIndex = spec.panels.size

        LaunchedEffect(spec) {
            spec.panels.forEachIndexed { index, panel ->
                launch {
                    delay(panel.atMillis)
                    snapshotFlow { latestImages[index] }.first { it != null }
                    panelShown[index].value = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    panelIn[index].animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium))
                }
            }
            spec.words.forEachIndexed { index, word ->
                launch {
                    delay(word.atMillis)
                    wordShown[index].value = true
                    wordIn[index].animateTo(1f, spring(dampingRatio = WordPopDamping, stiffness = Spring.StiffnessMedium))
                }
            }
            spec.backdropChanges.forEach { change ->
                launch {
                    delay(change.atMillis)
                    backdrop = change.backdrop
                }
            }
            launch {
                val finale = spec.finale
                delay(finale.atMillis)
                snapshotFlow { latestImages[finaleIndex] }.first { it != null }
                finaleShown = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                launch { shake.animateTo(0f, shakeSpec(with(density) { FinaleShake.toPx() })) }
                launch { finaleIn.animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium)) }
                launch {
                    delay(FinaleWordDelayMillis)
                    finaleWordIn.animateTo(1f, spring(dampingRatio = WordPopDamping, stiffness = Spring.StiffnessMedium))
                }
                delay((finale.expandAtMillis - finale.atMillis).coerceAtLeast(0L))
                expand.animateTo(1f, tween(CutsceneExpandMillis.toInt(), easing = FastOutSlowInEasing))
                delay((finale.holdMillis - FinaleWordFadeMillis).coerceAtLeast(0L))
                finaleWordOut.animateTo(1f, tween(FinaleWordFadeMillis.toInt(), easing = FastOutLinearInEasing))
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = shake.value },
        ) {
            spec.panels.forEachIndexed { index, panel ->
                val image = images[index]
                if (panelShown[index].value && image != null) {
                    CutscenePanelView(corners = panelCorners[index], inkPx = frame.inkPx, appear = { panelIn[index].value }) { bounds, zoom ->
                        drawImageIn(image, coverRect(image.sizeF, bounds, panel.focus, zoom))
                    }
                }
            }

            val finaleImage = images[finaleIndex]
            if (finaleShown && finaleImage != null) {
                FinalePanel(
                    image = finaleImage,
                    corners = finaleCorners,
                    focus = spec.finale.focus,
                    frame = frame,
                    appear = { finaleIn.value },
                    expand = { expand.value },
                )
            }
        }

        // 효과음 글자는 글자 크기 설정과 상관없이 캔버스 배율로만 커지고 줄어 구도를 지킨다.
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            spec.words.forEachIndexed { index, word ->
                if (wordShown[index].value) {
                    CutsceneWordText(
                        text = stringResource(word.text),
                        ink = word.ink.color(),
                        sizeScale = word.scale * frame.canvasScale,
                        center = { frame.point(word.x, word.y) },
                        layer = {
                            val pop = wordIn[index].value
                            scaleX = pop
                            scaleY = pop
                            rotationZ = word.tiltDegrees
                            alpha = pop.coerceIn(0f, 1f) * (1f - expand.value)
                        },
                    )
                }
            }
            if (finaleShown) {
                val finale = spec.finale
                val window = finaleCorners.bounds()
                CutsceneWordText(
                    text = stringResource(finale.word),
                    ink = finale.wordInk.color(),
                    sizeScale = finale.wordScale * frame.canvasScale,
                    center = {
                        val inWindow = Offset(window.center.x, window.top + FinaleWordInset * frame.unit)
                        lerp(inWindow, frame.point(CutsceneCanvas.WIDTH / 2f, finale.wordTop), expand.value)
                    },
                    layer = {
                        val pop = finaleWordIn.value
                        val grow = lerp(1f, finale.wordFullScale, expand.value)
                        scaleX = pop * grow
                        scaleY = pop * grow
                        rotationZ = finale.wordTiltDegrees
                        alpha = pop.coerceIn(0f, 1f) * (1f - finaleWordOut.value)
                    },
                )
            }
        }
    }
}

/**
 * 마지막 칸. 칸 모양 창으로 화면 전체 그림의 일부를 보여 주다가, 창이 화면 가득 펼쳐진다.
 * 창 안에서는 그림의 초점(냐미 얼굴)이 창 가운데 오게 옮겨 두고, 펼쳐지면서 제자리로 돌아온다.
 */
@Composable
private fun FinalePanel(
    image: ImageBitmap,
    corners: List<Offset>,
    focus: Offset,
    frame: CutsceneFrame,
    appear: () -> Float,
    expand: () -> Float,
) {
    val screen = frame.screen
    val window = remember(corners) { corners.bounds() }
    val full = remember(image, screen) { coverRect(image.sizeF, Rect(Offset.Zero, screen), focus) }
    val toWindow = window.center - (full.topLeft + Offset(focus.x * full.width, focus.y * full.height))
    val ink = NyummyTheme.colors.content.primary
    val path = remember { Path() }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val a = appear()
                val scale = FinaleSlamScale + (1f - FinaleSlamScale) * a
                scaleX = scale
                scaleY = scale
                alpha = (a * FinaleAlphaSpeed).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(window.center.x / screen.width, window.center.y / screen.height)
            },
    ) {
        val t = expand()
        corners.zip(frame.screenCorners) { from, to -> lerp(from, to, t) }.toPath(path)
        clipPath(path) { drawImageIn(image, full.translate(toWindow * (1f - t))) }
        if (t < 1f) drawPath(path, ink, style = Stroke(width = frame.inkPx * (1f - t), join = StrokeJoin.Miter))
    }
}

/** 흰 글자에 색 테두리를 두른 효과음 글자. ([center]) 가 글자 가운데다. 화면 낭독기는 한 번만 읽는다. */
@Composable
private fun CutsceneWordText(
    text: String,
    ink: Color,
    sizeScale: Float,
    center: () -> Offset,
    layer: GraphicsLayerScope.() -> Unit,
) {
    val base = NyummyTheme.typography.displayL
    val style = base.copy(fontSize = base.fontSize * WordBaseScale * sizeScale)
    val strokePx = with(LocalDensity.current) { WordStroke.toPx() * sizeScale }
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    val c = center()
                    placeable.place((c.x - placeable.width / 2f).roundToInt(), (c.y - placeable.height / 2f).roundToInt())
                }
            }
            .graphicsLayer(layer)
            .clearAndSetSemantics { contentDescription = text },
    ) {
        NyummyText(
            text = text,
            style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        NyummyText(text = text, style = style, color = NyummyTheme.colors.bg.surface, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun CutsceneBackdrop.color(): Color = when (this) {
    CutsceneBackdrop.Paper -> NyummyTheme.colors.bg.sceneCutscenePaper
    CutsceneBackdrop.Dawn -> NyummyTheme.colors.bg.sceneCutsceneDawn
    CutsceneBackdrop.Night -> NyummyTheme.colors.bg.sceneCutsceneNight
}

@Composable
private fun CutsceneInk.color(): Color = when (this) {
    CutsceneInk.Brand -> NyummyTheme.colors.content.brand
    CutsceneInk.Hot -> NyummyTheme.colors.content.danger
    CutsceneInk.Sleepy -> NyummyTheme.colors.content.secondary
    CutsceneInk.Sunny -> NyummyTheme.colors.content.promo
}

/** 화면 전체를 좌우로 짧게 한 번 흔든다(px). */
fun shakeSpec(amplitude: Float) = keyframes {
    durationMillis = ShakeMillis
    amplitude at ShakeMillis / 6
    -amplitude * 0.6f at ShakeMillis * 2 / 6
    amplitude * 0.3f at ShakeMillis * 3 / 6
    -amplitude * 0.1f at ShakeMillis * 4 / 6
    0f at ShakeMillis
}

private const val FinaleSlamScale = 1.06f
private const val FinaleAlphaSpeed = 3f
private const val SlamDamping = 0.6f

private const val WordBaseScale = 1.8f
private const val WordPopDamping = 0.42f
private val WordStroke = 5.dp

/** 마지막 칸 글자를 칸 위쪽에서 얼마나 안으로 둘지(캔버스 단위). */
private const val FinaleWordInset = 34f
private const val FinaleWordDelayMillis = 120L
private const val FinaleWordFadeMillis = 250L

private val FinaleShake = 2.dp
private const val ShakeMillis = 260

private const val BackdropMillis = 600
