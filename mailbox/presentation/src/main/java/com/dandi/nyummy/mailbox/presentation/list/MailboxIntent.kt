package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 우편함(문의 목록)에서 발생하는 사용자 의도. */
sealed interface MailboxIntent : MviIntent {

    /** 화면이 보이게 됐다(첫 진입, 문의를 보내고 복귀). 목록을 다시 읽는다. */
    data object ScreenResumed : MailboxIntent

    data object ClickBack : MailboxIntent

    /** 상단 바나 빈 화면의 문의하기. */
    data object ClickWrite : MailboxIntent

    data class ClickInquiry(val inquiryId: Long) : MailboxIntent
}
