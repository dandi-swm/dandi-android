package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 우편함(아이템 탭, 문의 탭)에서 발생하는 사용자 의도. */
sealed interface MailboxIntent : MviIntent {

    /** 화면이 보이게 됐다(첫 진입, 문의를 보내고 복귀). 목록을 다시 읽는다(아이템 탭이 켜져 있으면 우편도). */
    data object ScreenResumed : MailboxIntent

    data object ClickBack : MailboxIntent

    /** 목록 끝에 가까워졌다. 다음 묶음이 있으면 읽는다. */
    data object ReachListEnd : MailboxIntent

    /** 상단 바나 빈 화면의 문의하기. */
    data object ClickWrite : MailboxIntent

    data class ClickInquiry(val inquiryId: Long) : MailboxIntent

    /** 행을 왼쪽으로 밀어 지웠다. 목록에서 바로 빼고 서버에서 지운다(소프트 삭제). */
    data class DeleteInquiry(val inquiryId: Long) : MailboxIntent

    /** 지운 뒤 스낵바의 되돌리기. [row]를 원래 자리([index])에 되살린다. */
    data class UndoDelete(val row: InquiryRowUiModel, val index: Int) : MailboxIntent

    data class SelectTab(val tab: MailboxTab) : MailboxIntent

    /** 아이템 탭 목록 끝에 가까워졌다. 다음 묶음이 있으면 읽는다. */
    data object ReachMailListEnd : MailboxIntent

    /** 우편 하나의 받기(보상 상자는 열기). */
    data class ClaimMail(val mailId: Long) : MailboxIntent

    /** 보상 모두 받기. */
    data object ClaimAllMails : MailboxIntent

    /** 받은 우편 행을 왼쪽으로 밀어 지웠다. 목록에서 바로 빼고 서버에서 지운다. */
    data class DeleteMail(val mailId: Long) : MailboxIntent

    /** 우편을 지운 뒤 스낵바의 되돌리기. [row]를 원래 자리([index])에 되살린다. */
    data class UndoDeleteMail(val row: MailRowUiModel, val index: Int) : MailboxIntent
}
