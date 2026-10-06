package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 가로 구분선 1dp(border/subtle). */
@Composable
fun NyummyDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(NyummyTheme.borderWidth.hairline)
            .background(NyummyTheme.colors.border.subtle),
    )
}

/** 세로 구분선 1dp(border/subtle). 높이는 [height]로 정한다. */
@Composable
fun NyummyVerticalDivider(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(NyummyTheme.borderWidth.hairline)
            .height(height)
            .background(NyummyTheme.colors.border.subtle),
    )
}

/**
 * 섹션 헤더. 제목(title/s)과 선택 "전체보기 >"(Neutral S 글자 버튼).
 * 위 8, 아래 4. 섹션 사이 간격(위 24, 아래 8)은 화면에서 둔다.
 */
@Composable
fun NyummySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NyummyTheme.spacing.s8, bottom = NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(text = title, style = NyummyTheme.typography.titleS, modifier = Modifier.weight(1f), maxLines = 1)
        if (actionText != null) {
            SectionHeaderAction(actionText, onActionClick)
        }
    }
}

/** 설정 그룹 소제목. label/s tertiary, 위 20, 아래 4. */
@Composable
fun NyummySectionCaption(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NyummyTheme.spacing.s20, bottom = NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = title,
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (actionText != null) {
            SectionHeaderAction(actionText, onActionClick)
        }
    }
}

/** 제목 오른쪽에 아이콘 + 짧은 정보(예: 남은 시간)를 붙인 섹션 헤더. */
@Composable
fun NyummySectionHeaderWithMeta(
    title: String,
    meta: String,
    modifier: Modifier = Modifier,
    @DrawableRes metaIcon: Int = R.drawable.nyummy_ic_clock,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NyummyTheme.spacing.s8, bottom = NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(text = title, style = NyummyTheme.typography.titleS, modifier = Modifier.weight(1f), maxLines = 1)
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(metaIcon),
                contentDescription = null,
                tint = NyummyTheme.colors.content.tertiary,
                modifier = Modifier.size(NyummyTheme.size.iconXs),
            )
            NyummyText(
                text = meta,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionHeaderAction(text: String, onClick: () -> Unit) {
    NyummyTextButton(
        text = text,
        onClick = onClick,
        tone = NyummyTextButtonTone.Neutral,
        size = NyummyTextButtonSize.S,
        trailingIcon = R.drawable.nyummy_ic_chevron_right,
    )
}

enum class NyummySkeletonShape { Line, Block, Circle }

/**
 * 불러오는 동안 자리를 잡아 두는 회색 모양. 천천히 옅어졌다 진해진다.
 * 크기는 [modifier]로 정한다. Line은 높이 14가 기본이다.
 */
@Composable
fun NyummySkeleton(
    shape: NyummySkeletonShape,
    modifier: Modifier = Modifier,
) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f,
        targetValue = SkeletonMinAlpha,
        animationSpec = infiniteRepeatable(tween(SkeletonPulseMillis), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val drawShape: Shape = when (shape) {
        NyummySkeletonShape.Line -> RoundedCornerShape(NyummyTheme.radius.full)
        NyummySkeletonShape.Block -> RoundedCornerShape(NyummyTheme.radius.m)
        NyummySkeletonShape.Circle -> CircleShape
    }
    val sized = if (shape == NyummySkeletonShape.Line) Modifier.height(NyummyComponentDimens.SkeletonLineHeight) else Modifier
    Box(
        modifier
            .then(sized)
            .graphicsLayer { this.alpha = alpha }
            .background(NyummyTheme.colors.bg.surfaceSunken, drawShape),
    )
}

private const val SkeletonMinAlpha = 0.5f
private const val SkeletonPulseMillis = 900

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyStructurePreview() {
    NyummyTheme {
        Column(modifier = Modifier.padding(NyummyTheme.spacing.gutter)) {
            NyummySectionHeader(title = "오늘의 식사", actionText = "전체보기")
            NyummySectionCaption(title = "오늘의 식사", actionText = "전체보기")
            NyummySectionHeaderWithMeta(title = "오늘의 식사", meta = "6시간 남음")
            NyummyDivider()
            Row(
                modifier = Modifier.padding(top = NyummyTheme.spacing.s16),
                horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
            ) {
                NyummySkeleton(NyummySkeletonShape.Circle, Modifier.size(NyummyTheme.size.characterXs))
                Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
                    NyummySkeleton(NyummySkeletonShape.Line, Modifier.width(NyummyTheme.size.characterHero))
                    NyummySkeleton(NyummySkeletonShape.Line, Modifier.width(NyummyTheme.size.characterL))
                }
            }
        }
    }
}
