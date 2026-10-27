package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 우편함에서 발생하는 사용자 의도. */
sealed interface MailboxIntent : MviIntent {

    /** 화면이 보이게 됐다(첫 진입, 다른 화면에서 복귀). 우편을 다시 읽는다. */
    data object ScreenResumed : MailboxIntent

    data object ClickBack : MailboxIntent

    /** 목록 끝에 가까워졌다. 다음 묶음이 있으면 읽는다. */
    data object ReachMailListEnd : MailboxIntent

    /** 우편 하나의 받기(보상 상자는 열기). */
    data class ClaimMail(val mailId: Long) : MailboxIntent

    /** 보상 모두 받기. */
    data object ClaimAllMails : MailboxIntent

    /** 받은 우편 행을 왼쪽으로 밀어 지웠다. 목록에서 바로 빼고 서버에서 지운다. */
    data class DeleteMail(val mailId: Long) : MailboxIntent

    /** 우편을 지운 뒤 스낵바의 되돌리기. [row]를 원래 자리([index])에 되살린다. */
    data class UndoDeleteMail(val row: MailRowUiModel, val index: Int) : MailboxIntent

    /** 상자가 열리는 중에 화면을 누르거나 뒤로 갔다. 받은 보상을 바로 보여 준다. */
    data object SkipBoxOpening : MailboxIntent

    /** 상자가 열리는 연출이 끝났다. 받은 보상을 보여 준다. */
    data object BoxOpeningBurstFinished : MailboxIntent

    /** 받지 못한 상자가 무대에서 걷혔다. 안내를 띄운다. */
    data object BoxOpeningFailureShown : MailboxIntent

    /** 받은 보상을 본 뒤 계속하기(또는 뒤로 가기). 연출을 닫는다. */
    data object CloseBoxOpening : MailboxIntent

    /** 튜토리얼 소개 카드를 눌렀다. 소개를 마친다. */
    data object TutorialAdvance : MailboxIntent

    /** 튜토리얼이 가리킨 열기를 눌렀다. 상자 열기 연출이 끝나길 기다린다. */
    data object TutorialTargetClicked : MailboxIntent

    /** 튜토리얼 건너뛰기. 남은 튜토리얼을 모두 끝낸다. */
    data object TutorialSkip : MailboxIntent

    /** 튜토리얼 중 뒤로 가기. 우편함 안내만 마친다. */
    data object TutorialBack : MailboxIntent
}
