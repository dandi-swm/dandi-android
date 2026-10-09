package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface MailboxReducerEvent : ReducerEvent {
    data class InquiriesLoaded(val inquiries: ImmutableList<InquiryRowUiModel>) : MailboxReducerEvent
    data class InquiryRemoved(val inquiryId: Long) : MailboxReducerEvent

    /** 되돌리기나 삭제 실패로 [row]를 원래 자리([index])에 다시 넣는다. */
    data class InquiryRestored(val row: InquiryRowUiModel, val index: Int) : MailboxReducerEvent
}
