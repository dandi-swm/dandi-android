package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.mailbox.presentation.R

/** 튜토리얼이 가리키는 상자 행의 열기 버튼. 그 행에만 등록한다. */
internal val MailboxOpenCoachMarkKey = CoachMarkKey("mailbox.open")

/**
 * 우편함 첫 사용자 튜토리얼.
 *
 * @property boxMailId 열어 보게 할 받지 않은 상자 우편. null이면 받을 상자가 없어 우편함을 소개한다.
 * @property isWaitingForBox 안내대로 열기를 눌러 상자 열기 연출이 도는 중. 코치마크를 내리고 연출이 끝나길 기다린다.
 */
@Immutable
data class MailboxTutorialUiState(
    val boxMailId: Long?,
    val isWaitingForBox: Boolean = false,
)

/** 우편함 튜토리얼을 코치마크로 올린다. 상자 열기 연출 중에는 내린다(연출은 별도 창이라 코치마크보다 위에 뜬다). */
@Composable
internal fun MailboxTutorialCoachMark(
    tutorial: MailboxTutorialUiState?,
    isBoxOpening: Boolean,
    onIntent: (MailboxIntent) -> Unit,
) {
    val mark = tutorial?.takeUnless { it.isWaitingForBox || isBoxOpening }?.let {
        if (it.boxMailId != null) {
            CoachMark(
                title = stringResource(R.string.mailbox_tutorial_open_title),
                body = stringResource(R.string.mailbox_tutorial_open_body),
                expression = NyummyTutorialExpression.Point,
                target = MailboxOpenCoachMarkKey,
                onTargetClick = { onIntent(MailboxIntent.TutorialTargetClicked) },
                onSkip = { onIntent(MailboxIntent.TutorialSkip) },
                onBack = { onIntent(MailboxIntent.TutorialBack) },
            )
        } else {
            CoachMark(
                title = stringResource(R.string.mailbox_tutorial_intro_title),
                body = stringResource(R.string.mailbox_tutorial_intro_body),
                expression = NyummyTutorialExpression.Greet,
                onAdvance = { onIntent(MailboxIntent.TutorialAdvance) },
                onSkip = { onIntent(MailboxIntent.TutorialSkip) },
                onBack = { onIntent(MailboxIntent.TutorialBack) },
            )
        }
    }
    ShowCoachMark(mark)
}
