package com.dandi.nyummy.common.presentation.coachmark

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBubbleTail
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialSkipButton
import com.dandi.nyummy.common.presentation.designsystem.component.TutorialCardBodyTop
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.reward.rememberReducedMotion
import kotlin.math.roundToInt

/**
 * 코치마크를 그리는 맨 위 층. 루트가 화면과 바텀 네비 위에 둔다. 띄울 코치마크가 없으면 아무것도 그리지 않는다.
 * 뒤로 가기는 가로채지 않는다.
 */
@Composable
fun CoachMarkLayer(host: CoachMarkHost, modifier: Modifier = Modifier) {
    val mark = host.mark ?: return
    val target = mark.target?.let { host.targets[it] }
    CoachMarkContent(
        mark = mark,
        targetBounds = target?.bounds,
        anchorBounds = mark.anchor?.let { host.targets[it]?.bounds },
        onTargetClick = {
            target?.onClick?.invoke()
            mark.onTargetClick()
        },
        modifier = modifier,
    )
}

/**
 * 덮개, 카드, 건너뛰기. 덮개는 [targetBounds] 둘레만 둥글게 비우고 빛 링, 숨쉬는 맥박 링, 꾹꾹 누르는 발바닥을 얹는다.
 * 카드는 대상이 화면 아래 절반이면 대상 바로 위(꼬리 아래), 위 절반이면 바로 아래(꼬리 위)에 붙는다.
 * 대상이 없으면 [anchorBounds] 바로 위, 그것도 없으면 화면 아래에 둔다.
 * 건너뛰기는 화면 오른쪽 위(상태바 아래 상단 바 줄)에 고정하고, 비운 곳과 겹치면 그리지 않는다.
 * 덮개는 모든 터치를 받고, 비운 곳을 누를 때만 [onTargetClick]을 부른다. 영역을 아직 모르면 비우지 않고 카드만 보인다.
 * [CoachMark.blocksTouches]가 false면 덮개를 그리지 않고 터치도 받지 않는다. 대상의 빛 링·발바닥과 카드만 보인다.
 * 이때 터치를 받는 건 카드 몸통과 보이는 건너뛰기뿐이다. 숨긴 건너뛰기나 카드 위 냐미 자리는 아래 화면에 그대로 넘긴다.
 */
@Composable
internal fun CoachMarkContent(
    mark: CoachMark,
    targetBounds: Rect?,
    anchorBounds: Rect?,
    onTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val spot = targetBounds?.inflate(with(density) { SpotPadding.toPx() })
    val spotRadius = spot?.let { minOf(it.width, it.height, with(density) { SpotMaxRadius.toPx() * 2 }) / 2 } ?: 0f
    val latestOnTargetClick by rememberUpdatedState(onTargetClick)
    val scrim = NyummyTheme.colors.bg.scrim
    val ring = NyummyTheme.colors.bg.surface
    val reducedMotion = rememberReducedMotion()
    val motion = rememberInfiniteTransition(label = "coachMark")
    val pulse by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(PulseMillis), RepeatMode.Restart), label = "pulse")
    val press by motion.animateFloat(1f, PawPressScale, infiniteRepeatable(tween(PawMillis), RepeatMode.Reverse), label = "paw")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                if (mark.blocksTouches) drawRect(scrim)
                if (spot == null) return@drawBehind
                if (mark.blocksTouches) {
                    drawRoundRect(Color.Black, spot.topLeft, spot.size, CornerRadius(spotRadius), blendMode = BlendMode.Clear)
                }
                drawRoundRect(ring, spot.topLeft, spot.size, CornerRadius(spotRadius), style = Stroke(RingWidth.toPx()))
                if (reducedMotion) return@drawBehind
                val grow = PulseGrow.toPx() * pulse
                val pulseRect = spot.inflate(grow)
                drawRoundRect(
                    color = ring.copy(alpha = PulseAlpha * (1f - pulse)),
                    topLeft = pulseRect.topLeft,
                    size = pulseRect.size,
                    cornerRadius = CornerRadius(spotRadius + grow),
                    style = Stroke(RingWidth.toPx()),
                )
            }
            // 화면을 막지 않으면 터치 처리기를 아예 달지 않는다. 빈 처리기라도 있으면 아래 화면으로 탭이 내려가지 않는다.
            .then(
                if (!mark.blocksTouches) {
                    Modifier
                } else {
                    Modifier.pointerInput(spot) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var released = false
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                val up = event.changes.firstOrNull { it.changedToUpIgnoreConsumed() }
                                if (up != null) released = spot?.contains(down.position) == true && spot.contains(up.position)
                            } while (event.changes.any { it.pressed })
                            if (released) latestOnTargetClick()
                        }
                    }
                },
            ),
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val gutter = NyummyTheme.spacing.gutter
        var cardHeight by remember { mutableIntStateOf(0) }
        val tail = when {
            spot == null -> NyummyBubbleTail.None
            spot.center.y > maxHeightPx / 2 -> NyummyBubbleTail.Bottom
            else -> NyummyBubbleTail.Top
        }
        val gapPx = with(density) { CardGap.toPx() }
        val bottomPx = with(density) { WindowInsets.navigationBars.getBottom(this) + IntroBottomGap.toPx() }
        val cardTop = when (tail) {
            NyummyBubbleTail.Bottom -> spot!!.top - gapPx - cardHeight
            NyummyBubbleTail.Top -> spot!!.bottom + gapPx - with(density) { TutorialCardBodyTop.toPx() }
            else -> (anchorBounds?.top?.minus(gapPx) ?: (maxHeightPx - bottomPx)) - cardHeight
        }

        if (spot != null) {
            Image(
                painter = painterResource(R.drawable.nyummy_tutorial_paw_tap),
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        // 카드가 대상 아래에 붙으면(꼬리 위) 카드 윗변을 덜 가리게 조금 올려 대상 오른쪽 아래 모서리에 걸친다.
                        val lift = if (tail == NyummyBubbleTail.Top) PawTopLift.toPx() else 0f
                        val pawY = spot.bottom - PawInset.toPx() - lift
                        IntOffset((spot.right - PawInset.toPx()).roundToInt(), pawY.roundToInt())
                    }
                    .size(PawSize)
                    .graphicsLayer {
                        val scale = if (reducedMotion) 1f else press
                        scaleX = scale
                        scaleY = scale
                    },
            )
            // 화면 읽기에서는 비운 곳을 버튼 하나로 보이게 한다. 터치를 막지 않으면 대상이 그대로 읽힌다.
            if (mark.blocksTouches) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(spot.left.roundToInt(), spot.top.roundToInt()) }
                        .size(with(density) { spot.width.toDp() }, with(density) { spot.height.toDp() })
                        .clearAndSetSemantics {
                            contentDescription = mark.title
                            role = Role.Button
                            onClick { latestOnTargetClick(); true }
                        },
                )
            }
        }
        NyummyTutorialCard(
            title = mark.title,
            body = mark.body,
            expression = mark.expression,
            tail = tail,
            tailCenterX = spot?.let { with(density) { it.center.x.toDp() } - gutter },
            showHint = !mark.waitsForTarget,
            clickEnabled = !mark.waitsForTarget,
            onClick = mark.onAdvance,
            modifier = Modifier
                .offset { IntOffset(0, cardTop.roundToInt()) }
                .padding(horizontal = gutter)
                .fillMaxWidth()
                .onSizeChanged { cardHeight = it.height },
        )
        val onSkip = mark.onSkip
        if (onSkip != null) {
            var skipBounds by remember { mutableStateOf<Rect?>(null) }
            val statusBarTop = WindowInsets.statusBars.getTop(density)
            val overlapsTarget = spot != null && skipBounds?.overlaps(spot) == true
            NyummyTutorialSkipButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, statusBarTop + SkipTop.roundToPx()) }
                    .padding(end = gutter)
                    .onGloballyPositioned { skipBounds = it.boundsInParent() }
                    .graphicsLayer { alpha = if (overlapsTarget) 0f else 1f }
                    .then(if (overlapsTarget) Modifier.clearAndSetSemantics {} else Modifier),
                enabled = !overlapsTarget,
            )
        }
    }
}

/** 대상 둘레로 더 비우는 여백. */
private val SpotPadding = 8.dp
private val SpotMaxRadius = 20.dp
private val RingWidth = 3.dp
private val PulseGrow = 12.dp
private val CardGap = 4.dp
private val IntroBottomGap = 16.dp
private val PawSize = 48.dp

/** 발바닥이 비운 곳 오른쪽 아래 모서리 안으로 들어오는 깊이. */
private val PawInset = 26.dp

/** 카드가 대상 아래에 붙을 때 발바닥을 더 올리는 만큼. */
private val PawTopLift = 12.dp

/** 건너뛰기 터치 영역(48) 윗변의 상태바 아래 거리. 보이는 알약(32)은 상단 바 줄(상태바 아래 12)에 온다. */
private val SkipTop = 4.dp

private const val PulseAlpha = 0.6f
private const val PulseMillis = 1_400
private const val PawMillis = 600
private const val PawPressScale = 0.86f

/** 미리보기용. dp로 적은 영역을 지금 밀도의 px 영역으로 바꾼다. */
@Composable
private fun dpRect(x: Int, y: Int, width: Int, height: Int): Rect = with(LocalDensity.current) {
    Rect(x.dp.toPx(), y.dp.toPx(), (x + width).dp.toPx(), (y + height).dp.toPx())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkBottomTargetPreview() {
    NyummyTheme {
        CoachMarkContent(
            mark = CoachMark(
                title = "여기를 눌러 첫 기록을 해 보자!",
                body = "밥을 먹을 때마다 사진 한 장이면 기록 끝이야",
                expression = NyummyTutorialExpression.Point,
                target = CoachMarkKey("camera"),
                onSkip = {},
            ),
            targetBounds = dpRect(165, 748, 60, 60),
            anchorBounds = null,
            onTargetClick = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkTopRightTargetPreview() {
    NyummyTheme {
        CoachMarkContent(
            mark = CoachMark(
                title = "보상 상자가 도착했어!",
                body = "우편함에서 같이 열어 보자",
                expression = NyummyTutorialExpression.Surprise,
                target = CoachMarkKey("mail"),
                onSkip = {},
            ),
            targetBounds = dpRect(282, 56, 40, 40),
            anchorBounds = null,
            onTargetClick = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkIntroPreview() {
    NyummyTheme {
        CoachMarkContent(
            mark = CoachMark(
                title = "안녕, 집사! 나는 냐미야",
                body = "집사가 밥을 기록하면 나도 같이 먹을 수 있어",
                expression = NyummyTutorialExpression.Greet,
                onSkip = {},
            ),
            targetBounds = null,
            anchorBounds = null,
            onTargetClick = {},
        )
    }
}
