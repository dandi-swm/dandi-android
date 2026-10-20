package com.dandi.nyummy.mailbox.presentation.list

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.reward.RewardBurstFlash
import com.dandi.nyummy.common.presentation.reward.RewardConfetti
import com.dandi.nyummy.common.presentation.reward.RewardGlow
import com.dandi.nyummy.common.presentation.reward.RewardHeadline
import com.dandi.nyummy.common.presentation.reward.RewardLightRays
import com.dandi.nyummy.common.presentation.reward.rememberReducedMotion
import com.dandi.nyummy.mailbox.presentation.R
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 우편함에서 보상 상자를 열 때 화면 전체를 덮는 연출. 다이얼로그 창으로 띄워 상태 바와 내비게이션 바 뒤까지 무대를 깐다.
 *
 * 뒤로 가기는 상자가 열리는 중이면 받은 보상으로 건너뛰고, 보상을 보여 주는 중이면 닫는다.
 * 응답을 기다리거나 받지 못해 무대가 걷히는 중에는 막는다.
 */
@Composable
internal fun RewardBoxOpeningDialog(
    opening: BoxOpeningUiState,
    onIntent: (MailboxIntent) -> Unit,
) {
    var closing by remember { mutableStateOf(false) }
    val latestPhase by rememberUpdatedState(opening.phase)
    Dialog(
        onDismissRequest = {
            when (latestPhase) {
                BoxOpeningPhase.Opening -> onIntent(MailboxIntent.SkipBoxOpening)
                BoxOpeningPhase.Revealed -> closing = true
                BoxOpeningPhase.Waiting, BoxOpeningPhase.Failed -> Unit
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        StageWindowEffect()
        RewardBoxOpening(
            opening = opening,
            closing = closing,
            onSkip = { onIntent(MailboxIntent.SkipBoxOpening) },
            onBurstFinished = { onIntent(MailboxIntent.BoxOpeningBurstFinished) },
            onFailureShown = { onIntent(MailboxIntent.BoxOpeningFailureShown) },
            onContinue = { closing = true },
            onClosed = { onIntent(MailboxIntent.CloseBoxOpening) },
        )
    }
}

/**
 * 다이얼로그 창을 상태 바와 내비게이션 바 뒤까지 화면 전체로 펴고, 창 자체의 딤과 열림 애니메이션을 끄고,
 * 어두운 무대 위 시스템 바 아이콘을 밝게 한다.
 */
@Composable
private fun StageWindowEffect() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 바 뒤 배경을 창이 직접 그린다고 알려야 시스템이 상태 바와 내비게이션 바 자리를 검게 칠하지 않는다.
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.attributes = window.attributes.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.setDimAmount(0f)
        window.setWindowAnimations(0)
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

/**
 * 상자 열기 장면.
 *
 * 1. 기다림: 진한 그린 무대가 깔리고 닫힌 상자가 빛무리와 도는 빛줄기 앞에 통통 튀어나와 응답을 기다린다.
 * 2. 열림: 응답이 오면 점점 세게 세 번 흔들리고, 화면이 번쩍이며 열린 상자로 바뀌어 펑 커졌다 돌아오고 색종이가 터진다.
 * 3. 공개: 상자가 작아지며 아래로 가고, 받은 보상이 상자에서 솟아오르며, 위에 제목과 우편 제목이 내려온다.
 * 받지 못하면 상자가 고개를 젓듯 좌우로 흔들리고 무대가 걷힌다.
 *
 * 닫힌 상자와 열린 상자 그림은 처음부터 함께 불러 둬 바뀌는 순간 비지 않게 한다. 그림을 못 불러와도 장면은 그대로 흘러간다.
 */
@Composable
internal fun RewardBoxOpening(
    opening: BoxOpeningUiState,
    closing: Boolean,
    onSkip: () -> Unit,
    onBurstFinished: () -> Unit,
    onFailureShown: () -> Unit,
    onContinue: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    val latestPhase by rememberUpdatedState(opening.phase)
    val latestOnSkip by rememberUpdatedState(onSkip)
    val latestOnBurstFinished by rememberUpdatedState(onBurstFinished)
    val latestOnFailureShown by rememberUpdatedState(onFailureShown)
    val latestOnClosed by rememberUpdatedState(onClosed)

    val closedPainter = rememberAsyncImagePainter(rememberBoxRequest(opening.closedImageUrl))
    val openedPainter = rememberAsyncImagePainter(rememberBoxRequest(opening.openedImageUrl))

    val stage = remember { Animatable(0f) }
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val tilt = remember { Animatable(0f) }
    val refuse = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val rays = remember { Animatable(RaysWaitScale) }
    val reveal = remember { Animatable(0f) }
    val rewardsIn = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    var opened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { stage.animateTo(1f, tween(StageEnterMillis)) }
        if (!reducedMotion) {
            appear.animateTo(1f, spring(dampingRatio = AppearDamping, stiffness = Spring.StiffnessMediumLow))
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        val answer = snapshotFlow { latestPhase }.first { it != BoxOpeningPhase.Waiting }
        if (answer == BoxOpeningPhase.Failed) {
            if (!reducedMotion) refuse.animateTo(0f, refuseSpec())
            launch { appear.animateTo(0f, tween(StageLeaveMillis)) }
            stage.animateTo(0f, tween(StageLeaveMillis))
            latestOnFailureShown()
            return@LaunchedEffect
        }
        if (reducedMotion) {
            opened = true
            latestOnBurstFinished()
        } else {
            val burst = launch {
                repeat(ShakeCount) { index ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tilt.animateTo(0f, shakeSpec(ShakeDegrees + index * ShakeDegreesStep))
                    delay(ShakeGapMillis)
                }
                // 터짐: 화면이 번쩍이는 순간 열린 상자로 바뀌고, 펑 커졌다 돌아오며 색종이가 터진다.
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                opened = true
                launch {
                    flash.snapTo(1f)
                    flash.animateTo(0f, tween(FlashMillis, easing = FastOutSlowInEasing))
                }
                launch {
                    confetti.snapTo(0f)
                    confetti.animateTo(1f, tween(ConfettiMillis, easing = FastOutSlowInEasing))
                }
                launch { rays.animateTo(RaysBurstScale, spring(dampingRatio = RaysDamping, stiffness = Spring.StiffnessLow)) }
                pop.snapTo(PopScale)
                pop.animateTo(1f, spring(dampingRatio = PopDamping, stiffness = Spring.StiffnessMedium))
                delay(BurstHoldMillis)
                latestOnBurstFinished()
            }
            val skip = launch {
                snapshotFlow { latestPhase }.first { it == BoxOpeningPhase.Revealed }
                burst.cancel()
            }
            burst.join()
            skip.cancel()
            // 건너뛰었으면 흔들림, 섬광, 색종이가 멈춘 자리를 정리한다.
            opened = true
            tilt.snapTo(0f)
            pop.snapTo(1f)
            flash.snapTo(0f)
            confetti.snapTo(0f)
        }
        snapshotFlow { latestPhase }.first { it == BoxOpeningPhase.Revealed }
        if (reducedMotion) {
            rays.snapTo(RaysRevealScale)
            rewardsIn.snapTo(1f)
            reveal.snapTo(1f)
        } else {
            launch { rays.animateTo(RaysRevealScale, tween(RevealMillis, easing = FastOutSlowInEasing)) }
            launch {
                delay(RewardsDelayMillis)
                rewardsIn.animateTo(1f, tween(RewardsInMillis, easing = LinearEasing))
            }
            launch {
                delay(RewardsDelayMillis)
                confetti.snapTo(0f)
                confetti.animateTo(1f, tween(RevealConfettiMillis, easing = FastOutSlowInEasing))
            }
            reveal.animateTo(1f, tween(RevealMillis, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        leave.animateTo(1f, tween(StageLeaveMillis))
        latestOnClosed()
    }

    val idle = rememberInfiniteTransition(label = "RewardBoxIdle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BobMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rewardBoxBob",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (latestPhase == BoxOpeningPhase.Opening) latestOnSkip()
                }
            },
    ) {
        val density = LocalDensity.current
        val topInset = with(density) { WindowInsets.systemBars.getTop(this).toDp() }
        val bottomInset = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
        val layout = remember(maxWidth, maxHeight, topInset, bottomInset, opening.rewards.size) {
            BoxOpeningLayout.of(maxHeight, topInset, bottomInset, opening.rewards.size)
        }
        val centerX = maxWidth / 2

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = stage.value * StageAlpha * (1f - leave.value) }
                .background(NyummyTheme.colors.bg.sceneRewardStage),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - leave.value },
        ) {
            RewardLightRays(
                modifier = Modifier
                    .size(RaysBaseSize)
                    .offset {
                        val y = lerp(layout.waitBoxCenterY, layout.raysRevealCenterY, reveal.value)
                        IntOffset((centerX - RaysBaseSize / 2).roundToPx(), (y - RaysBaseSize / 2).roundToPx())
                    },
                alpha = { appear.value * stage.value },
                scale = { rays.value },
            )
            RewardConfetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize())

            // 상자: 빛무리 위에 그리고, 공개 때 작아지며 보상 아래 자리로 내려간다.
            Box(
                modifier = Modifier
                    .size(GlowSize)
                    .offset {
                        val y = lerp(layout.waitBoxCenterY, layout.revealBoxCenterY, reveal.value)
                        IntOffset((centerX - GlowSize / 2).roundToPx() + refuse.value.dp.roundToPx(), (y - GlowSize / 2).roundToPx())
                    }
                    .graphicsLayer {
                        val shrink = lerp(1f, RevealBoxScale, reveal.value)
                        val scale = appear.value * pop.value * shrink
                        scaleX = scale
                        scaleY = scale
                        translationY = -BobLift.toPx() * bob * appear.value * (1f - reveal.value)
                    },
                contentAlignment = Alignment.Center,
            ) {
                RewardGlow(
                    modifier = Modifier.fillMaxSize(),
                    alpha = { if (opened) OpenedGlowAlpha else 1f },
                )
                Box(
                    modifier = Modifier
                        .size(BoxSize)
                        .graphicsLayer {
                            rotationZ = tilt.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                ) {
                    Image(
                        painter = if (opened) openedPainter else closedPainter,
                        contentDescription = stringResource(R.string.mailbox_box_content_description),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            BoxRewards(
                rewards = opening.rewards,
                layout = layout,
                progress = { rewardsIn.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, layout.rewardsTop.roundToPx()) },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = layout.titleTop, start = NyummyTheme.spacing.gutter, end = NyummyTheme.spacing.gutter)
                    .graphicsLayer {
                        val t = ((reveal.value - TitleStart) / (1f - TitleStart)).coerceIn(0f, 1f)
                        alpha = t
                        translationY = -TitleDrop.toPx() * (1f - t)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                RewardHeadline(text = stringResource(R.string.mailbox_box_open_title))
                if (opening.mailTitle.isNotBlank()) {
                    NyummyText(
                        text = opening.mailTitle,
                        style = NyummyTheme.typography.labelS,
                        color = NyummyTheme.colors.content.brand,
                        maxLines = 1,
                        modifier = Modifier
                            .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                            .padding(horizontal = NyummyTheme.spacing.s12, vertical = NyummyTheme.spacing.s4),
                    )
                }
            }

            if (opening.phase == BoxOpeningPhase.Revealed) {
                NyummyButton(
                    text = stringResource(R.string.mailbox_box_open_continue),
                    onClick = onContinue,
                    enabled = !closing,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = NyummyTheme.spacing.gutter,
                            end = NyummyTheme.spacing.gutter,
                            bottom = bottomInset + CtaBottomGap,
                        )
                        .widthIn(max = ContentMaxWidth)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = ((reveal.value - CtaStart) / (1f - CtaStart)).coerceIn(0f, 1f)
                        },
                )
            }
        }
        RewardBurstFlash(
            progress = { flash.value },
            center = { with(density) { Offset(centerX.toPx(), layout.waitBoxCenterY.toPx()) } },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 상자에서 나온 보상. 하나면 크게, 여럿이면 한 줄에 셋씩 놓는다.
 * 저마다 조금씩 늦게 상자 자리에서 솟아올라 제자리에 커지며 선다.
 */
@Composable
private fun BoxRewards(
    rewards: List<BoxRewardUiModel>,
    layout: BoxOpeningLayout,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    if (rewards.isEmpty()) return
    val single = rewards.size == 1
    val rows = rewards.chunked(RewardsPerRow)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RewardRowGap),
    ) {
        var index = 0
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(RewardCellGap)) {
                row.forEach { reward ->
                    val order = index++
                    RewardCell(
                        reward = reward,
                        single = single,
                        modifier = Modifier.graphicsLayer {
                            val t = staggered(progress(), order, rewards.size)
                            val rise = RiseEasing.transform(t)
                            val grow = GrowEasing.transform(t)
                            translationY = layout.riseDistance.toPx() * (1f - rise)
                            val scale = lerp(RewardStartScale, 1f, grow)
                            scaleX = scale
                            scaleY = scale
                            alpha = (t * RewardFadeSpeed).coerceAtMost(1f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardCell(
    reward: BoxRewardUiModel,
    single: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconSize = if (single) SingleRewardIcon else RewardIcon
    Column(
        modifier = modifier.width(if (single) SingleRewardWidth else RewardCellWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (single) NyummyTheme.spacing.s8 else RewardLabelGap),
    ) {
        val coinFallback = painterResource(CommonR.drawable.nyummy_asset_coin)
        AsyncImage(
            model = reward.imageUrl.ifEmpty { null },
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            placeholder = if (reward is BoxRewardUiModel.Coin) coinFallback else null,
            error = if (reward is BoxRewardUiModel.Coin) coinFallback else null,
            fallback = if (reward is BoxRewardUiModel.Coin) coinFallback else null,
        )
        val label = when (reward) {
            is BoxRewardUiModel.Coin -> stringResource(R.string.mailbox_box_reward_coin, reward.quantity)
            is BoxRewardUiModel.Item -> if (reward.quantity > 1) {
                stringResource(R.string.mailbox_box_reward_item_count, reward.name, reward.quantity)
            } else {
                reward.name
            }
        }
        if (single) {
            RewardHeadline(
                text = label,
                style = if (reward is BoxRewardUiModel.Coin) NyummyTheme.typography.displayL else NyummyTheme.typography.titleL,
            )
        } else {
            RewardHeadline(text = label, style = NyummyTheme.typography.labelL, stroke = SmallLabelStroke)
        }
    }
}

/** 보상을 하나씩 조금 늦게 솟아오르게 한다. 전체 진행 [progress]에서 [index]번째 보상의 진행(0~1)을 구한다. */
private fun staggered(progress: Float, index: Int, count: Int): Float {
    if (count <= 1) return progress
    val span = 1f - RewardStagger * (count - 1)
    return ((progress - RewardStagger * index) / span).coerceIn(0f, 1f)
}

/** 상자 그림 요청. 그려지기 전에도 바로 불러오도록 원본 크기로 요청한다. 주소가 없으면 아무것도 불러오지 않는다. */
@Composable
private fun rememberBoxRequest(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(context, url) {
        ImageRequest.Builder(context)
            .data(url.ifEmpty { null })
            .size(Size.ORIGINAL)
            .build()
    }
}

/** 좌우로 한 번 흔들고 제자리로 온다. */
private fun shakeSpec(degrees: Float) = keyframes {
    durationMillis = ShakeMillis
    -degrees at ShakeMillis / 4 using LinearEasing
    degrees at ShakeMillis * 3 / 4 using LinearEasing
    0f at ShakeMillis
}

/** 고개를 젓듯 좌우로 두 번 흔들고 제자리로 온다(dp). */
private fun refuseSpec() = keyframes {
    durationMillis = RefuseMillis
    -RefuseDistance at RefuseMillis / 6
    RefuseDistance at RefuseMillis * 2 / 6
    -RefuseDistance * 0.6f at RefuseMillis * 3 / 6
    RefuseDistance * 0.6f at RefuseMillis * 4 / 6
    0f at RefuseMillis
}

/**
 * 장면의 세로 자리(dp). 기다릴 때 상자는 화면 가운데 조금 위에 있고, 공개 때는 제목 아래부터 계속하기 버튼 위까지의
 * 빈자리 가운데에 보상과 작아진 상자를 위아래로 묶어 둔다.
 */
private class BoxOpeningLayout(
    val waitBoxCenterY: Dp,
    val titleTop: Dp,
    val rewardsTop: Dp,
    val revealBoxCenterY: Dp,
    val raysRevealCenterY: Dp,
    val riseDistance: Dp,
) {
    companion object {
        fun of(height: Dp, topInset: Dp, bottomInset: Dp, rewardCount: Int): BoxOpeningLayout {
            val titleTop = topInset + TitleTopGap
            val areaTop = titleTop + TitleBlockHeight + ClusterMargin
            val areaBottom = height - bottomInset - CtaBottomGap - CtaHeight - ClusterMargin
            val rewardsHeight = when {
                rewardCount <= 0 -> 0.dp
                rewardCount == 1 -> SingleRewardIcon + SingleLabelGap + SingleLabelHeight
                else -> {
                    val rows = ceil(rewardCount / RewardsPerRow.toFloat()).toInt()
                    (RewardIcon + RewardLabelGap + RewardLabelHeight) * rows + RewardRowGap * (rows - 1)
                }
            }
            val revealBoxHeight = BoxSize * RevealBoxScale
            val gap = if (rewardCount > 0) ClusterGap else 0.dp
            val clusterHeight = rewardsHeight + gap + revealBoxHeight
            val clusterTop = areaTop + ((areaBottom - areaTop - clusterHeight) / 2).coerceAtLeast(0.dp)
            val revealBoxCenterY = clusterTop + rewardsHeight + gap + revealBoxHeight / 2
            return BoxOpeningLayout(
                waitBoxCenterY = height * WaitCenterFraction,
                titleTop = titleTop,
                rewardsTop = clusterTop,
                revealBoxCenterY = revealBoxCenterY,
                raysRevealCenterY = clusterTop + clusterHeight / 2,
                riseDistance = revealBoxCenterY - (clusterTop + rewardsHeight / 2),
            )
        }
    }
}

private val BoxSize = 200.dp
private val GlowSize = 300.dp
private val RaysBaseSize = 420.dp
private val BobLift = 6.dp
private val TitleTopGap = 56.dp
private val TitleBlockHeight = 76.dp
private val TitleDrop = 12.dp
private val ClusterMargin = 24.dp
private val ClusterGap = 8.dp
private val CtaHeight = 56.dp
private val CtaBottomGap = 16.dp
private val ContentMaxWidth = 480.dp
private val SingleRewardIcon = 96.dp
private val SingleRewardWidth = 240.dp
private val SingleLabelGap = 8.dp
private val SingleLabelHeight = 40.dp
private val RewardIcon = 72.dp
private val RewardCellWidth = 96.dp
private val RewardCellGap = 12.dp
private val RewardRowGap = 12.dp
private val RewardLabelGap = 6.dp
private val RewardLabelHeight = 24.dp
private val SmallLabelStroke = 4.dp

private const val RewardsPerRow = 3
private const val StageAlpha = 0.94f
private const val StageEnterMillis = 220
private const val StageLeaveMillis = 260
private const val AppearDamping = 0.5f
private const val ShakeCount = 3
private const val ShakeMillis = 280
private const val ShakeGapMillis = 120L
private const val ShakeDegrees = 6f
private const val ShakeDegreesStep = 3f
private const val RefuseMillis = 480
private const val RefuseDistance = 12f
private const val PopScale = 1.28f
private const val PopDamping = 0.45f
private const val BurstHoldMillis = 420L
private const val FlashMillis = 520
private const val ConfettiMillis = 1_000
private const val RevealConfettiMillis = 1_400
private const val RaysWaitScale = 0.6f
private const val RaysBurstScale = 1.2f
private const val RaysRevealScale = 1.45f
private const val RaysDamping = 0.6f
private const val RevealMillis = 520
private const val RevealBoxScale = 0.72f
private const val RewardsDelayMillis = 160L
private const val RewardsInMillis = 720
private const val RewardStagger = 0.15f
private const val RewardStartScale = 0.3f
private const val RewardFadeSpeed = 3f
private const val TitleStart = 0.3f
private const val CtaStart = 0.6f
private const val OpenedGlowAlpha = 0.55f
private const val BobMillis = 900
private const val WaitCenterFraction = 0.47f

/** 솟아오를 때 조금 지나쳤다 돌아오는 느낌. */
private val GrowEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val RiseEasing = FastOutSlowInEasing
