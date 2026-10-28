package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBubbleTail
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyChip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyVoiceBubble
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.RoomBackgroundImage
import com.dandi.nyummy.common.presentation.room.RoomCardLayout
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.meal.presentation.MealCeremonyStage
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.meal.presentation.mealPhotoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 식사 기록 연출 화면입니다. 사진을 제출한 순간부터 끝날 때까지 한 화면에서 이어집니다.
 *
 * 1. 기다림: 고양이방 카드(지금 마이룸에 깔린 배경) 안에서 숟가락을 든 냐미가 침을 꼴깍 삼키고, 식탁 위에 원본 사진 카드가 놓입니다.
 *    사진은 어떤 장면에서도 변형하지 않습니다.
 * 2. 응답(기록 접수)이 오면 띠욧! 컷인: 오물오물 → 정적 → 섬광, 흔들림, 빛줄기와 함께 별눈.
 *    분석 완료가 아니라 접수에 맞춰 넘어갑니다. 영양 분석은 접수 뒤에 진행됩니다.
 * 3. "냐미가 맛있게 먹었어요!". [sendsRewardBox] 가 아니면 여기서 끝납니다.
 * 4. 상자를 보내면 진한 그린 무대 위에 상자가 등장해 등급만큼 승급하고, 무대가 걷히며 오른쪽 위 우편 아이콘으로 날아갑니다.
 *
 * 어느 장면인지([stage])는 ViewModel 이 정합니다. 이 화면은 그 장면의 움직임만 재생하고, 끝나면 [onStageFinished] 로
 * 알립니다. 화면이 다시 만들어지면 지금 장면만 처음부터 다시 재생하고, 지난 장면의 결과(올라온 사진 카드,
 * 우편함에 든 상자)는 끝난 모습으로 둡니다.
 *
 * 띠욧 컷신은 오른쪽 위 건너뛰기 칩([onSkipClick])으로 건너뜁니다. 컷신만 건너뛰고 먹었어요와 상자 연출은 그대로
 * 보여 줍니다. 기기에서 애니메이션을 끄면 섬광, 흔들림, 컷인 없이 장면만 바꿉니다.
 *
 * @param rewardBoxGrade 접수된 기록의 보상 상자 등급. 접수 전이면 [RewardBoxGrade.NONE] 이다.
 * @param sendsRewardBox 상자를 우편함으로 보내는 장면까지 보여 줄지
 */
@Composable
internal fun MealCeremony(
    photoPath: String,
    stage: MealCeremonyStage,
    rewardBoxGrade: RewardBoxGrade,
    sendsRewardBox: Boolean,
    roomBackgroundUrl: String?,
    onStageFinished: (MealCeremonyStage) -> Unit,
    onSkipClick: () -> Unit,
    onCloseClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val latestOnStageFinished by rememberUpdatedState(onStageFinished)

    // 처음 그릴 때 이미 지난 장면이면(화면이 다시 만들어짐) 지난 장면의 결과는 끝난 모습으로 둔다.
    // 사진 카드는 올라와 있고, 상자가 날아가던 중이었으면 이미 우편함에 들어간 것으로 본다.
    var giftDelivered by remember { mutableStateOf(stage == MealCeremonyStage.BoxSend) }
    var boxCenter by remember { mutableStateOf(Offset.Zero) }
    var mailCenter by remember { mutableStateOf(Offset.Zero) }
    // 상자와 우편 아이콘 자리는 창 기준으로 잰다. 이 화면이 창 맨 위에서 시작하지 않으면(시스템 바를 피해 놓이는 등)
    // 그만큼 빼야 이 화면 안에 그리는 날아가는 상자가 우편 아이콘에 닿는다.
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    val serve = remember { Animatable(if (reducedMotion || stage != MealCeremonyStage.Serving) 1f else 0f) }
    val flash = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val mailGulp = remember { Animatable(1f) }

    // 장면마다 그 장면의 움직임만 재생하고, 끝나면 알린다. 다음 장면은 ViewModel 이 정한다.
    LaunchedEffect(stage) {
        when (stage) {
            // 1. 사진 카드가 식탁 위로 올라온다.
            MealCeremonyStage.Serving -> {
                if (!reducedMotion) serve.animateTo(1f, spring(dampingRatio = ServeDamping, stiffness = Spring.StiffnessLow))
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // 응답이 올 때까지 두근두근. 접수되면 ViewModel 이 띠욧으로 넘긴다.
            MealCeremonyStage.Waiting -> return@LaunchedEffect

            // 2. 띠욧! 컷인. 터지는 순간의 섬광과 흔들림은 컷인이 onPop 으로 알려 준다. 애니메이션을 끄면 곧장 넘어간다.
            MealCeremonyStage.Reaction -> if (!reducedMotion) delay(TasteReactionTotalMillis)

            // 3. 냐미가 맛있게 먹었어요. 띠욧을 건너뛰었으면 섬광과 흔들림이 남지 않게 걷는다.
            MealCeremonyStage.Ate -> {
                flash.snapTo(0f)
                shake.snapTo(0f)
                delay(AteMillis)
            }

            // 4. 상자 등장과 승급 → 우편함에 쏙 → 곧장 홈으로. 따로 끝 화면을 두지 않는다.
            MealCeremonyStage.BoxReveal -> delay(if (reducedMotion) ReducedRevealMillis else rewardBoxGrade.stageMillis())

            MealCeremonyStage.BoxSend -> {
                if (reducedMotion) giftDelivered = true
                snapshotFlow { giftDelivered }.first { it }
                delay(MailGulpMillis + SentHoldMillis)
            }

            // 끝 장면. 완료 버튼을 기다린다.
            MealCeremonyStage.Finished -> return@LaunchedEffect
        }
        latestOnStageFinished(stage)
    }

    // 상자가 닿으면 우편 아이콘이 꿀꺽 삼키듯 커졌다 돌아온다.
    LaunchedEffect(giftDelivered) {
        if (giftDelivered && !reducedMotion) {
            mailGulp.snapTo(MailGulpScale)
            mailGulp.animateTo(1f, spring(dampingRatio = MailGulpDamping, stiffness = Spring.StiffnessMedium))
        }
    }

    val shakePx = with(density) { ShakeDistance.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 빛줄기, 상자 무대의 빛, 색종이처럼 자기 영역보다 크게 그리는 효과가 상태 바와 내비게이션 바 위로 새지 않게 자른다.
            .clipToBounds()
            .background(NyummyTheme.colors.bg.surface)
            .graphicsLayer { translationX = shake.value * shakePx }
            .onGloballyPositioned { originInRoot = it.positionInRoot() },
    ) {
        when (stage) {
            MealCeremonyStage.Serving, MealCeremonyStage.Waiting, MealCeremonyStage.Reaction -> {
                // 홈처럼 방이 화면 전체(시스템 바 뒤까지)를 채우고, 닫기와 제목은 방 위에 뜬다.
                WaitingScene(
                    photoPath = photoPath,
                    roomBackgroundUrl = roomBackgroundUrl,
                    serveProgress = serve.value,
                    animate = !reducedMotion,
                    modifier = Modifier.fillMaxSize(),
                )
                CeremonyHeader(onCloseClick = onCloseClick, modifier = Modifier.statusBarsPadding())
            }

            // 먹었어요부터는 화면 전체를 덮는 장면이 그린다.
            else -> Unit
        }

        // 먹었어요: 띠욧 배경이 차분해지며 이어진다. 상자를 보낼 때는 상자 무대가 이 위를 덮고,
        // 상자가 우편 아이콘에 쏙 들어가면 제목만 "우편함에 쏙!"으로 바뀐 뒤 홈으로 간다.
        val showsAte = stage == MealCeremonyStage.Ate || stage == MealCeremonyStage.BoxReveal ||
            stage == MealCeremonyStage.BoxSend || stage == MealCeremonyStage.Finished
        if (showsAte) {
            AteCelebration(
                title = if (giftDelivered) R.string.meal_ceremony_box_sent else R.string.meal_record_done_title,
                showLine = stage == MealCeremonyStage.Ate,
                showDone = stage == MealCeremonyStage.Finished,
                animate = !reducedMotion,
                onDoneClick = onDoneClick,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 애니메이션을 끄면 띠욧 장면은 곧장 지나가므로 그리지 않는다.
        if (stage == MealCeremonyStage.Reaction && !reducedMotion) {
            // 만화 페이지 바탕은 시스템 바 뒤까지 깔고, 컷은 시스템 바를 피해 놓는다.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(NyummyTheme.colors.bg.sceneDark),
            )
            TasteReactionCutIn(
                art = MealCeremonyArt.tasting,
                onPop = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        // 섬광은 한 번만, 불투명도를 70%로 묶어 눈부시지 않게 한다.
                        flash.snapTo(FlashAlpha)
                        flash.animateTo(0f, tween(FlashMillis))
                    }
                    scope.launch { shake.animateTo(0f, shakeSpec()) }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            )
        }

        if (sendsRewardBox && (stage == MealCeremonyStage.BoxReveal || stage == MealCeremonyStage.BoxSend)) {
            RewardBoxStage(
                grade = rewardBoxGrade,
                leaving = stage == MealCeremonyStage.BoxSend,
                reducedMotion = reducedMotion,
                onBoxPositioned = { if (stage == MealCeremonyStage.BoxReveal) boxCenter = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 우편 아이콘: 홈 상단 우편 버튼과 같은 쪽(오른쪽 위)에 뜨고, 상자가 닿으면 꿀꺽 삼키듯 커졌다 돌아온다.
        if (sendsRewardBox) {
            MailTarget(
                visible = stage == MealCeremonyStage.BoxSend,
                showDot = giftDelivered,
                gulp = mailGulp.value,
                onPositioned = { mailCenter = it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
            )
        }
        if (sendsRewardBox && stage == MealCeremonyStage.BoxSend && !giftDelivered && !reducedMotion) {
            GiftFlight(
                grade = rewardBoxGrade,
                from = boxCenter - originInRoot,
                to = mailCenter - originInRoot,
                onArrived = {
                    giftDelivered = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        }

        // 건너뛰기: 띠욧 컷신 동안 상단 바 오른쪽에 둔다.
        if (stage.isSkippable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8)
                    .height(NyummyTheme.size.touchTarget),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(
                    text = stringResource(R.string.meal_ceremony_skip_label),
                    style = NyummyTheme.typography.labelM,
                    color = NyummyTheme.colors.content.secondary,
                    modifier = Modifier
                        .nyummyClickable(onClick = onSkipClick)
                        .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                        .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
                )
            }
        }

        val flashColor = NyummyTheme.colors.bg.surface
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = flash.value }
                .background(flashColor),
        )
    }
}

/** 상단 바: 닫기와 제목. */
@Composable
private fun CeremonyHeader(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
    ) {
        NyummyIconButton(
            icon = CommonR.drawable.nyummy_ic_x,
            contentDescription = stringResource(R.string.meal_record_close_content_description),
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.CenterStart),
            style = NyummyIconButtonStyle.Ghost,
        )
        NyummyText(
            text = stringResource(R.string.meal_record_title),
            modifier = Modifier.align(Alignment.Center),
            color = NyummyTheme.colors.content.primary,
            style = NyummyTheme.typography.titleM,
        )
    }
}

/**
 * 상자가 날아 들어갈 우편 아이콘. 자리는 늘 잡아 두고([onPositioned]) 보일 때만 그린다.
 * 상자가 닿으면 [showDot] 으로 빨간 점을 단다.
 */
@Composable
private fun MailTarget(
    visible: Boolean,
    showDot: Boolean,
    gulp: Float,
    onPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Box(
        modifier = modifier
            .size(MailTargetSize)
            .onGloballyPositioned { onPositioned(it.boundsInRoot().center) }
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
                scaleX = gulp
                scaleY = gulp
            }
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_mail),
            contentDescription = if (visible) stringResource(R.string.meal_ceremony_mailbox_content_description) else null,
            tint = NyummyTheme.colors.content.primary,
            modifier = Modifier.size(NyummyTheme.size.iconL),
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -MailDotInset, y = MailDotInset)
                    .size(MailDotSize)
                    .background(NyummyTheme.colors.content.danger, CircleShape)
                    .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.bg.surface, CircleShape),
            )
        }
    }
}

/**
 * 기다리는 장면: 지금 마이룸에 깔린 방이 홈처럼 화면 전체를 채우고, 숟가락을 든 냐미가 식탁 뒤에 앉아 있으며,
 * 원본 사진 카드가 식탁 위로 비스듬히 올라옵니다. 냐미 머리 위 말풍선이 "맛보는 중이야"라고 말합니다.
 */
@Composable
private fun WaitingScene(
    photoPath: String,
    roomBackgroundUrl: String?,
    serveProgress: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val motion = rememberInfiniteTransition(label = "WaitingMotion")
    val bob by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BobMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "waitingBob",
    )
    val twinkle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TwinkleMillis, easing = LinearEasing), RepeatMode.Reverse),
        label = "waitingTwinkle",
    )

    BoxWithConstraints(modifier = modifier) {
        // 지금 마이룸에 깔린 배경을 홈과 같은 규칙(화면을 덮고 바닥을 맞춤)으로 화면 전체에 깐다.
        // 배경이 없거나, 받는 중이거나, 받지 못하면 앱에 든 기본 빈 방이 보인다.
        val room = RoomCardLayout.roomRect(maxWidth, maxHeight)
        RoomBackgroundImage(
            imageUrl = roomBackgroundUrl,
            roomRect = with(density) { Rect(room.left.toPx(), room.top.toPx(), room.right.toPx(), room.bottom.toPx()) },
        )
        val tablePainter = painterResource(MealCeremonyArt.tableFront)
        val tableImageAspect = tablePainter.intrinsicSize.width / tablePainter.intrinsicSize.height
        // 냐미, 식탁, 사진은 시스템 바와 위에 뜬 닫기 줄을 피한 자리에 놓는다. 식탁은 화면 폭을 다 쓴다.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(
                    top = NyummyTheme.size.touchTarget + NyummyTheme.spacing.s8 * 3,
                    bottom = NyummyTheme.spacing.s24,
                ),
        ) {
            val sceneWidth = maxWidth
            val contentWidth = maxWidth - NyummyTheme.spacing.s20 * 2
            val cardHeight = maxHeight * CardHeightFraction
            val photoWidth = contentWidth * PhotoWidthFraction
            val catWidth = contentWidth * CatWidthFraction
            // 냐미는 식탁 뒤에 앉아 몸 아래쪽이 식탁에 가린다.
            val tableFrontHeight = sceneWidth / MealCeremonyArt.TableFrontAspect
            val catBottom = tableFrontHeight * CatBottomOnTable
            val waitingArt = MealCeremonyArt.waiting
            val catHeight = catWidth * (waitingArt.cellHeight.toFloat() / waitingArt.cellWidth)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(cardHeight),
            ) {
                CeremonyArtView(
                    art = waitingArt,
                    width = catWidth,
                    contentDescription = stringResource(R.string.meal_record_cat_content_description),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = catBottom)
                        .graphicsLayer {
                            translationY = if (animate) -BobLift.toPx() * bob else 0f
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                )
                if (animate) {
                    val sparkle = painterResource(R.drawable.ic_meal_sparkle)
                    val tint = NyummyTheme.colors.data.coin
                    Icon(
                        painter = sparkle,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = -SparkleSpread, y = -SparkleRise)
                            .size(SparkleSize)
                            .graphicsLayer {
                                alpha = twinkle
                                scaleX = 0.6f + 0.4f * twinkle
                                scaleY = 0.6f + 0.4f * twinkle
                            },
                    )
                    Icon(
                        painter = sparkle,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = SparkleSpread, y = -SparkleRise / 2)
                            .size(SparkleSize)
                            .graphicsLayer {
                                alpha = 1f - twinkle
                                scaleX = 1f - 0.4f * twinkle
                                scaleY = 1f - 0.4f * twinkle
                            },
                    )
                }
                // 식탁은 윗선부터 화면 아래 끝까지 덮어서, 아래로 방 바닥이 보이며 떠 보이지 않게 한다.
                Image(
                    painter = tablePainter,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = cardHeight - tableFrontHeight * (1f + TableLiftOverCat))
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .height(sceneWidth / tableImageAspect),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                )
                // 말풍선은 꼬리가 냐미 머리 바로 위에 오게 놓고, 냐미와 함께 들썩인다.
                NyummyVoiceBubble(
                    text = stringResource(R.string.meal_ceremony_waiting),
                    tail = NyummyBubbleTail.Bottom,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = catBottom + catHeight * (1f - WaitingHeadTop) + BubbleGap)
                        .graphicsLayer { translationY = if (animate) -BobLift.toPx() * bob else 0f }
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            // 원본 사진 카드. 식탁 아래 끝에 걸쳐 비스듬히 놓인다. 사진은 그대로 보여 준다.
            val serveDistancePx = with(density) { ServeDistance.toPx() }
            val photoShape = RoundedCornerShape(NyummyTheme.radius.m)
            AsyncImage(
                model = mealPhotoModel(photoPath),
                contentDescription = stringResource(R.string.meal_record_captured_photo_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(photoWidth)
                    .aspectRatio(PhotoAspect)
                    .graphicsLayer {
                        rotationZ = PhotoTiltDegrees
                        translationY = (1f - serveProgress) * serveDistancePx
                        alpha = serveProgress.coerceIn(0f, 1f)
                    }
                    .background(NyummyTheme.colors.bg.surface, photoShape)
                    .border(PhotoFrame, NyummyTheme.colors.bg.surface, photoShape)
                    .padding(PhotoFrame)
                    .clip(RoundedCornerShape(NyummyTheme.radius.s)),
            )
        }
    }
}

/** 만세하며 통통 뛰는 냐미. 뛰어오를 때는 위로 뜨고, 바닥에 닿는 순간 살짝 눌린다. */
@Composable
private fun CheeringNyummy(width: Dp, animate: Boolean, modifier: Modifier = Modifier) {
    val jump = rememberInfiniteTransition(label = "CheerJump")
    val hop by jump.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(jumpSpec()),
        label = "cheerHop",
    )
    CeremonyArtView(
        art = MealCeremonyArt.cheering,
        width = width,
        contentDescription = stringResource(R.string.meal_record_cat_content_description),
        modifier = modifier.graphicsLayer {
            if (animate) {
                translationY = -JumpHeight.toPx() * hop
                val squash = if (hop < LandingWindow) 1f - (LandingWindow - hop) * LandingSquash else 1f
                scaleY = squash
                scaleX = 2f - squash
            }
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
    )
}

/**
 * 냐미가 맛있게 먹었어요. 띠욧과 같은 에버그린 빛 배경을 더 차분하게 깔고, 만세하며 뛰는 냐미 위에
 * 말풍선, 아래에 흰 글자에 그린 테두리를 두른 제목을 둡니다. 우편함 연출이 꺼져 있으면 여기서 [완료] 로 끝냅니다.
 * 영양 분석은 접수 뒤에 진행되므로 말풍선([showLine])은 꼼꼼히 맛보고 알려 주겠다고 말합니다.
 */
@Composable
private fun AteCelebration(
    title: Int,
    showLine: Boolean,
    showDone: Boolean,
    animate: Boolean,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Box(modifier = modifier) {
        GlowBurstBackground(
            modifier = Modifier.fillMaxSize(),
            focus = Offset(0.5f, AteFocusY),
            intensity = AteGlowIntensity,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = appear.value.coerceIn(0f, 1f)
                    val scale = AteStartScale + (1f - AteStartScale) * appear.value
                    scaleX = scale
                    scaleY = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(AteTopWeight))
            Box(Modifier.height(BubbleSlotHeight), contentAlignment = Alignment.BottomCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showLine,
                    enter = fadeIn() + scaleIn(initialScale = PopInScale),
                    exit = fadeOut(),
                ) {
                    NyummyVoiceBubble(
                        text = stringResource(R.string.meal_ceremony_analysis_pending),
                        tail = NyummyBubbleTail.Bottom,
                    )
                }
            }
            CheeringNyummy(width = AteCheerWidth, animate = animate)
            Spacer(Modifier.height(NyummyTheme.spacing.s24))
            OutlinedTitle(
                text = stringResource(title),
                modifier = Modifier.padding(horizontal = NyummyTheme.spacing.s24),
            )
            Spacer(Modifier.weight(AteBottomWeight))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(ButtonAreaHeight),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(visible = showDone, enter = fadeIn(tween(ButtonFadeMillis))) {
                    NyummyButton(
                        text = stringResource(R.string.meal_record_done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NyummyTheme.spacing.s20),
                        style = NyummyButtonStyle.Neutral,
                        size = NyummyButtonSize.L,
                        onClick = onDoneClick,
                    )
                }
            }
        }
    }
}

/** 흰 글자에 그린 테두리를 두른 큰 제목. 띠욧, 먹었어요처럼 에버그린 배경 위에 쓴다. */
@Composable
private fun OutlinedTitle(text: String, modifier: Modifier = Modifier) {
    val style = NyummyTheme.typography.displayL
    val strokePx = with(LocalDensity.current) { TitleStroke.toPx() }
    Box(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        NyummyText(
            text = text,
            style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
            color = NyummyTheme.colors.content.brand,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        NyummyText(
            text = text,
            style = style,
            color = NyummyTheme.colors.bg.surface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** 화면 전체를 좌우로 짧게 흔든다. 값은 [ShakeDistance] 배율이다. */
private fun shakeSpec() = keyframes {
    durationMillis = ShakeMillis
    1f at ShakeMillis / 6
    -1f at ShakeMillis * 2 / 6
    0.7f at ShakeMillis * 3 / 6
    -0.5f at ShakeMillis * 4 / 6
    0.2f at ShakeMillis * 5 / 6
    0f at ShakeMillis
}

/** 통통 뛰기 한 번: 0 이 바닥, 1 이 가장 높은 곳. 바닥에서 잠깐 쉰다. */
private fun jumpSpec() = keyframes {
    durationMillis = JumpMillis
    0f at 0
    0f at JumpRestMillis
    1f at JumpRestMillis + (JumpMillis - JumpRestMillis) / 2 using FastOutSlowInEasing
    0f at JumpMillis
}

private const val ServeDamping = 0.62f
private val ServeDistance = 260.dp

private const val FlashAlpha = 0.5f
private const val FlashMillis = 180
private const val ShakeMillis = 320
private val ShakeDistance = 7.dp

private const val AteMillis = 1_400L
private const val ReducedRevealMillis = 900L
private const val MailGulpMillis = 380L

/** 우편함에 쏙 들어간 뒤 홈으로 가기 전에 머무는 시간. */
private const val SentHoldMillis = 700L
private const val MailGulpScale = 1.25f
private const val MailGulpDamping = 0.4f

private val MailTargetSize = 48.dp
private val MailDotSize = 9.dp
private val MailDotInset = 8.dp

private const val CardHeightFraction = 0.86f
private const val CatWidthFraction = 0.66f
/** 냐미 발이 식탁 앞면 높이의 몇 배 위에 오는지. 1이면 식탁 윗선이다. */
private const val CatBottomOnTable = 0.98f

/** 식탁 윗선을 냐미 발보다 얼마나 더 올릴지(식탁 앞면 높이 대비). 냐미 아래쪽이 식탁에 가려 식탁 뒤에 앉아 보인다. */
private const val TableLiftOverCat = 0.3f
private const val PhotoWidthFraction = 0.78f
private const val PhotoAspect = 4f / 3f
private const val PhotoTiltDegrees = -4f
private val PhotoFrame = 6.dp
private const val BobMillis = 700
private val BobLift = 4.dp
private const val TwinkleMillis = 650
private val SparkleSize = 22.dp
private val SparkleSpread = 96.dp
private val SparkleRise = 90.dp
private const val PopInScale = 0.85f


/** 기다리는 냐미 그림에서 머리 꼭대기가 있는 높이(그림 높이 대비 비율). 말풍선을 그 위에 놓는다. */
private const val WaitingHeadTop = 30f / 405f
private val BubbleGap = 4.dp

private val BubbleSlotHeight = 64.dp
private val JumpHeight = 22.dp
private const val JumpMillis = 900
private const val JumpRestMillis = 240
private const val LandingWindow = 0.08f
private const val LandingSquash = 1.2f
private val ButtonAreaHeight = 148.dp
private const val ButtonFadeMillis = 220

private const val AteFocusY = 0.42f
private const val AteGlowIntensity = 0.85f
private const val AteStartScale = 0.92f
private const val AteTopWeight = 1f
private const val AteBottomWeight = 0.6f
private val AteCheerWidth = 220.dp
private val TitleStroke = 5.dp
