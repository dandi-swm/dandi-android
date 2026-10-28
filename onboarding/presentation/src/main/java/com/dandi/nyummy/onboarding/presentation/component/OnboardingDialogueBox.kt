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
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadge
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTypewriterText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.onboarding.presentation.OnboardingSpeaker
import com.dandi.nyummy.onboarding.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 미연시 대화창. 흰 상자(모서리 20, 옅은 그림자) 안 첫 줄에 화자 이름표를 두고 그 아래 대사를 쓴다.
 *
 * - 냐미: 연초록 이름표, 냐미 말투(voice/m, content/voice)
 * - 사용자: 회색 이름표, 본문(body/l)
 * - 나레이션: 이름표 없이 본문(body/l)
 *
 * [slot]이 있으면(이름 짓기 등) 대화창 아래 흰 카드(모서리 20)에 입력 영역을 두고 "탭해서 계속" 안내는 숨긴다.
 * 안내가 없을 때도 같은 자리를 비워 두어 대화창 높이가 튀지 않게 한다.
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
    val shape = RoundedCornerShape(NyummyTheme.radius.ml)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .nyummyShadow(shape, NyummyTheme.elevation.float)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(
                start = NyummyTheme.spacing.s20,
                end = NyummyTheme.spacing.s20,
                top = BoxTopPadding,
                bottom = BoxBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        if (hasNameTag) {
            NyummyBadge(
                text = speakerName,
                tone = if (speaker == OnboardingSpeaker.USER) NyummyBadgeTone.Neutral else NyummyBadgeTone.Success,
            )
        }
        NyummyTypewriterText(
            text = text,
            lineKey = lineKey,
            revealed = revealed,
            onRevealed = onRevealed,
            color = if (speaker == OnboardingSpeaker.CAT) {
                NyummyTheme.colors.content.voice
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ContinueRowHeight),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ContinueHint(visible = showContinueHint && slot == null)
        }
    }
    if (slot != null) {
        // 이름 짓기, 식사 시각 같은 입력은 대화창 아래 흰 카드에 둔다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nyummyShadow(shape, NyummyTheme.elevation.float)
                .background(NyummyTheme.colors.bg.surface, shape)
                .padding(NyummyTheme.spacing.s20),
            content = slot,
        )
    }
    }
}

/** "탭해서 계속 ›" 안내. 화살표가 오른쪽으로 살짝씩 밀리며 눌러서 넘길 수 있음을 알린다. */
@Composable
private fun ContinueHint(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(ChipFadeInMillis)) + scaleIn(tween(ChipFadeInMillis), initialScale = ChipInitialScale),
        exit = fadeOut(tween(ChipFadeOutMillis)),
    ) {
        val nudge by rememberInfiniteTransition(label = "ContinueHint").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ChipNudgeMillis), RepeatMode.Reverse),
            label = "ContinueHintNudge",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(R.string.onboarding_tap_to_continue),
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
            )
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_chevron_right),
                contentDescription = null,
                tint = NyummyTheme.colors.content.tertiary,
                modifier = Modifier
                    .size(NyummyTheme.size.iconXs)
                    .offset(x = ChipNudge * nudge),
            )
        }
    }
}

/** 대사 두 줄(body/l 24 × 2) 높이. 한 줄 대사여도 대화창 크기를 맞춘다. */
private val DialogueTextMinHeight = 48.dp
private val BoxTopPadding = 18.dp
private val BoxBottomPadding = 14.dp
private val ContinueRowHeight = 14.dp
private val ChipNudge = 3.dp
private const val ChipInitialScale = 0.8f
private const val ChipFadeInMillis = 200
private const val ChipFadeOutMillis = 120
private const val ChipNudgeMillis = 520
