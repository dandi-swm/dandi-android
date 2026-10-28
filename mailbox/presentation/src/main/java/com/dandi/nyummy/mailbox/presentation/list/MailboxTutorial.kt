package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialSkipDialog
import com.dandi.nyummy.mailbox.presentation.R
import kotlinx.collections.immutable.ImmutableList

/** 튜토리얼이 가리키는 상자 행의 열기 버튼. 그 행에만 등록한다. */
internal val MailboxOpenCoachMarkKey = CoachMarkKey("mailbox.open")

/** 상단 바 뒤로가기. 상자를 연 뒤 홈으로 돌아가게 가리킨다. */
internal val MailboxBackCoachMarkKey = CoachMarkKey("mailbox.back")

/** 우편함 튜토리얼 단계. */
enum class MailboxTutorialStage {
    /** 받지 않은 상자의 열기를 가리킨다. */
    OpenBox,

    /** 받을 상자가 없어 우편함을 소개한다. */
    Intro,

    /** 우편함 단계를 마치고 뒤로가기를 가리켜 홈(마이룸 탭 단계)으로 이끈다. */
    GoBack,
}

/**
 * 우편함 첫 사용자 튜토리얼.
 *
 * @property boxMailId 열어 보게 할 받지 않은 상자 우편. [MailboxTutorialStage.OpenBox]에서만 있다.
 * @property isWaitingForBox 안내대로 열기를 눌러 상자 열기 연출이 도는 중. 코치마크를 내리고 연출이 끝나길 기다린다.
 * @property skipConfirm 건너뛰기 확인에 보일 남은 안내. 확인이 닫혀 있으면 null
 * @property isRoomNext 다음이 마이룸 단계인지. 마이룸 기능이 꺼져 있으면 뒤로가기 안내가 홈으로 돌아가자고 한다.
 */
@Immutable
data class MailboxTutorialUiState(
    val stage: MailboxTutorialStage,
    val boxMailId: Long? = null,
    val isWaitingForBox: Boolean = false,
    val skipConfirm: ImmutableList<NyummyTutorialLesson>? = null,
    val isRoomNext: Boolean = true,
)

/**
 * 우편함 튜토리얼을 코치마크로 올린다. 상자 열기 연출 중에는 내린다(연출은 별도 창이라 코치마크보다 위에 뜬다).
 * 건너뛰기는 화면 오른쪽 위에 두고, 확인을 거친다. 뒤로 가기는 가로채지 않는다.
 */
@Composable
internal fun MailboxTutorialCoachMark(
    tutorial: MailboxTutorialUiState?,
    isBoxOpening: Boolean,
    onIntent: (MailboxIntent) -> Unit,
) {
    val onSkip = { onIntent(MailboxIntent.TutorialSkip) }
    val mark = tutorial?.takeUnless { it.isWaitingForBox || isBoxOpening }?.let {
        when (it.stage) {
            MailboxTutorialStage.OpenBox -> CoachMark(
                title = stringResource(R.string.mailbox_tutorial_open_title),
                body = stringResource(R.string.mailbox_tutorial_open_body),
                expression = NyummyTutorialExpression.Point,
                target = MailboxOpenCoachMarkKey,
                onTargetClick = { onIntent(MailboxIntent.TutorialTargetClicked) },
                onSkip = onSkip,
            )
            MailboxTutorialStage.Intro -> CoachMark(
                title = stringResource(R.string.mailbox_tutorial_intro_title),
                body = stringResource(R.string.mailbox_tutorial_intro_body),
                expression = NyummyTutorialExpression.Greet,
                onAdvance = { onIntent(MailboxIntent.TutorialAdvance) },
                onSkip = onSkip,
            )
            MailboxTutorialStage.GoBack -> CoachMark(
                title = stringResource(R.string.mailbox_tutorial_back_title),
                body = stringResource(if (it.isRoomNext) R.string.mailbox_tutorial_back_body else R.string.mailbox_tutorial_back_home_body),
                expression = NyummyTutorialExpression.Cheer,
                target = MailboxBackCoachMarkKey,
                onSkip = onSkip,
            )
        }
    }
    ShowCoachMark(mark)
    tutorial?.skipConfirm?.let { remaining ->
        NyummyTutorialSkipDialog(
            remaining = remaining,
            onContinue = { onIntent(MailboxIntent.TutorialSkipCancelled) },
            onSkip = { onIntent(MailboxIntent.TutorialSkipConfirmed) },
        )
    }
}
