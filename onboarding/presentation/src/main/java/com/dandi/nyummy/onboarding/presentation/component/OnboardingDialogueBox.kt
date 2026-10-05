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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.onboarding.presentation.OnboardingSpeaker
import com.dandi.nyummy.onboarding.presentation.R

private val DialogueBorderWidth = 1.5.dp
private val DialogueMinTextHeight = 88.dp
private val DialogueNameTagOverlap = 18.dp
private val ContinueHintHeight = 32.dp
private val ContinueIconSize = 18.dp
private val ContinueNudge = 4.dp

/**
 * 미연시 대사창. 화자가 고양이·사용자면 창 왼쪽 위에 이름표를 걸치고, 나레이션은 이름표 없이 흐린 색으로 보여준다.
 * 사용자 대사는 이름표와 테두리를 다른 톤으로 칠해 고양이와 "주고받는" 느낌을 낸다.
 * 대사가 다 나오면 오른쪽 아래에 엔터 아이콘이 달린 "탭해서 계속" 칩이 통통 튀며 다음 대사가 있음을 알린다.
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
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    val isUser = speaker == OnboardingSpeaker.USER
    val hasNameTag = speaker != OnboardingSpeaker.NARRATOR

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (hasNameTag) DialogueNameTagOverlap else 0.dp),
            shape = DesignSystemThemeImpl.designSystemShape.sheetDefault,
            color = if (isUser) colors.bgSurfaceIvory else colors.bgCoachBubble,
            border = BorderStroke(
                DialogueBorderWidth,
                if (isUser) colors.borderDefaultLevel1 else colors.borderCoachBubble,
            ),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = spacing.space24,
                    end = spacing.space16,
                    top = if (hasNameTag) spacing.space32 else spacing.space24,
                    bottom = spacing.space12,
                ),
            ) {
                TypewriterText(
                    text = text,
                    lineKey = lineKey,
                    revealed = revealed,
                    onRevealed = onRevealed,
                    color = if (speaker == OnboardingSpeaker.NARRATOR) {
                        colors.contentDefaultLevel1
                    } else {
                        colors.contentDefaultLevel0
                    },
                    style = DesignSystemThemeImpl.typeScale.displayRegularL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = spacing.space8)
                        .heightIn(min = DialogueMinTextHeight),
                )
                // 칩이 없을 때도 같은 높이를 차지해 대사창 크기가 튀지 않게 한다.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ContinueHintHeight),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    ContinueHint(visible = showContinueHint)
                }
            }
        }
        if (hasNameTag) {
            Surface(
                modifier = Modifier.padding(start = spacing.space20),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = if (isUser) colors.bgSurfaceInverse else colors.bgBrandDefault,
            ) {
                DandiText(
                    text = speakerName,
                    modifier = Modifier.padding(horizontal = spacing.space20, vertical = spacing.space8),
                    color = colors.contentInverseDefault,
                    style = DesignSystemThemeImpl.typeScale.displayRegularM,
                )
            }
        }
    }
}

/** "탭해서 계속 ↵" 칩. 엔터 아이콘이 왼쪽으로 살짝씩 밀리며 "눌러서 넘기기"를 계속 알린다. */
@Composable
private fun ContinueHint(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
        exit = fadeOut(tween(120)),
    ) {
        val colors = DesignSystemThemeImpl.designSystemColor
        val transition = rememberInfiniteTransition(label = "continueHint")
        val nudge by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 520), RepeatMode.Reverse),
            label = "continueHintNudge",
        )
        Surface(
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            color = colors.bgBrandDefault,
        ) {
            Row(
                modifier = Modifier.padding(
                    start = DesignSystemThemeImpl.designSystemSpacing.space12,
                    end = DesignSystemThemeImpl.designSystemSpacing.space8,
                    top = DesignSystemThemeImpl.designSystemSpacing.space4,
                    bottom = DesignSystemThemeImpl.designSystemSpacing.space4,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space4),
            ) {
                DandiText(
                    text = stringResource(R.string.onboarding_tap_to_continue),
                    color = colors.contentInverseDefault,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
                Icon(
                    painter = painterResource(R.drawable.onboarding_ic_enter),
                    contentDescription = null,
                    tint = colors.contentInverseDefault,
                    modifier = Modifier
                        .size(ContinueIconSize)
                        .offset(x = -ContinueNudge * nudge),
                )
            }
        }
    }
}
