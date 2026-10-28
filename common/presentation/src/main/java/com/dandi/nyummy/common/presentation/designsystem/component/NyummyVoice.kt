package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

/** 말풍선 꼬리 방향. 꼬리는 말하는 냐미 쪽을 향한다. */
enum class NyummyBubbleTail { Bottom, Left, Top, None }

/**
 * 냐미 말풍선(v2 Speech Bubble). Voice 서체(voice/m, content/voice) 전용, 최대 3줄. 흰 바탕, 모서리 16, 옅은 그림자.
 * 꼬리는 모서리 둥근 정사각형을 45도 돌려 몸통 가장자리에 반쯤 겹친 것이고, 몸통과 하나의 외곽선으로 합쳐
 * 그림자와 바탕에 이음새가 보이지 않는다.
 * 너비는 글 길이에 맞춰 늘어나고, 최대 너비는 [modifier]로 제한한다.
 *
 * @param typing true면 대사가 바뀔 때마다 미연시처럼 한 글자씩 찍는다. 말풍선 크기는 처음부터 문장 전체에 맞춘다.
 * @param lineKey 바뀌면 처음부터 다시 찍는다([typing]일 때만)
 * @param onRevealed 한 글자씩 찍기가 끝났다([typing]일 때만)
 */
@Composable
fun NyummyVoiceBubble(
    text: String,
    modifier: Modifier = Modifier,
    tail: NyummyBubbleTail = NyummyBubbleTail.Bottom,
    typing: Boolean = false,
    lineKey: Any = text,
    onRevealed: () -> Unit = {},
) {
    val dimens = NyummyComponentDimens
    val cornerRadius = NyummyTheme.radius.m
    val shape = remember(tail, cornerRadius) {
        BubbleShape(
            tail = tail,
            cornerRadius = cornerRadius,
            tailSize = dimens.BubbleTailSize,
            tailCorner = dimens.BubbleTailCorner,
            tailHeight = dimens.BubbleTailHeight,
            tailOffset = if (tail == NyummyBubbleTail.Left) dimens.BubbleTailOffsetVertical else dimens.BubbleTailOffset,
        )
    }
    val tailPadding = Modifier.padding(
        start = if (tail == NyummyBubbleTail.Left) dimens.BubbleTailHeight else 0.dp,
        top = if (tail == NyummyBubbleTail.Top) dimens.BubbleTailHeight else 0.dp,
        bottom = if (tail == NyummyBubbleTail.Bottom) dimens.BubbleTailHeight else 0.dp,
    )
    Box(
        modifier = modifier
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.voiceBubble, shape)
            .then(tailPadding)
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s12),
    ) {
        if (typing) {
            NyummyTypewriterText(
                text = text,
                style = NyummyTheme.typography.voiceM,
                color = NyummyTheme.colors.content.voice,
                lineKey = lineKey,
                onRevealed = onRevealed,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            NyummyText(
                text = text,
                style = NyummyTheme.typography.voiceM,
                color = NyummyTheme.colors.content.voice,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 둥근 몸통 + 45도 돌린 둥근 정사각형 꼬리를 하나의 Path로 합친 말풍선 모양. 꼬리 중심은 몸통 가장자리 위에 있다. */
@Immutable
private class BubbleShape(
    private val tail: NyummyBubbleTail,
    private val cornerRadius: Dp,
    private val tailSize: Dp,
    private val tailCorner: Dp,
    private val tailHeight: Dp,
    private val tailOffset: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { cornerRadius.toPx() }
        val side = with(density) { tailSize.toPx() }
        val corner = with(density) { tailCorner.toPx() }
        val height = with(density) { tailHeight.toPx() }
        val offset = with(density) { tailOffset.toPx() }

        val left = if (tail == NyummyBubbleTail.Left) height else 0f
        val top = if (tail == NyummyBubbleTail.Top) height else 0f
        val bottom = size.height - if (tail == NyummyBubbleTail.Bottom) height else 0f
        val body = Path().apply {
            addRoundRect(RoundRect(left, top, size.width, bottom, CornerRadius(radius)))
        }
        // 꼬리 중심. 옛 삼각 꼬리의 밑변 가운데와 같은 자리라 호출하는 쪽의 꼬리 위치 맞춤은 그대로다.
        val center = when (tail) {
            NyummyBubbleTail.Bottom -> Offset(left + offset + side / 2, bottom)
            NyummyBubbleTail.Top -> Offset(left + offset + side / 2, top)
            NyummyBubbleTail.Left -> Offset(left, top + offset + side / 2)
            NyummyBubbleTail.None -> return Outline.Generic(body)
        }
        val tailPath = Path().apply {
            addRoundRect(RoundRect(-side / 2, -side / 2, side / 2, side / 2, CornerRadius(corner)))
            transform(
                Matrix().apply {
                    translate(center.x, center.y)
                    rotateZ(TAIL_ROTATION_DEGREES)
                },
            )
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, tailPath))
    }
}

private const val TAIL_ROTATION_DEGREES = 45f

/**
 * 냐미 코멘트 카드. 매달린 냐미(Hang)가 연민트 말풍선 윗변에 앞발을 걸친다.
 * 접힌 상태는 최대 3줄이고, 글이 3줄을 넘을 때만 "더보기"가 보인다. 말풍선을 누르면 펼치고 접는다.
 */
@Composable
fun NyummyCoachCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    val dimens = NyummyComponentDimens
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    val expandable = overflows || expanded

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(top = dimens.CoachHangHeight - dimens.CoachCardTopPadding)
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.selected, RoundedCornerShape(dimens.CoachCardRadius))
                .then(
                    if (expandable) {
                        Modifier.clickable(
                            interactionSource = rememberNyummyInteractionSource(),
                            indication = null,
                            role = Role.Button,
                            onClickLabel = if (expanded) "접기" else "더보기",
                            onClick = { expanded = !expanded },
                        )
                    } else {
                        Modifier
                    },
                )
                .animateContentSize()
                .padding(
                    start = dimens.CoachCardHorizontalPadding,
                    end = dimens.CoachCardHorizontalPadding,
                    top = dimens.CoachCardTopPadding,
                    bottom = dimens.CoachCardBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        ) {
            NyummyText(
                text = text,
                style = NyummyTheme.typography.voiceM,
                maxLines = if (expanded) Int.MAX_VALUE else CollapsedMaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result -> if (!expanded) overflows = result.hasVisualOverflow },
            )
            if (expandable) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CoachToggleLabel(expanded = expanded)
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.nyummy_pose_hang),
            contentDescription = null,
            modifier = Modifier
                .padding(start = dimens.CoachCardHorizontalPadding)
                .size(dimens.CoachHangWidth, dimens.CoachHangHeight),
        )
    }
}

/** 더보기/접기 표시. 말풍선 전체가 터치 영역이라 이 글자는 따로 누르지 않는다. */
@Composable
private fun CoachToggleLabel(expanded: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = NyummyTheme.spacing.s4, vertical = NyummyTheme.spacing.s8),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = if (expanded) "접기" else "더보기",
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
        )
        Icon(
            painter = painterResource(if (expanded) R.drawable.nyummy_ic_chevron_up else R.drawable.nyummy_ic_chevron_down),
            contentDescription = null,
            tint = NyummyTheme.colors.content.brand,
            modifier = Modifier.size(NyummyTheme.size.iconXs),
        )
    }
}

private const val CollapsedMaxLines = 3

/**
 * 냐미 목소리 토스트. 흰 알약 + 2px 테두리 + float 그림자, 왼쪽에 냐미 아바타(48).
 * 3~5초 뒤 사라지게 하는 것은 호출하는 쪽이 맡는다. 시스템 알림은 Snackbar를 쓴다.
 */
@Composable
fun NyummyVoiceToast(
    text: String,
    modifier: Modifier = Modifier,
    pose: NyummyPose = NyummyPose.Hello,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Row(
        modifier = modifier
            .nyummyShadow(shape, NyummyTheme.elevation.float)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
            .padding(
                start = NyummyComponentDimens.VoiceToastPadding + NyummyTheme.borderWidth.bold,
                top = NyummyComponentDimens.VoiceToastPadding + NyummyTheme.borderWidth.bold,
                bottom = NyummyComponentDimens.VoiceToastPadding + NyummyTheme.borderWidth.bold,
                end = NyummyTheme.spacing.s20,
            ),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyPoseImage(pose = pose, size = NyummyTheme.size.characterXs)
        NyummyText(text = text, style = NyummyTheme.typography.voiceS, maxLines = 2)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyVoicePreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyBubbleTail.entries.forEach { NyummyVoiceBubble(text = "집사~ 오늘 첫 끼는 뭐야?", tail = it) }
            NyummyCoachCard(text = "채소 가득한 비빔밥이네! 오늘 첫 끼 최고였어. 다음 끼니도 같이 먹자. 내일은 단백질도 조금 더 챙겨 보자")
            NyummyVoiceToast(text = "기록 완료! 냐미가 맛있게 먹었어")
        }
    }
}
