package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.presentation.OnboardingSpeaker
import com.dandi.nyummy.onboarding.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 미연시 대화창. 화자에 따라 모양이 다르다.
 *
 * - 냐미: 연한 그린 바탕, Voice 서체, 그린 이름표
 * - 사용자: 흰 바탕, 본문 서체, 진한 이름표
 * - 나레이션: 흰 바탕, 보조색 본문 서체, 이름표 없음
 *
 * [slot]이 있으면(이름 짓기 등) 대사 아래에 입력 영역을 두고 "탭해서 계속" 칩은 숨긴다.
 * 칩이 없을 때도 같은 자리를 비워 두어 대화창 높이가 튀지 않게 한다.
 */
@Composable
internal fun OnboardingDialogueBox(
    speaker: OnboardingSpeaker,
    speakerName: String,
    text: String,
    lineKey: Any,
    revealed: Boolean,
    showContinueHint: Boolean,
    onRevealed: () -> Unit,
    modifier: Modifier = Modifier,
    slot: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val hasNameTag = speaker != OnboardingSpeaker.NARRATOR
    val shape = RoundedCornerShape(NyummyTheme.radius.l)
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (hasNameTag) NameTagOverlap else 0.dp)
                .background(
                    color = if (speaker == OnboardingSpeaker.CAT) {
                        NyummyTheme.colors.bg.voiceCoach
                    } else {
                        NyummyTheme.colors.bg.surface
                    },
                    shape = shape,
                )
                .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
                .padding(
                    start = NyummyTheme.spacing.s20,
                    end = NyummyTheme.spacing.s16,
                    top = if (hasNameTag) BoxTopPaddingWithTag else NyummyTheme.spacing.s20,
                    bottom = NyummyTheme.spacing.s12,
                ),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        ) {
            TypewriterText(
                text = text,
                lineKey = lineKey,
                revealed = revealed,
                onRevealed = onRevealed,
                color = if (speaker == OnboardingSpeaker.NARRATOR) {
                    NyummyTheme.colors.content.secondary
                } else {
                    NyummyTheme.colors.content.primary
                },
                style = if (speaker == OnboardingSpeaker.CAT) {
                    NyummyTheme.typography.voiceM
                } else {
                    NyummyTheme.typography.bodyL
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DialogueTextMinHeight),
            )
            if (slot != null) {
                Column(
                    modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
                    content = slot,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ContinueRowHeight),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    ContinueChip(visible = showContinueHint)
                }
            }
        }
        if (hasNameTag) {
            NameTag(
                name = speakerName,
                isUser = speaker == OnboardingSpeaker.USER,
                modifier = Modifier.padding(start = NyummyTheme.spacing.s20),
            )
        }
    }
}

@Composable
private fun NameTag(name: String, isUser: Boolean, modifier: Modifier = Modifier) {
    NyummyText(
        text = name,
        style = NyummyTheme.typography.labelM,
        color = if (isUser) NyummyTheme.colors.content.onInverse else NyummyTheme.colors.content.onAction,
        maxLines = 1,
        modifier = modifier
            .background(
                color = if (isUser) NyummyTheme.colors.bg.surfaceInverse else NyummyTheme.colors.bg.actionPrimary,
                shape = RoundedCornerShape(NyummyTheme.radius.full),
            )
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = NameTagVerticalPadding),
    )
}

/** "탭해서 계속 ↵" 칩. 화살표가 왼쪽으로 살짝씩 밀리며 눌러서 넘길 수 있음을 알린다. */
@Composable
private fun ContinueChip(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(ChipFadeInMillis)) + scaleIn(tween(ChipFadeInMillis), initialScale = ChipInitialScale),
        exit = fadeOut(tween(ChipFadeOutMillis)),
    ) {
        val nudge by rememberInfiniteTransition(label = "ContinueChip").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ChipNudgeMillis), RepeatMode.Reverse),
            label = "ContinueChipNudge",
        )
        Row(
            modifier = Modifier
                .background(NyummyTheme.colors.bg.selected, RoundedCornerShape(NyummyTheme.radius.full))
                .padding(
                    start = ChipStartPadding,
                    end = NyummyTheme.spacing.s8,
                    top = NyummyTheme.spacing.s4,
                    bottom = NyummyTheme.spacing.s4,
                ),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(R.string.onboarding_tap_to_continue),
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.brand,
            )
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_corner_down_left),
                contentDescription = null,
                tint = NyummyTheme.colors.content.brand,
                modifier = Modifier
                    .size(NyummyTheme.size.iconS)
                    .offset(x = -ChipNudge * nudge),
            )
        }
    }
}

/** 대사 두 줄(body/l 28 × 2) 높이. 한 줄 대사여도 대화창 크기를 맞춘다. */
private val DialogueTextMinHeight = 56.dp
private val NameTagOverlap = 16.dp
private val NameTagVerticalPadding = 6.dp
private val BoxTopPaddingWithTag = 28.dp
private val ContinueRowHeight = 28.dp
private val ChipStartPadding = 10.dp
private val ChipNudge = 3.dp
private const val ChipInitialScale = 0.8f
private const val ChipFadeInMillis = 200
private const val ChipFadeOutMillis = 120
private const val ChipNudgeMillis = 520
