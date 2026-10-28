package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.reward.rememberReducedMotion

/** 튜토리얼을 진행하는 냐미의 표정. 튜토리얼 전용 상반신 일러스트다. */
enum class NyummyTutorialExpression(@DrawableRes internal val drawable: Int) {
    /** 인사. 대상 없이 소개하는 단계. */
    Greet(R.drawable.nyummy_tutorial_greet),

    /** 앞발로 가리킴. 누를 곳을 안내하는 단계. */
    Point(R.drawable.nyummy_tutorial_point),

    /** 깜짝 놀람. 보상이 도착했을 때, 건너뛰기를 말릴 때. */
    Surprise(R.drawable.nyummy_tutorial_surprise),

    /** 응원. 다음 단계로 이어 주거나 잘했다고 할 때. */
    Cheer(R.drawable.nyummy_tutorial_cheer),
}

/**
 * 첫 사용자 튜토리얼 안내 카드(피그마 `Tutorial Card`). 연초록 카드 위로 튜토리얼 냐미가 고개를 내밀고,
 * 이름표가 카드 왼쪽 위 모서리에 걸친다. 대사는 제목 한 줄과 설명 한 줄이다. 건너뛰기는 카드가 아니라 화면 오른쪽 위에 둔다.
 * 처음 나올 때 살짝 튀어오르며, 줄인 모션 설정이면 바로 보인다.
 *
 * 카드는 누를 곳에 붙여 꼬리로 가리킨다. 대상이 카드 아래면 [tail]을 [NyummyBubbleTail.Bottom], 위면
 * [NyummyBubbleTail.Top]으로 두고 [tailCenterX]에 대상 가운데의 가로 위치(카드 왼쪽 기준)를 넘긴다. 대상이 없으면 [NyummyBubbleTail.None].
 * 냐미는 꼬리 방향과 상관없이 늘 카드 윗변 위에 있다. 꼬리가 위로 나고 대상이 냐미 자리(카드 왼쪽)에 있으면
 * 냐미가 대상을 가리지 않게 카드 가운데로 옮긴다.
 *
 * @param showHint "탭해서 계속"을 보인다. 누를 곳이 있어 카드를 눌러도 넘어가지 않는 단계는 false.
 */
@Composable
fun NyummyTutorialCard(
    title: String,
    expression: NyummyTutorialExpression,
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
    BoxWithConstraints(
        modifier = modifier.graphicsLayer {
            alpha = appear.value
            val scale = lerp(AppearStartScale, 1f, appear.value)
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, if (tail == NyummyBubbleTail.Top) 0f else 1f)
        },
    ) {
        val guideOverTarget = tail == NyummyBubbleTail.Top && tailCenterX != null && tailCenterX < GuideStart + GuideSize
        Image(
            painter = painterResource(expression.drawable),
            contentDescription = null,
            modifier = Modifier
                .padding(start = if (guideOverTarget) (maxWidth - GuideSize) / 2 else GuideStart)
                .size(GuideSize),
        )
        TutorialCardBody(
            title = title,
            body = body,
            tail = tail,
            tailCenterX = tailCenterX,
            showHint = showHint,
            modifier = Modifier.padding(top = TutorialCardBodyTop),
        )
    }
}

/**
 * 화면 오른쪽 위에 고정하는 튜토리얼 건너뛰기(피그마 `Tutorial / Skip`). 흰 알약에 글자만 있고,
 * 보이는 높이는 32지만 터치 영역은 48을 지킨다. 누르면 건너뛰기 확인을 띄운다.
 */
@Composable
fun NyummyTutorialSkipButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .heightIn(min = NyummyTheme.size.touchTarget)
            .nyummyClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(
            text = stringResource(R.string.nyummy_tutorial_skip),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.secondary,
            modifier = Modifier
                .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.full))
                .padding(horizontal = NyummyTheme.spacing.s10, vertical = NyummyTheme.spacing.s6),
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
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val radius = NyummyTheme.radius.ml
        val tailOffset = ((tailCenterX ?: (maxWidth / 2)) - TailSize / 2)
            .coerceIn(radius, maxWidth - radius - TailSize)
        val shape = remember(tail, radius, tailOffset) {
            BubbleShape(
                tail = tail,
                cornerRadius = radius,
                tailSize = TailSize,
                tailCorner = TailCorner,
                tailHeight = TailHeight,
                tailOffset = tailOffset,
            )
        }
        val tailTop = if (tail == NyummyBubbleTail.Top) TailHeight else 0.dp
        // 위 꼬리가 이름표 자리에 오면 이름표를 꼬리 오른쪽으로 비켜 꼬리가 가려지지 않게 한다.
        val nameTagStart = if (tail == NyummyBubbleTail.Top && tailOffset < NameTagClearance) {
            tailOffset + TailSize + NyummyTheme.spacing.s6
        } else {
            NyummyTheme.spacing.s16
        }
        val tailBottom = if (tail == NyummyBubbleTail.Bottom) TailHeight else 0.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nyummyShadow(shape, NyummyTheme.elevation.float)
                .background(NyummyTheme.colors.bg.brandSubtle, shape)
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
                    color = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = NyummyTheme.spacing.s6),
                )
            }
        }
        NyummyText(
            text = stringResource(R.string.nyummy_tutorial_name),
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = nameTagStart, y = tailTop - NameTagOverlap)
                .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.xs))
                .padding(horizontal = NyummyTheme.spacing.s6, vertical = NameTagVerticalPadding),
        )
    }
}

/** 카드 위로 고개를 내미는 냐미 크기. 모든 단계에서 같다. */
private val GuideSize = 96.dp

/** 냐미가 카드 윗변 아래로 겹치는 깊이. 카드 뒤에 그려 고개만 내민다. */
private val GuideOverlap = 16.dp

/** 안내 카드 맨 위에서 카드 몸통 윗변까지의 거리(냐미가 차지하는 높이). 코치마크가 카드를 대상 아래에 붙일 때 쓴다. */
internal val TutorialCardBodyTop = GuideSize - GuideOverlap

/** 냐미의 카드 왼쪽 기준 시작 위치. 이름표 바로 위에 온다. */
private val GuideStart = 2.dp

/** 꼬리: 16 정사각형(모서리 2)을 45도 돌려 몸통 가장자리에 반쯤 겹친다. */
private val TailSize = 16.dp
private val TailCorner = 2.dp
private val TailHeight = 11.dp

/** 이름표가 카드 윗변 밖으로 걸치는 깊이. */
private val NameTagOverlap = 10.dp
private val NameTagVerticalPadding = 3.dp

/** 이름표가 차지하는 카드 왼쪽 폭. 위 꼬리가 이 안에 오면 이름표를 비킨다. */
private val NameTagClearance = 56.dp

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
                showHint = true,
            )
            NyummyTutorialCard(
                title = "여기를 눌러 첫 기록을 해 보자!",
                body = "밥을 먹을 때마다 사진 한 장이면 기록 끝이야",
                expression = NyummyTutorialExpression.Point,
                tail = NyummyBubbleTail.Bottom,
                tailCenterX = 175.dp,
            )
            NyummyTutorialCard(
                title = "열기를 눌러 볼까?",
                body = "상자 안에 뭐가 들었을까",
                expression = NyummyTutorialExpression.Point,
                tail = NyummyBubbleTail.Top,
                tailCenterX = 296.dp,
            )
        }
    }
}
