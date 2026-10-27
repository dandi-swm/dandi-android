package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.reward.rememberReducedMotion

/** 튜토리얼을 진행하는 냐미의 표정. 튜토리얼 전용 상반신 그림이다. */
enum class NyummyTutorialExpression(@DrawableRes internal val drawable: Int) {
    /** 인사. 대상 없이 소개하는 단계. */
    Greet(R.drawable.nyummy_tutorial_greet),

    /** 앞발로 가리킴. 누를 곳을 안내하는 단계. */
    Point(R.drawable.nyummy_tutorial_point),

    /** 깜짝 기뻐함. 보상이 도착했을 때. */
    Surprise(R.drawable.nyummy_tutorial_surprise),

    /** 응원. 다음 단계로 이어 주거나 잘했다고 할 때. */
    Cheer(R.drawable.nyummy_tutorial_cheer),
}

/**
 * 첫 사용자 튜토리얼 안내 카드. 카드 왼쪽 위에서 튜토리얼 냐미가 고개를 내밀고, 이름표와 건너뛰기가 카드 모서리에 걸친다.
 * 대사는 두 줄 이내다. 처음 나올 때 살짝 튀어오르며, 줄인 모션 설정이면 바로 보인다.
 *
 * 카드는 누를 곳에 붙여 꼬리로 가리킨다. 대상이 아래쪽이면 [tail]을 [NyummyBubbleTail.Bottom], 위쪽이면
 * [NyummyBubbleTail.Top]으로 두고 [tailCenterX]에 대상 가운데의 가로 위치(카드 왼쪽 기준)를 넘긴다.
 * 꼬리가 위로 나면 대상과 겹치지 않게 건너뛰기를 카드 아래 모서리로 옮긴다. 대상이 없으면 [NyummyBubbleTail.None].
 *
 * @param onSkip 건너뛰기. null이면 건너뛸 수 없는 단계라 건너뛰기를 그리지 않는다.
 * @param showHint "탭해서 계속"을 보인다. 누를 곳이 있어 카드를 눌러도 넘어가지 않는 단계는 false.
 */
@Composable
fun NyummyTutorialCard(
    title: String,
    expression: NyummyTutorialExpression,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
    body: String? = null,
    tail: NyummyBubbleTail = NyummyBubbleTail.None,
    tailCenterX: Dp? = null,
    showHint: Boolean = false,
) {
    val reducedMotion = rememberReducedMotion()
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) appear.animateTo(1f, spring(dampingRatio = AppearDamping, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = appear.value
            val scale = lerp(AppearStartScale, 1f, appear.value)
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, if (tail == NyummyBubbleTail.Top) 0f else 1f)
        },
    ) {
        Image(
            painter = painterResource(expression.drawable),
            contentDescription = null,
            modifier = Modifier
                .padding(start = NyummyTheme.spacing.s4)
                .size(GuideSize),
        )
        TutorialCardBody(
            title = title,
            body = body,
            tail = tail,
            tailCenterX = tailCenterX,
            showHint = showHint,
            onSkip = onSkip,
            modifier = Modifier.padding(top = GuideSize - GuideOverlap),
        )
    }
}

@Composable
private fun TutorialCardBody(
    title: String,
    body: String?,
    tail: NyummyBubbleTail,
    tailCenterX: Dp?,
    showHint: Boolean,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val dimens = NyummyComponentDimens
        val radius = NyummyTheme.radius.l
        val tailOffset = ((tailCenterX ?: (maxWidth / 2)) - dimens.BubbleTailBase / 2)
            .coerceIn(radius, maxWidth - radius - dimens.BubbleTailBase)
        val shape = remember(tail, radius, tailOffset) {
            BubbleShape(tail, radius, dimens.BubbleTailBase, dimens.BubbleTailHeight, tailOffset)
        }
        val tailTop = if (tail == NyummyBubbleTail.Top) dimens.BubbleTailHeight else 0.dp
        val tailBottom = if (tail == NyummyBubbleTail.Bottom) dimens.BubbleTailHeight else 0.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nyummyShadow(shape, NyummyTheme.elevation.soft)
                .background(NyummyTheme.colors.bg.voiceCoach, shape)
                .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
                .padding(
                    start = NyummyTheme.spacing.s20,
                    end = NyummyTheme.spacing.s20,
                    top = tailTop + NyummyTheme.spacing.s24,
                    bottom = tailBottom + NyummyTheme.spacing.s16,
                ),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleS)
            if (!body.isNullOrBlank()) {
                NyummyText(text = body, style = NyummyTheme.typography.bodyM, color = NyummyTheme.colors.content.secondary)
            }
            if (showHint) {
                NyummyText(
                    text = stringResource(R.string.nyummy_tutorial_hint),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        TutorialPill(
            text = stringResource(R.string.nyummy_tutorial_name),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = NyummyTheme.spacing.s16, y = tailTop - PillOverlap)
                .background(NyummyTheme.colors.bg.actionPrimary, RoundedCornerShape(NyummyTheme.radius.full)),
            strong = true,
        )
        if (onSkip == null) return@BoxWithConstraints
        val skipAtBottom = tail == NyummyBubbleTail.Top
        TutorialPill(
            text = stringResource(R.string.nyummy_tutorial_skip),
            modifier = Modifier
                .align(if (skipAtBottom) Alignment.BottomEnd else Alignment.TopEnd)
                .offset(x = -NyummyTheme.spacing.s16, y = if (skipAtBottom) PillOverlap else tailTop - PillOverlap)
                .nyummyClickable(onClick = onSkip)
                .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, RoundedCornerShape(NyummyTheme.radius.full)),
            strong = false,
        )
    }
}

/** 카드 모서리에 걸치는 작은 알약(이름표, 건너뛰기). */
@Composable
private fun TutorialPill(text: String, strong: Boolean, modifier: Modifier = Modifier) {
    NyummyText(
        text = text,
        style = if (strong) NyummyTheme.typography.labelSStrong else NyummyTheme.typography.labelS,
        color = if (strong) NyummyTheme.colors.content.onAction else NyummyTheme.colors.content.secondary,
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.s12, vertical = PillVerticalPadding),
    )
}

/** 카드 위로 고개를 내미는 냐미 크기. 모든 단계에서 같다. */
private val GuideSize = 104.dp

/** 냐미가 카드 윗변 아래로 겹치는 깊이. */
private val GuideOverlap = 18.dp

/** 이름표와 건너뛰기가 카드 윗변(아래 모서리면 아랫변) 밖으로 걸치는 깊이. */
private val PillOverlap = 13.dp
private val PillVerticalPadding = 5.dp

private const val AppearStartScale = 0.92f
private const val AppearDamping = 0.6f

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyTutorialCardPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s20),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s32),
        ) {
            NyummyTutorialCard(
                title = "안녕, 집사! 나는 냐미야",
                body = "집사가 밥을 기록하면 나도 같이 먹을 수 있어",
                expression = NyummyTutorialExpression.Greet,
                onSkip = {},
                showHint = true,
            )
            NyummyTutorialCard(
                title = "밥을 먹으면 여기를 눌러 줘!",
                body = "사진 한 장이면 기록 끝이야",
                expression = NyummyTutorialExpression.Point,
                onSkip = {},
                tail = NyummyBubbleTail.Bottom,
                tailCenterX = 175.dp,
            )
            NyummyTutorialCard(
                title = "보상 상자가 도착했어!",
                body = "우편함에서 같이 열어 보자",
                expression = NyummyTutorialExpression.Surprise,
                onSkip = {},
                tail = NyummyBubbleTail.Top,
                tailCenterX = 264.dp,
            )
        }
    }
}
