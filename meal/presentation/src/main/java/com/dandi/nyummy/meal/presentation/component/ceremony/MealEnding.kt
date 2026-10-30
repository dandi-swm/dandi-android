package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.cutscene.CutsceneBackdrop
import com.dandi.nyummy.common.presentation.cutscene.CutsceneFrame
import com.dandi.nyummy.common.presentation.cutscene.CutsceneImageRequest
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage.Seam
import com.dandi.nyummy.common.presentation.cutscene.CutscenePanelView
import com.dandi.nyummy.common.presentation.cutscene.CutsceneStage
import com.dandi.nyummy.common.presentation.cutscene.bounds
import com.dandi.nyummy.common.presentation.cutscene.color
import com.dandi.nyummy.common.presentation.cutscene.coverRect
import com.dandi.nyummy.common.presentation.cutscene.drawImageIn
import com.dandi.nyummy.common.presentation.cutscene.drawPainterIn
import com.dandi.nyummy.common.presentation.cutscene.rememberCutsceneImages
import com.dandi.nyummy.common.presentation.cutscene.shakeSpec
import com.dandi.nyummy.common.presentation.cutscene.sizeF
import com.dandi.nyummy.common.presentation.cutscene.toPath
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.presentation.MealCutscene
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.meal.presentation.mealPhotoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 식사 기록 연출의 마무리 장면입니다. 컷신의 마지막 그림이 화면 가득 깔린 모습에서 시작해 만화 마지막 페이지로 끝납니다.
 *
 * 1. 마지막 그림이 위쪽 칸으로 접히면서, 칸 안의 그림이 방금 찍은 음식 사진으로 탁 바뀝니다. 냐미가 먹은 밥이 이 사진이라는 연결입니다.
 *    마지막 그림을 다 읽은 뒤에 접기 시작하고, 그때 [onFinaleShown] 으로 알립니다. 그 전까지는 컷신이 마지막 화면으로 위를 덮고 있습니다.
 * 2. 아래 큰 칸에 배부른 냐미가 박힙니다.
 * 3. 사진 위에 냐미 발도장이 쾅 찍힙니다(진동, 짧은 흔들림 한 번).
 * 4. "냐미가 맛있게 먹었어요!" 내레이션 상자가 위 칸 경계에, 영양 분석 안내 상자가 아래 칸 경계에 차례로 박힙니다.
 *    [showDone] 이면 완료 버튼이 나옵니다.
 *
 * 컷신과 같은 무대([CutsceneStage])와 칸([CutscenePanelView])을 쓰고, 섬광이나 빛줄기는 쓰지 않습니다.
 * 사진은 칸에 맞춰 자를 뿐 다른 변형은 하지 않습니다.
 *
 * @param title 내레이션 상자 글. 상자가 우편함에 들어가면 "우편함에 쏙!"으로 바뀐다.
 * @param playIntro 처음부터 재생할지. 처음 그릴 때만 본다. 아니면 끝난 모습으로 둔다.
 */
@Composable
internal fun MealEnding(
    photoPath: String,
    cutscene: MealCutscene,
    @StringRes title: Int,
    showDone: Boolean,
    playIntro: Boolean,
    onFinaleShown: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val start = remember { if (playIntro) 0f else 1f }
    val fold = remember { Animatable(start) }
    val epilogueIn = remember { Animatable(start) }
    val stampIn = remember { Animatable(start) }
    val captionIn = remember { Animatable(start) }
    val noteIn = remember { Animatable(start) }
    val shake = remember { Animatable(0f) }
    var finaleShown by remember { mutableStateOf(start == 1f) }
    val latestOnFinaleShown by rememberUpdatedState(onFinaleShown)
    val finale = cutscene.spec.finale
    val stamp = painterResource(R.drawable.meal_ending_paw_stamp)
    val stampTint = ColorFilter.tint(NyummyTheme.colors.content.danger.copy(alpha = StampAlpha))
    val paper = CutsceneBackdrop.Paper.color()

    LaunchedEffect(Unit) {
        if (start == 1f) return@LaunchedEffect
        // 그림을 읽는 사이에 접기가 앞서 나가면 첫 움직임이 잘려 보인다.
        snapshotFlow { finaleShown }.first { it }
        fold.animateTo(1f, tween(FoldMillis, easing = FastOutSlowInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch { epilogueIn.animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium)) }
        delay(StampDelayMillis)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { shake.animateTo(0f, shakeSpec(with(density) { StampShake.toPx() })) }
        launch { stampIn.animateTo(1f, spring(dampingRatio = StampDamping, stiffness = Spring.StiffnessMedium)) }
        delay(CaptionDelayMillis)
        captionIn.animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium))
        delay(NoteDelayMillis)
        noteIn.animateTo(1f, spring(dampingRatio = SlamDamping, stiffness = Spring.StiffnessMedium))
    }

    CutsceneStage(backdrop = { paper }, modifier = modifier) { frame ->
        val photoCorners = remember(frame) { frame.corners(PhotoShape) }
        val epilogueCorners = remember(frame) { frame.corners(EpilogueShape) }
        val requests = remember(frame, finale) {
            listOf(
                CutsceneImageRequest(finale.image, frame.screen),
                CutsceneImageRequest(R.drawable.meal_cutscene_epilogue, epilogueCorners.bounds().size),
            )
        }
        val images = rememberCutsceneImages(requests)
        val finaleLoaded = images[0] != null
        LaunchedEffect(finaleLoaded) {
            if (finaleLoaded) {
                finaleShown = true
                latestOnFinaleShown()
            }
        }
        // 사진은 칸 크기로만 읽는다. 원본 크기로 읽으면 카메라 사진 한 장이 수십 MB 다.
        val context = LocalContext.current
        val photoSize = remember(photoCorners) { photoCorners.bounds().size }
        val photoRequest = remember(context, photoPath, photoSize) {
            ImageRequest.Builder(context)
                .data(mealPhotoModel(photoPath))
                .size(photoSize.width.roundToInt(), photoSize.height.roundToInt())
                .build()
        }
        val photo = rememberAsyncImagePainter(photoRequest)
        val ink = NyummyTheme.colors.content.primary
        val path = remember { Path() }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = shake.value },
        ) {
            // 1. 화면 가득한 마지막 그림이 사진 칸으로 접히며 음식 사진으로 바뀐다.
            Canvas(Modifier.fillMaxSize()) {
                val t = fold.value
                val corners = frame.screenCorners.zip(photoCorners) { from, to -> lerp(from, to, t) }
                corners.toPath(path)
                val window = corners.bounds()
                // 흐려지며 겹치지 않게, 접히는 도중 한 프레임에 음식 사진으로 바꿔 낀다. 사진을 아직 못 읽었으면 그림을 그대로 둔다.
                val photoSizeNow = photo.intrinsicSize
                val photoReady = photoSizeNow.isSpecified && photoSizeNow.width > 0f
                clipPath(path) {
                    if (photoReady && t >= SwapAt) {
                        drawPainterIn(photo, coverRect(photoSizeNow, window, CenterFocus))
                    } else {
                        images[0]?.let { drawImageIn(it, coverRect(it.sizeF, window, finale.focus)) }
                    }
                }
                if (t > 0f) drawPath(path, ink, style = Stroke(width = frame.inkPx * t, join = StrokeJoin.Miter))
            }

            // 2. 배부른 냐미.
            images[1]?.let { image ->
                CutscenePanelView(corners = epilogueCorners, inkPx = frame.inkPx, appear = { epilogueIn.value }) { bounds, zoom ->
                    drawImageIn(image, coverRect(image.sizeF, bounds, EpilogueFocus, zoom))
                }
            }

            // 3. 발도장 쾅.
            val stampSize = StampSize * frame.canvasScale
            Image(
                painter = stamp,
                contentDescription = null,
                colorFilter = stampTint,
                modifier = Modifier
                    .centeredAt { frame.point(StampX, StampY) }
                    .size(stampSize.dp)
                    .graphicsLayer {
                        val s = stampIn.value
                        val scale = lerp(StampFromScale, 1f, s)
                        scaleX = scale
                        scaleY = scale
                        rotationZ = StampTiltDegrees
                        alpha = (s * StampAlphaSpeed).coerceIn(0f, 1f)
                    },
            )
        }

        // 4. 내레이션 상자 둘. 위 칸 경계 왼쪽에 먹었다는 말, 아래 칸 경계 오른쪽에 영양 분석 안내.
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            EndingCaption(
                text = catNameStringResource(title),
                frame = frame,
                anchor = Offset(CaptionX, CaptionY),
                alignEnd = false,
                tiltDegrees = CaptionTiltDegrees,
                textScale = CaptionTextScale,
                appear = { captionIn.value },
            )
            EndingCaption(
                text = stringResource(R.string.meal_ceremony_analysis_pending),
                frame = frame,
                anchor = Offset(NoteEndX, NoteY),
                alignEnd = true,
                tiltDegrees = NoteTiltDegrees,
                textScale = NoteTextScale,
                appear = { noteIn.value },
            )
        }
        EndingDoneButton(visible = showDone, onClick = onDoneClick)
    }
}

/**
 * 만화 내레이션 상자. 흰 바탕에 먹선을 두르고 살짝 기울여 칸 경계에 걸쳐 박힌다.
 *
 * @param anchor 상자 윗변의 기준 자리(캔버스 단위). [alignEnd] 면 오른쪽 끝, 아니면 왼쪽 끝이 여기에 온다.
 */
@Composable
private fun EndingCaption(
    text: String,
    frame: CutsceneFrame,
    anchor: Offset,
    alignEnd: Boolean,
    tiltDegrees: Float,
    textScale: Float,
    appear: () -> Float,
) {
    val base = NyummyTheme.typography.displayL
    val style = base.copy(fontSize = base.fontSize * textScale * frame.canvasScale)
    val inkDp = with(LocalDensity.current) { frame.inkPx.toDp() }
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    val at = frame.point(anchor.x, anchor.y)
                    val x = if (alignEnd) at.x - placeable.width else at.x
                    placeable.place(x.roundToInt(), at.y.roundToInt())
                }
            }
            .graphicsLayer {
                val a = appear()
                val scale = lerp(CaptionFromScale, 1f, a)
                scaleX = scale
                scaleY = scale
                rotationZ = tiltDegrees
                alpha = (a * StampAlphaSpeed).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0.5f)
            }
            .background(NyummyTheme.colors.bg.surface)
            .border(inkDp, NyummyTheme.colors.content.primary)
            .padding(horizontal = NyummyTheme.spacing.s16 * frame.canvasScale, vertical = NyummyTheme.spacing.s12 * frame.canvasScale),
    ) {
        NyummyText(text = text, style = style, color = NyummyTheme.colors.content.primary, maxLines = 1)
    }
}

@Composable
private fun BoxScope.EndingDoneButton(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(ButtonFadeMillis)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s24),
    ) {
        NyummyButton(
            text = stringResource(R.string.meal_record_done),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.L,
            onClick = onClick,
        )
    }
}

/** 재고 나면 가운데가 [center] 에 오게 놓는다. */
private fun Modifier.centeredAt(center: () -> Offset) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val c = center()
        placeable.place((c.x - placeable.width / 2f).roundToInt(), (c.y - placeable.height / 2f).roundToInt())
    }
}


/** 마무리 장면이 다 박히고 영양 분석 안내를 읽을 만큼 머무는 시간. 이 뒤에 상자 장면이나 완료 버튼으로 넘어간다. */
internal const val MealEndingMillis = 2_800L

// 페이지: 위 칸은 음식 사진(4:3에 가깝게), 아래 칸은 배부른 냐미(3:2에 가깝게). 그 아래는 완료 버튼 자리로 비운다.
private val PhotoBottom = Seam(300f, 284f)
private val PhotoShape = CutscenePage.row(CutscenePage.Top, PhotoBottom)
private val EpilogueShape = CutscenePage.row(PhotoBottom, Seam(566f, 582f))
private val EpilogueFocus = Offset(0.6f, 0.5f)
private val CenterFocus = Offset(0.5f, 0.5f)

/** 발도장 자리(가운데, 캔버스 단위)와 크기. 사진 칸 오른쪽 아래에 걸친다. */
private const val StampX = 284f
private const val StampY = 236f
private const val StampSize = 116f
private const val StampTiltDegrees = -14f
private const val StampFromScale = 2.2f
private const val StampDamping = 0.5f
private const val StampAlpha = 0.9f
private const val StampAlphaSpeed = 3f
private val StampShake = 5.dp

/** 내레이션 상자 왼쪽 위 자리(캔버스 단위). 두 칸 경계에 걸친다. */
private const val CaptionX = 14f
private const val CaptionY = 266f
private const val CaptionTiltDegrees = -1.5f
private const val CaptionTextScale = 1.1f
private const val CaptionFromScale = 1.15f

/** 영양 분석 안내 상자 오른쪽 위 자리(캔버스 단위). 배부른 냐미 칸 아래 경계에 걸친다. 글이 길어 조금 작게 쓴다. */
private const val NoteEndX = 346f
private const val NoteY = 556f
private const val NoteTiltDegrees = 1.5f
private const val NoteTextScale = 0.72f

private const val FoldMillis = 450

/** 접히는 동안 마지막 그림이 음식 사진으로 바뀌는 순간(접힘 진행 비율). */
private const val SwapAt = 0.5f
private const val StampDelayMillis = 450L
private const val CaptionDelayMillis = 300L
private const val NoteDelayMillis = 150L
private const val SlamDamping = 0.6f
private const val ButtonFadeMillis = 220
