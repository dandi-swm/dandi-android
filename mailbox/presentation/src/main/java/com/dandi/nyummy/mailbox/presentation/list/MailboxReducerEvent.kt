package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface MailboxReducerEvent : ReducerEvent {
    data class InquiriesLoaded(val inquiries: ImmutableList<InquiryRowUiModel>) : MailboxReducerEvent
}
